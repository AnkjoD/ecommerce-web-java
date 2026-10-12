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

public interface OrderRepository extends JpaRepository<Order, String>, org.springframework.data.jpa.repository.JpaSpecificationExecutor<Order> {
    Page<Order> findByUserId(String userId, Pageable pageable);
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);
    Page<Order> findByUserIdAndStatusIn(String userId, java.util.Collection<OrderStatus> status, Pageable pageable);
    Page<Order> findByUserIdAndStatus(String userId, OrderStatus status, Pageable pageable);
    Optional<Order> findByOrderCode(String orderCode);
    Optional<Order> findByIdAndUserId(String id, String userId);

    boolean existsByAddressId(String addressId);
    long countByCreatedAtBetween(Instant from, Instant to);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.paymentStatus = :status")
    BigDecimal sumTotalByPaymentStatus(PaymentStatus status);

    @Query("SELECT COALESCE(SUM(o.total), 0) FROM Order o WHERE o.paymentStatus = :status AND o.createdAt BETWEEN :from AND :to")
    BigDecimal sumTotalByPaymentStatusAndCreatedAtBetween(PaymentStatus status, Instant from, Instant to);

    long countByCreatedAtBetweenAndPaymentStatus(Instant from, Instant to, PaymentStatus status);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select x from Order x where x.id = :id")
    java.util.Optional<Order> findLocked(@org.springframework.data.repository.query.Param("id") String id);
    @Query("select distinct o from Order o join o.items i join i.variant v where o.userId=:userId and o.status=com.ankkun.ecommerce.common.enums.OrderStatus.delivered and v.productId=:productId order by o.createdAt desc")
    List<Order> findDeliveredContaining(@org.springframework.data.repository.query.Param("userId") String userId,@org.springframework.data.repository.query.Param("productId") String productId);
}
