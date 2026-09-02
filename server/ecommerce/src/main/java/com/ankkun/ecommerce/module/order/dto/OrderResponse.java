package com.ankkun.ecommerce.module.order.dto;

import com.ankkun.ecommerce.common.enums.OrderStatus;
import com.ankkun.ecommerce.common.enums.PaymentMethod;
import com.ankkun.ecommerce.common.enums.PaymentStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
public class OrderResponse {
    private String id;
    private String orderCode;
    private OrderStatus status;
    private PaymentStatus paymentStatus;
    private PaymentMethod paymentMethod;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal total;
    private Instant createdAt;
    private List<OrderItemResponse> items;
}
