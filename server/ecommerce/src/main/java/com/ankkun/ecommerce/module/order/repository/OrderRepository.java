package com.ankkun.ecommerce.module.order.repository;

import com.ankkun.ecommerce.common.enums.OrderStatus;
import com.ankkun.ecommerce.common.enums.PaymentStatus;
import com.ankkun.ecommerce.module.order.entity.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {
    Page<Order> findByUserId(String userId, Pageable pageable);
    Optional<Order> findByOrderCode(String orderCode);
    Optional<Order> findByIdAndUserId(String id, String userId);

    long countByCreatedAtBetween(Instant from, Instant to);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.paymentStatus = :status")
    BigDecimal sumTotalByPaymentStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.paymentStatus = :status AND o.createdAt BETWEEN :from AND :to")
    BigDecimal sumTotalByPaymentStatusAndCreatedAtBetween(PaymentStatus status, Instant from, Instant to);

    long countByCreatedAtBetweenAndPaymentStatus(Instant from, Instant to, PaymentStatus status);
}
