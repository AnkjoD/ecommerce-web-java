package com.ankkun.ecommerce.config;
import com.ankkun.ecommerce.module.user.entity.User;
import com.ankkun.ecommerce.common.enums.UserRole;
import com.ankkun.ecommerce.module.user.repository.UserRepository;
import com.ankkun.ecommerce.module.product.entity.*;
import com.ankkun.ecommerce.module.product.repository.*;
import com.ankkun.ecommerce.module.category.entity.Category;
import com.ankkun.ecommerce.module.category.repository.CategoryRepository;
import com.ankkun.ecommerce.module.coupon.entity.Coupon;
import com.ankkun.ecommerce.module.coupon.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.context.annotation.Profile;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
@Component
@Profile("demo")
@RequiredArgsConstructor
public class DemoData implements CommandLineRunner {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CategoryRepository categories;
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final CouponRepository coupons;
    @Override @Transactional public void run(String... args) {
        account("admin@homura.test", "DemoAdmin123!", "Demo Admin", UserRole.admin);
        account("customer@homura.test", "DemoCustomer123!", "Demo Customer", UserRole.customer);
        var category = categories.findBySlug("demo-collection").orElseGet(() -> categories.save(Category.builder().name("Bộ sưu tập demo").slug("demo-collection").build()));
        String[] names = {"Homura Sneakers", "Everyday Backpack", "Classic Hoodie"};
        int[] prices = {450000,320000,280000};
        for (int i=0; i<names.length; i++) {
            String slug = "demo-product-"+i;
            if (products.existsBySlug(slug)) continue;
            String image = "/assets/demo-product-"+i+".svg";
            var p = products.save(Product.builder().name(names[i]).slug(slug).brand("Homura").categoryId(category.getId()).status("active")
                    .description("{\"short\":\"Sản phẩm mẫu dùng để thử luồng mua hàng.\",\"full\":\"Dữ liệu demo, không phải sản phẩm đang được bán.\"}")
                    .media("{\"thumbnail\":\""+image+"\",\"images\":[\""+image+"\"]}")
                    .minPrice(BigDecimal.valueOf(prices[i])).maxPrice(BigDecimal.valueOf(prices[i])).build());
            variants.save(ProductVariant.builder().productId(p.getId()).sku("DEMO-"+i).price(BigDecimal.valueOf(prices[i]))
                    .priceBeforeDiscount(BigDecimal.valueOf(prices[i]+50000)).stockQuantity(20).options("{\"Màu\":\"Đen\",\"Size\":\"M\"}").imageUrl(image).build());
        }
        if (!coupons.existsByCode("DEMO10")) coupons.save(Coupon.builder().code("DEMO10").discountType("percent").discountValue(BigDecimal.TEN).usageLimit(1000).build());
    }
    private void account(String email, String password, String name, UserRole role) {
        if (!users.existsByEmail(email)) users.save(User.builder().email(email).fullName(name).passwordHash(encoder.encode(password)).role(role).build());
    }
}
