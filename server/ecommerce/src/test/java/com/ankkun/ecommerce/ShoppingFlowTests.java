package com.ankkun.ecommerce;

import com.ankkun.ecommerce.common.enums.UserRole;
import com.ankkun.ecommerce.module.user.entity.User;
import com.ankkun.ecommerce.module.user.repository.UserRepository;
import com.ankkun.ecommerce.module.product.repository.ProductVariantRepository;
import com.ankkun.ecommerce.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ShoppingFlowTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired ProductVariantRepository variants;
    @Autowired JwtTokenProvider jwt;
    @Autowired PasswordEncoder passwords;
    @Autowired com.ankkun.ecommerce.module.coupon.repository.CouponRepository coupons;

    private String token(UserRole role) {
        var user = users.saveAndFlush(User.builder().email(UUID.randomUUID()+"@example.test")
                .fullName("Test User").passwordHash(passwords.encode("TestPass123!")).role(role).build());
        return jwt.generateAccessToken(user.getId(), user.getEmail(), role.name());
    }
    private JsonNode data(MvcResult result) throws Exception {
        return json.readTree(result.getResponse().getContentAsString()).get("data");
    }
    private String fixture(String admin, int stock) throws Exception {
        var product = data(mvc.perform(post("/api/admin/products").header("Authorization","Bearer "+admin)
                .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"Test Shoe\",\"slug\":\""+UUID.randomUUID()+"\",\"status\":\"active\",\"attributes\":{\"material\":\"cotton\"},\"media\":{\"images\":[]}}"))
                .andExpect(status().isOk()).andReturn());
        assertThat(product.path("attributes").path("material").asText()).isEqualTo("cotton");
        return data(mvc.perform(post("/api/admin/products/"+product.path("id").asText()+"/variants")
                .header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON)
                .content("{\"sku\":\""+UUID.randomUUID()+"\",\"price\":100000,\"stock_quantity\":"+stock+",\"options\":{\"size\":\"M\"}}"))
                .andExpect(status().isOk()).andReturn()).path("id").asText();
    }
    private String address(String customer) throws Exception {
        return data(mvc.perform(post("/api/addresses").header("Authorization","Bearer "+customer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"recipient_name\":\"Test\",\"phone\":\"0900000000\",\"province\":\"Hà Nội\",\"district\":\"Cầu Giấy\",\"ward\":\"Dịch Vọng\",\"street\":\"1 Test Street\"}"))
                .andExpect(status().isCreated()).andReturn()).path("id").asText();
    }
    private void add(String customer, String variant, int qty, int expected) throws Exception {
        mvc.perform(post("/api/carts/items").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"variant_id\":\""+variant+"\",\"quantity\":"+qty+"}"))
                .andExpect(status().is(expected));
    }
    @Test void cookieLoginRefreshAndReplayProtection() throws Exception {
        String email = UUID.randomUUID()+"@example.test";
        var registered = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+email+"\",\"password\":\"TestPass123!\",\"full_name\":\"Cookie User\"}"))
                .andExpect(status().isCreated()).andExpect(cookie().httpOnly("accessToken",true)).andReturn();
        var logged = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+email+"\",\"password\":\"TestPass123!\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.full_name").value("Cookie User")).andReturn();
        assertThat(logged.getResponse().getCookie("refreshToken").getValue())
                .isNotEqualTo(registered.getResponse().getCookie("refreshToken").getValue());
        Cookie access = logged.getResponse().getCookie("accessToken");
        mvc.perform(get("/api/auth/me").cookie(access)).andExpect(status().isOk());
        Cookie refresh = logged.getResponse().getCookie("refreshToken");
        mvc.perform(post("/api/auth/refresh").cookie(refresh)).andExpect(status().isOk());
        mvc.perform(post("/api/auth/refresh").cookie(refresh)).andExpect(status().isForbidden());
        mvc.perform(post("/api/auth/logout").cookie(access)).andExpect(status().isOk()).andExpect(cookie().maxAge("accessToken",0));
    }
    @Test void addressesAndAdminUsersDoNotExposeEntities() throws Exception {
        String customer = token(UserRole.customer), admin = token(UserRole.admin);
        address(customer);
        var result = mvc.perform(get("/api/addresses").header("Authorization","Bearer "+customer)).andExpect(status().isOk()).andReturn();
        assertThat(result.getResponse().getContentAsString()).doesNotContain("password", "tokenHash", "refresh_tokens", "\"user\"");
        assertThat(data(result).get(0).path("recipient_name").asText()).isEqualTo("Test");
        mvc.perform(post("/api/auth/seed-admin").header("Authorization","Bearer "+customer)).andExpect(status().isNotFound());
        mvc.perform(post("/api/admin/products").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        var userList = mvc.perform(get("/api/admin/users").header("Authorization","Bearer "+admin)).andExpect(status().isOk()).andReturn();
        assertThat(userList.getResponse().getContentAsString()).doesNotContain("password", "tokenHash", "refreshTokens");
    }
    @Test void checkoutHistoryOwnershipAndCancellationRestoreStock() throws Exception {
        String admin = token(UserRole.admin), customer = token(UserRole.customer), stranger = token(UserRole.customer);
        String variant = fixture(admin,2), address = address(customer);
        add(customer,variant,2,200);
        var order = data(mvc.perform(post("/api/orders").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"address_id\":\""+address+"\",\"payment_method\":\"cod\"}"))
                .andExpect(status().isOk()).andReturn());
        String id = order.path("id").asText();
        assertThat(order.path("items").size()).isEqualTo(1);
        assertThat(order.path("total").decimalValue()).isEqualByComparingTo("230000");
        assertThat(variants.findById(variant).orElseThrow().getStockQuantity()).isZero();
        mvc.perform(get("/api/carts").header("Authorization","Bearer "+customer)).andExpect(jsonPath("$.data.total_items").value(0));
        mvc.perform(get("/api/orders").header("Authorization","Bearer "+customer)).andExpect(jsonPath("$.data.total").value(1));
        mvc.perform(get("/api/orders/"+id).header("Authorization","Bearer "+stranger)).andExpect(status().isNotFound());
        mvc.perform(post("/api/orders/"+id+"/cancel").header("Authorization","Bearer "+customer)).andExpect(status().isOk());
        assertThat(variants.findById(variant).orElseThrow().getStockQuantity()).isEqualTo(2);
        mvc.perform(post("/api/orders/"+id+"/cancel").header("Authorization","Bearer "+customer)).andExpect(status().isBadRequest());
        assertThat(variants.findById(variant).orElseThrow().getStockQuantity()).isEqualTo(2);
    }
    @Test void stockCannotBeExceededByAddingTwiceOrCompetingCart() throws Exception {
        String admin=token(UserRole.admin), first=token(UserRole.customer), second=token(UserRole.customer);
        String variant=fixture(admin,1), firstAddress=address(first), secondAddress=address(second);
        add(first,variant,1,200); add(first,variant,1,400); add(second,variant,1,200);
        mvc.perform(post("/api/orders").header("Authorization","Bearer "+first).contentType(MediaType.APPLICATION_JSON)
                .content("{\"address_id\":\""+firstAddress+"\",\"payment_method\":\"cod\"}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/orders").header("Authorization","Bearer "+second).contentType(MediaType.APPLICATION_JSON)
                .content("{\"address_id\":\""+secondAddress+"\",\"payment_method\":\"cod\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/orders").header("Authorization","Bearer "+second)).andExpect(jsonPath("$.data.total").value(0));
    }
    @Test void browserMutationRejectsUntrustedOrigin() throws Exception {
        String customer=token(UserRole.customer);
        mvc.perform(post("/api/auth/logout").header("Authorization","Bearer "+customer).header("Origin","https://untrusted.example"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/products/missing")).andExpect(status().isNotFound());
    }

    @Test void adminAndCustomerCookiesCanCoexist() throws Exception {
        String customer = token(UserRole.customer), admin = token(UserRole.admin);
        var adminUser = users.findById(jwt.parseAccessToken(admin).getSubject()).orElseThrow();
        var result = mvc.perform(post("/api/auth/admin/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\""+adminUser.getEmail()+"\",\"password\":\"TestPass123!\"}"))
                .andExpect(status().isOk()).andExpect(cookie().httpOnly("adminAccessToken",true)).andReturn();
        Cookie customerCookie = new Cookie("accessToken",customer);
        Cookie adminCookie = result.getResponse().getCookie("adminAccessToken");
        mvc.perform(get("/api/auth/me").cookie(adminCookie,customerCookie)).andExpect(jsonPath("$.data.role").value("customer"));
        mvc.perform(get("/api/auth/me").header("X-Client-App","admin").cookie(customerCookie,adminCookie))
                .andExpect(jsonPath("$.data.role").value("admin"));
    }

    @Test void couponIsAppliedOnServerAndCannotExceedUsageLimit() throws Exception {
        String customer=token(UserRole.customer), admin=token(UserRole.admin);
        String variant=fixture(admin,3), address=address(customer), code="TEST-"+UUID.randomUUID();
        var coupon = coupons.saveAndFlush(com.ankkun.ecommerce.module.coupon.entity.Coupon.builder().code(code.toUpperCase())
                .discountType("percent").discountValue(java.math.BigDecimal.TEN).usageLimit(1).build());
        add(customer,variant,1,200);
        mvc.perform(post("/api/orders").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"address_id\":\""+address+"\",\"payment_method\":\"cod\",\"coupon_code\":\""+code+"\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.discount_amount").value(10000))
                .andExpect(jsonPath("$.data.total").value(120000));
        assertThat(coupons.findById(coupon.getId()).orElseThrow().getUsedCount()).isEqualTo(1);
        add(customer,variant,1,200);
        mvc.perform(post("/api/orders").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"address_id\":\""+address+"\",\"payment_method\":\"cod\",\"coupon_code\":\""+code+"\"}"))
                .andExpect(status().isBadRequest());
        assertThat(variants.findById(variant).orElseThrow().getStockQuantity()).isEqualTo(2);
    }

    @Test void checkoutOnlyBuysSelectedItems() throws Exception {
        String customer=token(UserRole.customer), admin=token(UserRole.admin);
        String selectedVariant=fixture(admin,1), remainingVariant=fixture(admin,1), address=address(customer);
        add(customer,selectedVariant,1,200); add(customer,remainingVariant,1,200);
        var cart=data(mvc.perform(get("/api/carts").header("Authorization","Bearer "+customer)).andReturn());
        String selectedItem="";
        for (var item:cart.path("items")) if(item.path("variant_id").asText().equals(selectedVariant)) selectedItem=item.path("id").asText();
        mvc.perform(post("/api/orders").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"address_id\":\""+address+"\",\"payment_method\":\"cod\",\"item_ids\":[\""+selectedItem+"\"]}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.subtotal").value(100000))
                .andExpect(jsonPath("$.data.items.length()").value(1));
        mvc.perform(get("/api/carts").header("Authorization","Bearer "+customer))
                .andExpect(jsonPath("$.data.items[0].variant_id").value(remainingVariant))
                .andExpect(jsonPath("$.data.total_items").value(1));
        assertThat(variants.findById(remainingVariant).orElseThrow().getStockQuantity()).isEqualTo(1);
    }
}
