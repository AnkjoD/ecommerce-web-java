package com.ankkun.ecommerce.module.product.repository;

import com.ankkun.ecommerce.module.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    Optional<ProductVariant> findBySkuAndIsActiveTrue(String sku);
    List<ProductVariant> findByProductIdOrderByPriceAsc(String productId);
    List<ProductVariant> findByStockQuantityLessThanEqual(int threshold);
}
