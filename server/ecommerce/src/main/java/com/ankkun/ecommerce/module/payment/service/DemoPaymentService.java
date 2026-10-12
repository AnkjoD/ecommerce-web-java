package com.ankkun.ecommerce.module.payment.service;
import com.ankkun.ecommerce.common.exception.*;
import com.ankkun.ecommerce.common.enums.*;
import com.ankkun.ecommerce.module.order.entity.*;
import com.ankkun.ecommerce.module.order.repository.*;
import com.ankkun.ecommerce.module.order.service.OrderService;
import com.ankkun.ecommerce.module.payment.entity.SavedPaymentMethod;
import com.ankkun.ecommerce.module.payment.repository.SavedPaymentMethodRepository;
import com.ankkun.ecommerce.module.mail.DemoMailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
@Service @RequiredArgsConstructor @Transactional @ConditionalOnProperty(name="app.demo-mode",havingValue="true")
public class DemoPaymentService {
    private final OrderRepository orders;private final PaymentRepository payments;private final OrderService orderService;private final SavedPaymentMethodRepository methods;private final DemoMailService mail;
    public Map<String,Object> create(String userId,String orderId,String provider,boolean save) {
        var order=owned(userId,orderId);
        if(order.getPaymentMethod()==PaymentMethod.cod||!order.getPaymentMethod().name().equals(provider))throw new BadRequestException("Phương thức không khớp đơn hàng");
        if(order.getPaymentStatus()==PaymentStatus.paid||!Set.of(OrderStatus.pending,OrderStatus.payment_failed).contains(order.getStatus()))throw new BadRequestException("Đơn hàng không thể thanh toán");
        var p=payments.findByOrderId(orderId).orElse(Payment.builder().orderId(orderId).build());String nonce=UUID.randomUUID().toString()+UUID.randomUUID();
        p.setProvider(provider);p.setProviderOrderId(order.getOrderCode());p.setAmount(order.getTotal());p.setStatus("pending");p.setSaveMethod(save);p.setNonceHash(hash(nonce));p.setNonceExpiresAt(Instant.now().plusSeconds(900));payments.saveAndFlush(p);
        order.setStatus(OrderStatus.pending);order.setPaymentStatus(PaymentStatus.unpaid);
        return Map.of("url","/payment-demo/"+orderId+"?nonce="+nonce,"params",Map.of(),"mode","demo");
    }
    public Object preview(String userId,String id,String nonce) {
        var order=owned(userId,id);var p=payments.findByOrderId(id).orElseThrow(()->new NotFoundException("Payment not found"));check(p,nonce);
        return Map.of("order",orderService.getMyOrder(id,userId),"provider",p.getProvider(),"mode","demo","save_method",Boolean.TRUE.equals(p.getSaveMethod()),"expires_at",p.getNonceExpiresAt());
    }
    public Object complete(String userId,String id,String nonce,String outcome) {
        if(!Set.of("success","failure").contains(outcome))throw new BadRequestException("Kết quả không hợp lệ");
        var order=owned(userId,id);var p=payments.findByOrderId(id).orElseThrow(()->new NotFoundException("Payment not found"));check(p,nonce);
        if(order.getStatus()==OrderStatus.cancelled)throw new BadRequestException("Đơn hàng đã hủy");
        if(!p.getStatus().equals("pending"))return Map.of("isSuccess",order.getPaymentStatus()==PaymentStatus.paid,"order_code",order.getOrderCode(),"mode","demo");
        boolean success=outcome.equals("success");p.setStatus(success?"success":"failed");p.setTransactionId("DEMO-"+UUID.randomUUID());p.setPaidAt(success?Instant.now():null);
        order.setPaymentStatus(success?PaymentStatus.paid:PaymentStatus.failed);
        if(success){order.setStatus(OrderStatus.pending);orderService.advance(id,OrderStatus.confirmed);
            if(Boolean.TRUE.equals(p.getSaveMethod()))methods.save(SavedPaymentMethod.builder().userId(userId).provider(p.getProvider()).bankCode("DEMO-BANK").build());
        }else{order.setStatus(OrderStatus.payment_failed);mail.send(userId,"Thanh toán demo thất bại "+order.getOrderCode(),"Bạn có thể thử lại hoặc hủy đơn. Không có tiền thật được giao dịch.");}
        return Map.of("isSuccess",success,"order_code",order.getOrderCode(),"amount",order.getTotal(),"mode","demo");
    }
    private Order owned(String userId,String id){return orders.findLocked(id).filter(o->o.getUserId().equals(userId)).orElseThrow(()->new NotFoundException("Order not found"));}
    private void check(Payment p,String nonce){if(nonce==null||p.getNonceHash()==null||!MessageDigest.isEqual(hash(nonce).getBytes(StandardCharsets.UTF_8),p.getNonceHash().getBytes(StandardCharsets.UTF_8)))throw new ForbiddenException("Phiên thanh toán không hợp lệ");if(p.getNonceExpiresAt().isBefore(Instant.now()))throw new BadRequestException("Phiên thanh toán hết hạn");}
    private String hash(String s){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException(e);}}
}
