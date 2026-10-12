package com.ankkun.ecommerce.module.payment.service;

import com.ankkun.ecommerce.common.enums.OrderStatus;
import com.ankkun.ecommerce.common.enums.PaymentStatus;
import com.ankkun.ecommerce.common.exception.BadRequestException;
import com.ankkun.ecommerce.module.order.entity.Order;
import com.ankkun.ecommerce.module.order.entity.Payment;
import com.ankkun.ecommerce.module.order.repository.OrderRepository;
import com.ankkun.ecommerce.module.order.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Value("${vnpay.tmn-code}") private String tmnCode;
    @Value("${vnpay.hash-secret}") private String hashSecret;
    @Value("${vnpay.return-url}") private String returnUrl;
    @Value("${vnpay.payment-url}") private String vnpUrl;

    public boolean enabled() { return !tmnCode.isBlank() && !hashSecret.isBlank(); }
    public Map<String, Object> createPaymentUrl(String userId, String orderId, String ipAddr) {
        if (!enabled()) throw new BadRequestException("VNPay chưa được cấu hình");
        Order order = orderRepository.findByIdAndUserId(orderId, userId).orElseThrow(() -> new BadRequestException("Đơn hàng không tồn tại"));

        if (order.getPaymentMethod() != com.ankkun.ecommerce.common.enums.PaymentMethod.vnpay
                || order.getPaymentStatus() == PaymentStatus.paid || order.getStatus() == OrderStatus.cancelled)
            throw new BadRequestException("Đơn hàng không thể thanh toán VNPay");
        String vnp_Version = "2.1.0";
        String vnp_Command = "pay";
        String vnp_TxnRef = order.getOrderCode();
        String vnp_IpAddr = "0:0:0:0:0:0:0:1".equals(ipAddr) || "::1".equals(ipAddr) ? "127.0.0.1" : ipAddr;
        String vnp_TmnCode = tmnCode;

        long amount = order.getTotal().longValue() * 100;
        Map<String, String> vnp_Params = new HashMap<>();
        vnp_Params.put("vnp_Version", vnp_Version);
        vnp_Params.put("vnp_Command", vnp_Command);
        vnp_Params.put("vnp_TmnCode", vnp_TmnCode);
        vnp_Params.put("vnp_Amount", String.valueOf(amount));
        vnp_Params.put("vnp_CurrCode", "VND");
        vnp_Params.put("vnp_TxnRef", vnp_TxnRef);
        vnp_Params.put("vnp_OrderInfo", "Thanh toan don hang " + vnp_TxnRef);
        vnp_Params.put("vnp_OrderType", "other");
        vnp_Params.put("vnp_Locale", "vn");
        vnp_Params.put("vnp_ReturnUrl", returnUrl);
        vnp_Params.put("vnp_IpAddr", vnp_IpAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        String vnp_CreateDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_CreateDate", vnp_CreateDate);

        cld.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = formatter.format(cld.getTime());
        vnp_Params.put("vnp_ExpireDate", vnp_ExpireDate);

        List<String> fieldNames = new ArrayList<>(vnp_Params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();

        for (String fieldName : fieldNames) {
            String fieldValue = vnp_Params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.UTF_8)).append('=')
                     .append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                hashData.append('&');
                query.append('&');
            }
        }
        hashData.setLength(hashData.length() - 1);
        query.setLength(query.length() - 1);

        String queryUrl = query.toString();
        String vnp_SecureHash = hmacSHA512(hashSecret, hashData.toString());
        queryUrl += "&vnp_SecureHash=" + vnp_SecureHash;
        String paymentUrl = vnpUrl + "?" + queryUrl;

        return Map.of("url", paymentUrl, "params", vnp_Params);
    }

    @Transactional
    public Object vnpayReturn(Map<String, String> params) {
        if (!enabled()) throw new BadRequestException("VNPay chưa được cấu hình");
        String secureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.UTF_8));
                hashData.append('&');
            }
        }
        if (hashData.length() > 0) hashData.setLength(hashData.length() - 1);

        String signValue = hmacSHA512(hashSecret, hashData.toString());
        if (secureHash == null || !java.security.MessageDigest.isEqual(signValue.getBytes(StandardCharsets.UTF_8), secureHash.getBytes(StandardCharsets.UTF_8))) throw new BadRequestException("Invalid signature");

        String orderCode = params.get("vnp_TxnRef");
        Order order = orderRepository.findByOrderCode(orderCode).orElseThrow(() -> new BadRequestException("Order not found"));

        order = orderRepository.findLocked(order.getId()).orElseThrow();
        if (!tmnCode.equals(params.get("vnp_TmnCode")) || !order.getTotal().multiply(new java.math.BigDecimal("100")).toBigIntegerExact().toString().equals(params.get("vnp_Amount")))
            throw new BadRequestException("Invalid payment amount or merchant");
        if (order.getStatus() == OrderStatus.cancelled) throw new BadRequestException("Order cancelled");
        if (order.getPaymentStatus() == PaymentStatus.paid) return Map.of("isSuccess", true, "order_code", order.getOrderCode(), "amount", order.getTotal());
        if ("00".equals(params.get("vnp_ResponseCode")) && "00".equals(params.get("vnp_TransactionStatus"))) {
            order.setPaymentStatus(PaymentStatus.paid);
            if (order.getStatus() == OrderStatus.pending) order.setStatus(OrderStatus.confirmed);
            orderRepository.save(order);

            Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(Payment.builder().orderId(order.getId()).build());
            payment.setProvider("vnpay");
            payment.setTransactionId(params.get("vnp_TransactionNo"));
            payment.setAmount(order.getTotal());
            payment.setStatus("success");
            payment.setPaidAt(Instant.now());
            paymentRepository.save(payment);

            return Map.of("isSuccess", true, "message", "Giao dịch thành công", "order_code", order.getOrderCode(), "amount", order.getTotal());
        } else {
            order.setPaymentStatus(PaymentStatus.failed);
            orderRepository.save(order);
            return Map.of("isSuccess", false, "message", "Giao dịch thất bại", "order_code", order.getOrderCode(), "amount", order.getTotal());
        }
    }

    private String hmacSHA512(String key, String data) {
        try {
            Mac hmac512 = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            hmac512.init(secretKey);
            byte[] result = hmac512.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(2 * result.length);
            for (byte b : result) {
                sb.append(String.format("%02x", b & 0xff));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot sign payment request", ex);
        }
    }
}
