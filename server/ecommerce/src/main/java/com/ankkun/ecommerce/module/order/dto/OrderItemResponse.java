package com.ankkun.ecommerce.module.order.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class OrderItemResponse {
    private String id;
    private String variantId;
    private String productName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;
}
