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

    public Map<String, Object> createPaymentUrl(String orderId, String ipAddr) {
        Order order = orderRepository.findById(orderId).orElseThrow(() -> new BadRequestException("Đơn hàng không tồn tại"));

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

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
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
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(fieldName, StandardCharsets.US_ASCII)).append('=')
                     .append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
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
        String secureHash = params.get("vnp_SecureHash");
        params.remove("vnp_SecureHash");
        params.remove("vnp_SecureHashType");

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        for (String fieldName : fieldNames) {
            String fieldValue = params.get(fieldName);
            if ((fieldValue != null) && (!fieldValue.isEmpty())) {
                hashData.append(fieldName).append('=').append(URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII));
                hashData.append('&');
            }
        }
        if (hashData.length() > 0) hashData.setLength(hashData.length() - 1);

        String signValue = hmacSHA512(hashSecret, hashData.toString());
        if (!signValue.equals(secureHash)) throw new BadRequestException("Invalid signature");

        String orderCode = params.get("vnp_TxnRef");
        Order order = orderRepository.findByOrderCode(orderCode).orElseThrow(() -> new BadRequestException("Order not found"));

        if ("00".equals(params.get("vnp_ResponseCode"))) {
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

            return Map.of("success", true, "message", "Giao dịch thành công", "order_id", order.getId());
        } else {
            order.setPaymentStatus(PaymentStatus.failed);
            orderRepository.save(order);
            return Map.of("success", false, "message", "Giao dịch thất bại");
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
            return "";
        }
    }
}
