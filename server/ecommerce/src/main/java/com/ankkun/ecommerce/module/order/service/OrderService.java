package com.ankkun.ecommerce.module.order.service;

import com.ankkun.ecommerce.common.enums.OrderStatus;
import com.ankkun.ecommerce.common.enums.PaymentMethod;
import com.ankkun.ecommerce.common.enums.PaymentStatus;
import com.ankkun.ecommerce.common.exception.BadRequestException;
import com.ankkun.ecommerce.common.exception.ForbiddenException;
import com.ankkun.ecommerce.common.exception.NotFoundException;
import com.ankkun.ecommerce.module.address.entity.Address;
import com.ankkun.ecommerce.module.address.repository.AddressRepository;
import com.ankkun.ecommerce.module.cart.entity.Cart;
import com.ankkun.ecommerce.module.cart.entity.CartItem;
import com.ankkun.ecommerce.module.cart.repository.CartItemRepository;
import com.ankkun.ecommerce.module.cart.repository.CartRepository;
import com.ankkun.ecommerce.module.order.dto.OrderCreateRequest;
import com.ankkun.ecommerce.module.order.dto.OrderItemResponse;
import com.ankkun.ecommerce.module.order.dto.OrderResponse;
import com.ankkun.ecommerce.module.order.entity.Order;
import com.ankkun.ecommerce.module.order.entity.OrderItem;
import com.ankkun.ecommerce.module.order.repository.OrderItemRepository;
import com.ankkun.ecommerce.module.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final AddressRepository addressRepository;

    @Transactional
    public OrderResponse createFromCart(String userId, OrderCreateRequest dto) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new BadRequestException("Giỏ hàng trống"));
        List<CartItem> items = cart.getItems();
        if (items.isEmpty()) throw new BadRequestException("Giỏ hàng trống");

        String addressId = dto.getAddressId();
        Address address = addressRepository.findById(addressId).orElseThrow(() -> new ForbiddenException("Địa chỉ không hợp lệ"));
        if (!address.getUserId().equals(userId)) throw new ForbiddenException("Địa chỉ không hợp lệ");

        PaymentMethod pm = PaymentMethod.valueOf(dto.getPaymentMethod().toLowerCase());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem ci : items) {
            subtotal = subtotal.add(ci.getVariant().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())));
        }

        BigDecimal shippingFee = new BigDecimal("30000"); // mock
        BigDecimal discount = BigDecimal.ZERO;
        BigDecimal total = subtotal.add(shippingFee).subtract(discount);

        Order order = Order.builder()
                .userId(userId)
                .addressId(addressId)
                .orderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paymentMethod(pm)
                .status(OrderStatus.pending)
                .paymentStatus(PaymentStatus.unpaid)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discount)
                .total(total)
                .build();
        order = orderRepository.save(order);

        for (CartItem ci : items) {
            OrderItem oi = OrderItem.builder()
                    .orderId(order.getId())
                    .variantId(ci.getVariantId())
                    .productName(ci.getVariant().getProduct().getName())
                    .unitPrice(ci.getVariant().getPrice())
                    .quantity(ci.getQuantity())
                    .subtotal(ci.getVariant().getPrice().multiply(BigDecimal.valueOf(ci.getQuantity())))
                    .build();
            orderItemRepository.save(oi);
        }

        cartItemRepository.deleteByCartId(cart.getId()); // clear cart

        return toResponse(orderRepository.findById(order.getId()).orElseThrow());
    }

    public OrderResponse getMyOrder(String orderId, String userId) {
        return toResponse(orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order not found")));
    }

    @Transactional
    public OrderResponse cancelOrder(String orderId, String userId) {
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new NotFoundException("Order not found"));
        if (order.getStatus() != OrderStatus.pending && order.getStatus() != OrderStatus.confirmed) {
            throw new BadRequestException("Không thể hủy đơn hàng lúc này");
        }
        order.setStatus(OrderStatus.cancelled);
        return toResponse(orderRepository.save(order));
    }

    private OrderResponse toResponse(Order order) {
        OrderResponse r = new OrderResponse();
        r.setId(order.getId());
        r.setOrderCode(order.getOrderCode());
        r.setStatus(order.getStatus());
        r.setPaymentStatus(order.getPaymentStatus());
        r.setPaymentMethod(order.getPaymentMethod());
        r.setSubtotal(order.getSubtotal());
        r.setShippingFee(order.getShippingFee());
        r.setDiscountAmount(order.getDiscountAmount());
        r.setTotal(order.getTotal());
        r.setCreatedAt(order.getCreatedAt());
        
        if (order.getItems() != null) {
            r.setItems(order.getItems().stream().map(this::toItemResponse).collect(Collectors.toList()));
        }
        return r;
    }

    private OrderItemResponse toItemResponse(OrderItem item) {
        OrderItemResponse r = new OrderItemResponse();
        r.setId(item.getId());
        r.setVariantId(item.getVariantId());
        r.setProductName(item.getProductName());
        r.setUnitPrice(item.getUnitPrice());
        r.setQuantity(item.getQuantity());
        r.setSubtotal(item.getSubtotal());
        return r;
    }
}
