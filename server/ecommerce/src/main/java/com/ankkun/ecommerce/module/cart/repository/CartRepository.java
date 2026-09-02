package com.ankkun.ecommerce.module.cart.repository;

import com.ankkun.ecommerce.module.cart.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, String> {
    Optional<Cart> findByUserId(String userId);
}
