package com.ankkun.ecommerce.module.cart.repository;

import com.ankkun.ecommerce.module.cart.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, String> {
    Optional<CartItem> findByCartIdAndVariantId(String cartId, String variantId);
    void deleteByCartId(String cartId);
}
