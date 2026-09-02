package com.ankkun.ecommerce.module.order.repository;

import com.ankkun.ecommerce.module.order.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, String> {
    Optional<Payment> findByOrderId(String orderId);
    Optional<Payment> findByProviderOrderId(String providerOrderId);
}
