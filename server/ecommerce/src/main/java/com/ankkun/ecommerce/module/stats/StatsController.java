package com.ankkun.ecommerce.module.stats;
import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.common.enums.*;
import com.ankkun.ecommerce.module.order.repository.OrderRepository;
import com.ankkun.ecommerce.module.user.repository.UserRepository;
import com.ankkun.ecommerce.module.product.repository.ProductRepository;
import com.ankkun.ecommerce.module.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
@RestController @RequiredArgsConstructor
public class StatsController {
    private final OrderRepository orders;private final UserRepository users;private final ProductRepository products;private final ReviewRepository reviews;
    @GetMapping("/api/admin/stats/overview")@Transactional(readOnly=true)public Object overview(){
        ZoneId zone=ZoneId.of("Asia/Ho_Chi_Minh");LocalDate today=LocalDate.now(zone);var all=orders.findAll();var paid=all.stream().filter(o->o.getPaymentStatus()==PaymentStatus.paid).toList();
        Map<String,Object> data=new LinkedHashMap<>();data.put("total_revenue",paid.stream().map(o->o.getTotal()).reduce(BigDecimal.ZERO,BigDecimal::add));data.put("total_orders",all.size());data.put("total_users",users.count());data.put("total_products",products.count());
        var history=new ArrayList<Map<String,Object>>();for(int i=29;i>=0;i--){var day=today.minusDays(i);var daily=all.stream().filter(o->o.getCreatedAt().atZone(zone).toLocalDate().equals(day)).toList();var revenue=daily.stream().filter(o->o.getPaymentStatus()==PaymentStatus.paid).map(o->o.getTotal()).reduce(BigDecimal.ZERO,BigDecimal::add);history.add(Map.of("label",day.toString(),"date",day.toString(),"count",daily.size(),"orders",daily.size(),"revenue",revenue));}
        data.put("history",history);data.put("today",Map.of("revenue",history.getLast().get("revenue"),"orders",history.getLast().get("count")));data.put("yesterday",Map.of("revenue",history.get(28).get("revenue"),"orders",history.get(28).get("count")));
        data.put("status_distribution",Arrays.stream(OrderStatus.values()).map(s->Map.of("status",s.name(),"count",all.stream().filter(o->o.getStatus()==s).count())).toList());
        Map<String,Map<String,Object>> top=new HashMap<>();for(var o:paid)for(var item:o.getItems()){String id=item.getVariant().getProductId();var row=top.computeIfAbsent(id,x->new HashMap<>(Map.of("id",id,"name",item.getProductName(),"sold",0,"revenue",BigDecimal.ZERO)));row.put("sold",((Integer)row.get("sold"))+item.getQuantity());row.put("revenue",((BigDecimal)row.get("revenue")).add(item.getSubtotal()));}
        data.put("top_products",top.values().stream().sorted((a,b)->((BigDecimal)b.get("revenue")).compareTo((BigDecimal)a.get("revenue"))).limit(5).toList());
        Map<String,Map<String,Object>> customers=new HashMap<>();for(var o:paid){var row=customers.computeIfAbsent(o.getUserId(),id->{var u=users.findById(id).orElseThrow();return new HashMap<>(Map.of("id",id,"name",u.getFullName(),"full_name",u.getFullName(),"email",u.getEmail(),"total_spent",BigDecimal.ZERO,"total",BigDecimal.ZERO,"order_count",0));});row.put("total_spent",((BigDecimal)row.get("total_spent")).add(o.getTotal()));row.put("total",row.get("total_spent"));row.put("order_count",((Integer)row.get("order_count"))+1);}
        data.put("top_customers",customers.values().stream().sorted((a,b)->((BigDecimal)b.get("total_spent")).compareTo((BigDecimal)a.get("total_spent"))).limit(5).toList());data.put("rating_average",reviews.findAll().stream().mapToInt(r->r.getRating()).average().orElse(0));return ApiResponse.ok(data);
    }
}
