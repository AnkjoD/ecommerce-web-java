package com.ankkun.ecommerce.module.order.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import java.math.BigDecimal;

@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Data
public class OrderItemResponse {
    private String id;
    private String orderId;
    private String variantInfo;
    private com.ankkun.ecommerce.module.product.dto.ProductVariantResponse variant;
    private String variantId;
    private String productName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;
}
