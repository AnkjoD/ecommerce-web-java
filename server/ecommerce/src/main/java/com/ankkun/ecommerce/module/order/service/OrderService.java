package com.ankkun.ecommerce.module.order.service;
import com.ankkun.ecommerce.common.dto.PaginatedResponse;
import com.ankkun.ecommerce.common.enums.*;
import com.ankkun.ecommerce.common.exception.*;
import com.ankkun.ecommerce.module.address.repository.AddressRepository;
import com.ankkun.ecommerce.module.cart.repository.CartRepository;
import com.ankkun.ecommerce.module.coupon.service.CouponService;
import com.ankkun.ecommerce.module.order.dto.*;
import com.ankkun.ecommerce.module.order.entity.*;
import com.ankkun.ecommerce.module.order.repository.*;
import com.ankkun.ecommerce.module.product.entity.ProductVariant;
import com.ankkun.ecommerce.module.product.repository.ProductVariantRepository;
import com.ankkun.ecommerce.module.product.service.ProductService;
import com.ankkun.ecommerce.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    private final com.fasterxml.jackson.databind.ObjectMapper json;
    private final OrderRepository orders;
    private final OrderItemRepository orderItems;
    private final CartRepository carts;
    private final AddressRepository addresses;
    private final ProductVariantRepository variants;
    private final UserRepository users;
    private final ProductService products;
    private final CouponService coupons;
    private final com.ankkun.ecommerce.module.mail.DemoMailService mail;
    @org.springframework.beans.factory.annotation.Value("${app.demo-mode:false}")private boolean demo;
    private final com.ankkun.ecommerce.module.payment.service.PaymentService payments;
    public OrderResponse createFromCart(String userId, OrderCreateRequest req) {
        users.findLocked(userId).orElseThrow(() -> new UnauthorizedException("User not found"));
        var cart = carts.findByUserId(userId).orElseThrow(() -> new BadRequestException("Giỏ hàng trống"));
        var selectedItems = req.getItemIds() == null ? List.copyOf(cart.getItems())
                : cart.getItems().stream().filter(item -> req.getItemIds().contains(item.getId())).toList();
        if (selectedItems.isEmpty()) throw new BadRequestException("Giỏ hàng trống");
        if (req.getItemIds() != null && selectedItems.size() != new HashSet<>(req.getItemIds()).size())
            throw new BadRequestException("Mục giỏ hàng không hợp lệ");
        var address = addresses.findByIdAndUserId(req.getAddressId(), userId).orElseThrow(() -> new ForbiddenException("Địa chỉ không hợp lệ"));
        PaymentMethod method;
        try { method = PaymentMethod.valueOf(req.getPaymentMethod().toLowerCase()); }
        catch (Exception e) { throw new BadRequestException("Phương thức thanh toán không hợp lệ"); }
        if (!demo && method == PaymentMethod.momo) throw new BadRequestException("MoMo chưa được hỗ trợ");
        if (!demo && method == PaymentMethod.vnpay && !payments.enabled()) throw new BadRequestException("VNPay chưa được cấu hình");
        Map<String, ProductVariant> locked = new HashMap<>();
        selectedItems.stream().map(i -> i.getVariantId()).sorted().forEach(id -> locked.put(id,
                variants.findLocked(id).orElseThrow(() -> new NotFoundException("Variant not found"))));
        BigDecimal subtotal = BigDecimal.ZERO;
        for (var item : selectedItems) {
            var variant = locked.get(item.getVariantId());
            if (!variant.getIsActive() || !"active".equals(variant.getProduct().getStatus()) || item.getQuantity() <= 0
                    || variant.getStockQuantity() - variant.getReservedQuantity() < item.getQuantity()) throw new BadRequestException("Không đủ hàng trong kho");
            subtotal = subtotal.add(variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        BigDecimal discount = coupons.consume(req.getCouponCode(), subtotal);
        String province = address.getProvince().toLowerCase();
        BigDecimal shipping = subtotal.subtract(discount).compareTo(new BigDecimal("500000")) >= 0 ? BigDecimal.ZERO
                : new BigDecimal(province.contains("hồ chí minh") ? "20000" : province.contains("hà nội") || province.contains("đà nẵng") ? "30000" : "40000");
        Order order = orders.save(Order.builder().userId(userId).addressId(address.getId())
                .addressSnapshot(json.valueToTree(com.ankkun.ecommerce.module.address.dto.AddressResponse.from(address)).toString())
                .orderCode("ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .paymentMethod(method).status(OrderStatus.pending).paymentStatus(PaymentStatus.unpaid)
                .subtotal(subtotal).shippingFee(shipping).discountAmount(discount).total(subtotal.subtract(discount).add(shipping)).build());
        for (var item : selectedItems) {
            var variant = locked.get(item.getVariantId());
            variant.setStockQuantity(variant.getStockQuantity() - item.getQuantity());
            OrderItem row = orderItems.save(OrderItem.builder().orderId(order.getId()).variantId(variant.getId()).variant(variant)
                    .productName(variant.getProduct().getName()).variantInfo(variant.getOptions()).unitPrice(variant.getPrice())
                    .quantity(item.getQuantity()).subtotal(variant.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))).build());
            order.getItems().add(row);
        }
        cart.getItems().removeAll(selectedItems);
        mail.send(userId,"Đơn hàng "+order.getOrderCode(),"Đã tạo đơn "+order.getOrderCode()+". Tổng tiền: "+order.getTotal()+"đ. Đây là email demo, không gửi ra ngoài.");
        return toResponse(order);
    }
    @Transactional(readOnly=true)
    public PaginatedResponse<OrderResponse> history(String userId, String status, int page, int limit) {
        page = Math.max(1, page); limit = Math.max(1, Math.min(100, limit));
        var pageable = PageRequest.of(page - 1, limit, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = status == null || status.equals("all") ? orders.findByUserId(userId, pageable)
                : orders.findByUserIdAndStatusIn(userId,Arrays.stream(status.split(",")).map(OrderStatus::valueOf).toList(),pageable);
        return PaginatedResponse.of(result.getContent().stream().map(this::toResponse).toList(), page, limit, result.getTotalElements());
    }
    @Transactional(readOnly=true)
    public OrderResponse getMyOrder(String orderId, String userId) {
        return toResponse(orders.findByIdAndUserId(orderId, userId).orElseThrow(() -> new NotFoundException("Order not found")));
    }
    public OrderResponse cancelOrder(String orderId, String userId) {
        Order order = orders.findLocked(orderId).filter(o -> o.getUserId().equals(userId)).orElseThrow(() -> new NotFoundException("Order not found"));
        if ((order.getStatus() != OrderStatus.pending && order.getStatus() != OrderStatus.confirmed && order.getStatus() != OrderStatus.payment_failed) || order.getPaymentStatus() == PaymentStatus.paid)
            throw new BadRequestException("Không thể hủy đơn hàng lúc này");
        order.getItems().stream().sorted(Comparator.comparing(OrderItem::getVariantId)).forEach(item -> {
            var variant = variants.findLocked(item.getVariantId()).orElseThrow();
            variant.setStockQuantity(variant.getStockQuantity() + item.getQuantity());
        });
        order.setStatus(OrderStatus.cancelled);mail.send(userId,"Hủy đơn "+order.getOrderCode(),"Đã hủy đơn và trả lại tồn kho."); return toResponse(order);
    }
    public PaginatedResponse<OrderResponse> adminHistory(String status, String search, String userId, int page, int limit) {
        page = Math.max(1, page); limit = Math.max(1, Math.min(100, limit));
        var pageable = PageRequest.of(page-1, limit, Sort.by(Sort.Direction.DESC,"createdAt"));
        org.springframework.data.jpa.domain.Specification<Order> spec=(root,query,cb)->{var ps=new ArrayList<jakarta.persistence.criteria.Predicate>();if(status!=null&&!status.equals("all"))ps.add(cb.equal(root.get("status"),OrderStatus.valueOf(status)));if(userId!=null&&!userId.isBlank())ps.add(cb.equal(root.get("userId"),userId));if(search!=null&&!search.isBlank()){var user=root.join("user");String q="%"+search.toLowerCase()+"%";ps.add(cb.or(cb.like(cb.lower(root.get("orderCode")),q),cb.like(cb.lower(user.get("email")),q),cb.like(cb.lower(user.get("fullName")),q)));}return cb.and(ps.toArray(new jakarta.persistence.criteria.Predicate[0]));};
        var result=orders.findAll(spec,pageable);
        return PaginatedResponse.of(result.getContent().stream().map(this::toResponse).toList(),page,limit,result.getTotalElements());
    }
    public OrderResponse advance(String id, OrderStatus next) {
        var order = orders.findLocked(id).orElseThrow(() -> new NotFoundException("Order not found"));
        if (next == OrderStatus.cancelled) return cancelOrder(id,order.getUserId());
        var flow = List.of(OrderStatus.pending,OrderStatus.confirmed,OrderStatus.processing,OrderStatus.shipping,OrderStatus.delivered);
        int position = flow.indexOf(order.getStatus());
        if (position < 0 || position >= flow.size()-1 || next != flow.get(position+1)) throw new BadRequestException("Trạng thái đơn hàng không hợp lệ");
        if (order.getPaymentMethod() != PaymentMethod.cod && order.getPaymentStatus() != PaymentStatus.paid) throw new BadRequestException("Đơn hàng chưa thanh toán");
        order.setStatus(next);
        if(demo){if(next==OrderStatus.confirmed&&order.getGhtkLabel()==null)order.setGhtkLabel("DEMO-GHTK-"+order.getOrderCode());order.setShippingLog("{\"mode\":\"demo\",\"status\":\""+next.name()+"\",\"updated_at\":\""+java.time.Instant.now()+"\"}");}
        mail.send(order.getUserId(),"Cập nhật đơn "+order.getOrderCode(),"Trạng thái: "+next.name()+". Vận chuyển mô phỏng local.");
        if (next == OrderStatus.delivered) {
            order.setPaymentStatus(PaymentStatus.paid);
            for (var item : order.getItems()) {
                var variant = variants.findLocked(item.getVariantId()).orElseThrow(); variant.setSoldCount(variant.getSoldCount()+item.getQuantity());
                var product = variant.getProduct(); product.setSoldCount(product.getSoldCount()+item.getQuantity());
            }
        }
        return toResponse(order);
    }
    public OrderResponse updatePayment(String id,PaymentStatus next){
        var order=orders.findLocked(id).orElseThrow(()->new NotFoundException("Order not found"));
        if(next==order.getPaymentStatus())return toResponse(order);
        if(next==PaymentStatus.paid){if(order.getPaymentMethod()!=PaymentMethod.cod||order.getStatus()==OrderStatus.cancelled||order.getPaymentStatus()==PaymentStatus.refunded)throw new BadRequestException("Chỉ xác nhận thu tiền COD; thanh toán online dùng màn hình demo");order.setPaymentStatus(next);}
        else if(next==PaymentStatus.refunded){if(order.getPaymentStatus()!=PaymentStatus.paid||!Set.of(OrderStatus.pending,OrderStatus.confirmed,OrderStatus.delivered).contains(order.getStatus()))throw new BadRequestException("Chỉ hoàn tiền đơn đã trả tiền, chưa gửi hoặc đã trả hàng sau giao");
            boolean delivered=order.getStatus()==OrderStatus.delivered;
            order.getItems().stream().sorted(Comparator.comparing(OrderItem::getVariantId)).forEach(item->{var v=variants.findLocked(item.getVariantId()).orElseThrow();v.setStockQuantity(v.getStockQuantity()+item.getQuantity());if(delivered){v.setSoldCount(Math.max(0,v.getSoldCount()-item.getQuantity()));v.getProduct().setSoldCount(Math.max(0,v.getProduct().getSoldCount()-item.getQuantity()));}});
            order.setPaymentStatus(next);order.setStatus(OrderStatus.cancelled);
        }else throw new BadRequestException("Không thể sửa trạng thái thanh toán theo cách này");
        mail.send(order.getUserId(),"Thanh toán đơn "+order.getOrderCode(),"Trạng thái local: "+next.name());return toResponse(order);
    }
    private OrderResponse toResponse(Order order) {
        OrderResponse r = new OrderResponse(); r.setId(order.getId()); r.setUserId(order.getUserId()); r.setAddressId(order.getAddressId());
        if(order.getAddressSnapshot()!=null)r.setAddress(json.convertValue(com.ankkun.ecommerce.common.dto.JsonData.parse(order.getAddressSnapshot()),com.ankkun.ecommerce.module.address.dto.AddressResponse.class));else r.setAddress(com.ankkun.ecommerce.module.address.dto.AddressResponse.from(order.getAddress() != null ? order.getAddress() : addresses.findById(order.getAddressId()).orElseThrow()));
        r.setUser(com.ankkun.ecommerce.module.user.service.UserService.toResponse(users.findById(order.getUserId()).orElseThrow()));
        r.setGhtkLabel(order.getGhtkLabel());r.setShippingLog(com.ankkun.ecommerce.common.dto.JsonData.parse(order.getShippingLog()));
        r.setOrderCode(order.getOrderCode()); r.setStatus(order.getStatus()); r.setPaymentStatus(order.getPaymentStatus()); r.setPaymentMethod(order.getPaymentMethod());
        r.setSubtotal(order.getSubtotal()); r.setShippingFee(order.getShippingFee()); r.setDiscountAmount(order.getDiscountAmount()); r.setTotal(order.getTotal());
        r.setCreatedAt(order.getCreatedAt()); r.setUpdatedAt(order.getUpdatedAt());
        r.setItems(order.getItems().stream().map(item -> {
            OrderItemResponse row = new OrderItemResponse(); row.setId(item.getId()); row.setOrderId(order.getId()); row.setVariantId(item.getVariantId());
            row.setProductName(item.getProductName()); row.setVariantInfo(item.getVariantInfo()); row.setUnitPrice(item.getUnitPrice()); row.setQuantity(item.getQuantity()); row.setSubtotal(item.getSubtotal());
            row.setVariant(products.toVariantResponse(item.getVariant())); return row;
        }).toList()); return r;
    }
}
