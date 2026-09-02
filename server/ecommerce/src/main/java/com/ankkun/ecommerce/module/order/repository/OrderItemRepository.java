package com.ankkun.ecommerce.module.order.repository;
import com.ankkun.ecommerce.module.order.entity.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;
public interface OrderItemRepository extends JpaRepository<OrderItem, String> { }
