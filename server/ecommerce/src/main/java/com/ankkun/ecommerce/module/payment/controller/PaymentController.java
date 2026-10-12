package com.ankkun.ecommerce.module.payment.controller;
import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.common.exception.ServiceUnavailableException;
import com.ankkun.ecommerce.module.payment.service.*;
import com.ankkun.ecommerce.module.payment.repository.SavedPaymentMethodRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
@RestController @RequestMapping("/api/payments") @RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;private final ObjectProvider<DemoPaymentService> demoPayments;private final SavedPaymentMethodRepository methods;
    private DemoPaymentService demo(){var d=demoPayments.getIfAvailable();if(d==null)throw new ServiceUnavailableException("Thanh toán demo chỉ hoạt động trong profile demo");return d;}
    @GetMapping("/config")public Object config(){boolean local=demoPayments.getIfAvailable()!=null;return ApiResponse.ok(Map.of("vnpay",local||paymentService.enabled(),"momo",local,"mode",local?"demo":"live"));}
    @PostMapping("/create-url")public Object create(@AuthenticationPrincipal String userId,@RequestBody Map<String,String> body,@RequestParam(defaultValue="false")boolean saveCard,HttpServletRequest req){var d=demoPayments.getIfAvailable();return ApiResponse.ok(d!=null?d.create(userId,body.get("order_id"),"vnpay",saveCard):paymentService.createPaymentUrl(userId,body.get("order_id"),req.getRemoteAddr()));}
    @PostMapping("/momo/create/{id}")public Object momo(@AuthenticationPrincipal String userId,@PathVariable String id,@RequestParam(defaultValue="false")boolean saveCard){return ApiResponse.ok(demo().create(userId,id,"momo",saveCard).get("url"));}
    @GetMapping("/demo/{id}")public Object preview(@AuthenticationPrincipal String userId,@PathVariable String id,@RequestParam String nonce){return ApiResponse.ok(demo().preview(userId,id,nonce));}
    public record Completion(@NotBlank String nonce,@NotBlank String outcome){}
    @PostMapping("/demo/{id}/complete")public Object complete(@AuthenticationPrincipal String userId,@PathVariable String id,@Valid @RequestBody Completion c){return ApiResponse.ok(demo().complete(userId,id,c.nonce(),c.outcome()));}
    @GetMapping("/methods")public Object methods(@AuthenticationPrincipal String userId){return ApiResponse.ok(methods.findByUserIdOrderByCreatedAtDesc(userId).stream().map(m->Map.of("id",m.getId(),"provider",m.getProvider(),"bank_code",m.getBankCode(),"card_type",m.getCardType(),"created_at",m.getCreatedAt(),"mode","demo")).toList());}
    @DeleteMapping("/methods/{id}")@Transactional public Object delete(@AuthenticationPrincipal String userId,@PathVariable String id){var m=methods.findById(id).filter(x->x.getUserId().equals(userId)).orElseThrow(()->new com.ankkun.ecommerce.common.exception.NotFoundException("Method not found"));methods.delete(m);return ApiResponse.ok(null);}
    @GetMapping({"/vnpay-return","/vnpay/callback"})public Object callback(@RequestParam Map<String,String> params){if(demoPayments.getIfAvailable()!=null)throw new com.ankkun.ecommerce.common.exception.BadRequestException("Dùng màn hình thanh toán demo local");return ApiResponse.ok(paymentService.vnpayReturn(params));}
}
