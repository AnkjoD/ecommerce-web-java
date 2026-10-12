package com.ankkun.ecommerce.module.order.dto;

import com.ankkun.ecommerce.common.enums.OrderStatus;
import com.ankkun.ecommerce.common.enums.PaymentMethod;
import com.ankkun.ecommerce.common.enums.PaymentStatus;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class OrderResponse {
    private String id;
    private String ghtkLabel;
    private com.fasterxml.jackson.databind.JsonNode shippingLog;
    private com.ankkun.ecommerce.module.address.dto.AddressResponse address;
    private com.ankkun.ecommerce.module.user.dto.UserResponse user;
    private String userId;
    private String addressId;
    private Instant updatedAt;
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
