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

@SpringBootTest(properties="app.demo-mode=true")
@AutoConfigureMockMvc
@Transactional
class DemoFeaturesTests {
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

    @Autowired com.ankkun.ecommerce.module.order.repository.PaymentRepository payments;
    @Autowired com.ankkun.ecommerce.module.product.repository.ProductRepository products;
    @Autowired com.ankkun.ecommerce.module.mail.repository.DemoMessageRepository outbox;
    private JsonNode order(String customer,String admin,String provider) throws Exception {
        String v=fixture(admin,3), a=address(customer);add(customer,v,1,200);
        return data(mvc.perform(post("/api/orders").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON)
          .content(json.writeValueAsString(java.util.Map.of("address_id",a,"payment_method",provider)))).andExpect(status().isOk()).andReturn());
    }
    private String nonce(String customer,String id,String provider,boolean save) throws Exception {
        var res=provider.equals("momo")?mvc.perform(post("/api/payments/momo/create/"+id).param("saveCard",String.valueOf(save)).header("Authorization","Bearer "+customer)).andExpect(status().isOk()).andReturn():mvc.perform(post("/api/payments/create-url").param("saveCard",String.valueOf(save)).header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("order_id",id)))).andExpect(status().isOk()).andReturn();
        var d=data(res);return (d.isTextual()?d.asText():d.path("url").asText()).split("nonce=")[1];
    }
    private MvcResult complete(String customer,String id,String nonce,String outcome,int expected) throws Exception {
        return mvc.perform(post("/api/payments/demo/"+id+"/complete").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON)
          .content(json.writeValueAsString(java.util.Map.of("nonce",nonce,"outcome",outcome)))).andExpect(status().is(expected)).andReturn();
    }
    @Test void vnpaySuccessReplayOwnershipAndSavedMethod() throws Exception {
        String admin=token(UserRole.admin),customer=token(UserRole.customer),stranger=token(UserRole.customer);
        var o=order(customer,admin,"vnpay");String id=o.path("id").asText(),n=nonce(customer,id,"vnpay",true);
        complete(stranger,id,n,"success",404);complete(customer,id,"forged","success",403);
        assertThat(data(complete(customer,id,n,"success",200)).path("isSuccess").asBoolean()).isTrue();
        complete(customer,id,n,"failure",200);
        mvc.perform(get("/api/orders/"+id).header("Authorization","Bearer "+customer)).andExpect(jsonPath("$.data.status").value("confirmed")).andExpect(jsonPath("$.data.payment_status").value("paid")).andExpect(jsonPath("$.data.shipping_log.mode").value("demo"));
        var methods=data(mvc.perform(get("/api/payments/methods").header("Authorization","Bearer "+customer)).andReturn());assertThat(methods.size()).isEqualTo(1);String method=methods.get(0).path("id").asText();
        mvc.perform(delete("/api/payments/methods/"+method).header("Authorization","Bearer "+stranger)).andExpect(status().isNotFound());
        mvc.perform(delete("/api/payments/methods/"+method).header("Authorization","Bearer "+customer)).andExpect(status().isOk());
    }
    @Test void momoFailureRetryInvalidatesOldNonceAndCancellation() throws Exception {
        String admin=token(UserRole.admin),customer=token(UserRole.customer);var o=order(customer,admin,"momo");String id=o.path("id").asText(),n=nonce(customer,id,"momo",false);
        complete(customer,id,n,"failure",200);String newer=nonce(customer,id,"momo",false);complete(customer,id,n,"success",403);
        mvc.perform(post("/api/orders/"+id+"/cancel").header("Authorization","Bearer "+customer)).andExpect(status().isOk());complete(customer,id,newer,"success",400);
        assertThat(variants.findById(o.path("items").get(0).path("variant_id").asText()).orElseThrow().getStockQuantity()).isEqualTo(3);
    }
    @Test void expiredNonceAndProviderMismatchAreRejected() throws Exception {
        String admin=token(UserRole.admin),customer=token(UserRole.customer);String id=order(customer,admin,"vnpay").path("id").asText(),n=nonce(customer,id,"vnpay",false);
        mvc.perform(post("/api/payments/momo/create/"+id).header("Authorization","Bearer "+customer)).andExpect(status().isBadRequest());
        var p=payments.findByOrderId(id).orElseThrow();p.setNonceExpiresAt(java.time.Instant.now().minusSeconds(1));payments.saveAndFlush(p);complete(customer,id,n,"success",400);
        mvc.perform(get("/api/payments/vnpay-return")).andExpect(status().isBadRequest());
    }
    @Test void deliveredReviewModerationOwnershipAndRatingAggregate() throws Exception {
        String admin=token(UserRole.admin),customer=token(UserRole.customer),stranger=token(UserRole.customer);var o=order(customer,admin,"cod");String id=o.path("id").asText(),product=o.path("items").get(0).path("variant").path("product").path("id").asText();String body=json.writeValueAsString(java.util.Map.of("product_id",product,"rating",5,"comment","Great"));
        mvc.perform(post("/api/reviews").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        for(String state:java.util.List.of("confirmed","processing","shipping","delivered"))mvc.perform(patch("/api/admin/orders/"+id+"/status").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(java.util.Map.of("status",state)))).andExpect(status().isOk());
        String review=data(mvc.perform(post("/api/reviews").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isOk()).andReturn()).path("id").asText();
        mvc.perform(post("/api/reviews").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isConflict());
        mvc.perform(delete("/api/reviews/"+review).header("Authorization","Bearer "+stranger)).andExpect(status().isNotFound());
        mvc.perform(patch("/api/admin/reviews/"+review).header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content("{\"admin_reply\":\"Thanks\"}")).andExpect(jsonPath("$.data.admin_reply").value("Thanks"));
        assertThat(products.findById(product).orElseThrow().getRatingAvg()).isEqualTo(5);
        mvc.perform(delete("/api/reviews/"+review).header("Authorization","Bearer "+customer)).andExpect(status().isOk());assertThat(products.findById(product).orElseThrow().getRatingCount()).isZero();
    }
    @Test void refundRestoresStockOnlyOnceAndUpdatesStatistics() throws Exception {
        String admin=token(UserRole.admin),customer=token(UserRole.customer);var o=order(customer,admin,"vnpay");String id=o.path("id").asText(),n=nonce(customer,id,"vnpay",false);complete(customer,id,n,"success",200);
        for(int i=0;i<2;i++)mvc.perform(patch("/api/admin/orders/"+id+"/payment-status").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"refunded\"}")).andExpect(status().isOk());
        assertThat(variants.findById(o.path("items").get(0).path("variant_id").asText()).orElseThrow().getStockQuantity()).isEqualTo(3);
        assertThat(outbox.count()).isGreaterThan(0);
        mvc.perform(get("/api/admin/stats/overview").header("Authorization","Bearer "+admin)).andExpect(status().isOk());
    }
    @Test void avatarFilesValidateContentAndProfilePersists() throws Exception {
        String customer=token(UserRole.customer);var bytes=new java.io.ByteArrayOutputStream();javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2,2,java.awt.image.BufferedImage.TYPE_INT_RGB),"png",bytes);
        String url=data(mvc.perform(multipart("/api/users/me/avatar").file(new org.springframework.mock.web.MockMultipartFile("image","avatar.png","image/png",bytes.toByteArray())).header("Authorization","Bearer "+customer)).andExpect(status().isOk()).andReturn()).asText();
        mvc.perform(get(url)).andExpect(status().isOk()).andExpect(header().string("X-Content-Type-Options","nosniff"));
        mvc.perform(get("/api/users/me").header("Authorization","Bearer "+customer)).andExpect(jsonPath("$.data.avatar").value(url));
        mvc.perform(multipart("/api/users/me/avatar").file(new org.springframework.mock.web.MockMultipartFile("image","evil.svg","image/svg+xml","<svg/>".getBytes())).header("Authorization","Bearer "+customer)).andExpect(status().isBadRequest());
    }
    @Test void passwordChangeRevokesExistingAccessTokens() throws Exception {
        String customer=token(UserRole.customer);
        mvc.perform(post("/api/users/me/change-password").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON).content("{\"password\":\"TestPass123!\",\"new_password\":\"ChangedPass123!\"}")).andExpect(status().isOk());
        mvc.perform(get("/api/users/me").header("Authorization","Bearer "+customer)).andExpect(status().isUnauthorized());
    }
    @Test void chatOnlyReturnsOwnOrdersAndCouponAdminHasRoleGuard() throws Exception {
        String admin=token(UserRole.admin),customer=token(UserRole.customer),stranger=token(UserRole.customer);var o=order(customer,admin,"cod");
        var result=mvc.perform(post("/api/chat/completions").header("Authorization","Bearer "+stranger).contentType(MediaType.APPLICATION_JSON).content("{\"message\":\"đơn hàng\"}")).andExpect(status().isOk()).andReturn();assertThat(result.getResponse().getContentAsString()).doesNotContain(o.path("order_code").asText());
        mvc.perform(post("/api/coupons").header("Authorization","Bearer "+customer).contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isForbidden());
    }
}
