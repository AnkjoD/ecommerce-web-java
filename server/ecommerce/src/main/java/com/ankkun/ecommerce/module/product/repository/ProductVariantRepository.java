package com.ankkun.ecommerce.module.product.repository;

import com.ankkun.ecommerce.module.product.entity.ProductVariant;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, String> {
    Optional<ProductVariant> findBySkuAndIsActiveTrue(String sku);
    List<ProductVariant> findByProductIdOrderByPriceAsc(String productId);
    List<ProductVariant> findByStockQuantityLessThanEqual(int threshold);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select x from ProductVariant x where x.id = :id")
    java.util.Optional<ProductVariant> findLocked(@org.springframework.data.repository.query.Param("id") String id);
}
