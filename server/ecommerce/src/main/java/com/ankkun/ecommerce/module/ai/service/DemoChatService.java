package com.ankkun.ecommerce.module.ai.service;
import com.ankkun.ecommerce.module.product.repository.ProductRepository;
import com.ankkun.ecommerce.module.product.repository.ProductVariantRepository;
import com.ankkun.ecommerce.module.coupon.service.CouponService;
import com.ankkun.ecommerce.module.order.service.OrderService;
import com.ankkun.ecommerce.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.text.Normalizer;
@Service @RequiredArgsConstructor
public class DemoChatService {
    private final ProductRepository products;private final ProductVariantRepository variants;private final CouponService coupons;private final OrderService orders;
    private String normalize(String s){return Normalizer.normalize(s.toLowerCase(),Normalizer.Form.NFD).replaceAll("\\p{M}","").replace('đ','d');}
    @Transactional(readOnly=true)public Object reply(String userId,String message){
        if(message==null||message.isBlank()||message.length()>500)throw new BadRequestException("Tin nhắn phải từ 1 đến 500 ký tự");String q=normalize(message);String text;List<Map<String,Object>> rows=new ArrayList<>();
        if(q.contains("don hang")||q.contains("don mua")||q.contains("order")||q.contains("giao chua")){
            var history=orders.history(userId,"all",1,5).getData();text=history.isEmpty()?"Bạn chưa có đơn hàng.":"Các đơn của bạn:\n"+history.stream().map(o->o.getOrderCode()+" — "+o.getStatus()+" — "+o.getTotal()+"đ").collect(java.util.stream.Collectors.joining("\n"));
        }else if(q.contains("voucher")||q.contains("giam gia")||q.contains("coupon")){var list=coupons.active();text=list.isEmpty()?"Hiện chưa có mã giảm giá hoạt động.":"Mã hiện có: "+list.stream().map(c->c.getCode()+" ("+c.getDiscountValue()+(c.getDiscountType().equals("percent")?"%":"đ")+")").collect(java.util.stream.Collectors.joining(", "));}
        else if(Set.of("xin chao","hello","hi","chao").contains(q.trim()))text="Chào bạn! Mình là trợ lý demo local. Có thể tìm sản phẩm, mã giảm giá và tra đơn của chính bạn. Mình không gọi AI hay dịch vụ bên ngoài.";
        else{
            String[] tokens=q.replace("giay","sneakers").replace("balo","backpack").replace("ao","hoodie").split("\\s+");
            var found=products.findAll().stream().filter(p->"active".equals(p.getStatus())).sorted((a,b)->Integer.compare(score(normalize(b.getName()+" "+b.getBrand()),tokens),score(normalize(a.getName()+" "+a.getBrand()),tokens))).toList();
            boolean generic=q.contains("san pham")||q.contains("tu van")||q.contains("tim")&&Arrays.stream(tokens).noneMatch(t->t.length()>4);for(var p:found){if(rows.size()==5)break;if(!generic&&score(normalize(p.getName()+" "+p.getBrand()),tokens)==0)continue;var active=variants.findByProductIdOrderByPriceAsc(p.getId()).stream().filter(v->v.getIsActive()&&v.getStockQuantity()>v.getReservedQuantity()).findFirst();if(active.isEmpty())continue;var v=active.get();Map<String,Object> row=new LinkedHashMap<>();row.put("id",v.getId());row.put("product_id",p.getId());row.put("name",p.getName());row.put("price",v.getPrice());row.put("image_url",v.getImageUrl());row.put("url","/"+p.getSlug()+"-i."+v.getId());rows.add(row);}
            text=rows.isEmpty()?"Chưa tìm thấy sản phẩm còn hàng phù hợp. Bạn có thể thử tên như sneakers, hoodie, backpack hoặc hỏi mã giảm giá.":"Từ danh mục hiện tại, mình tìm được: "+rows.stream().map(p->p.get("name")+" — "+p.get("price")+"đ").collect(java.util.stream.Collectors.joining("; "))+". Bạn có thể mở thẻ sản phẩm để xem chi tiết.";
        }
        return Map.of("response",text,"products",rows,"mode","local_catalog");
    }
    private int score(String name,String[] tokens){return (int)Arrays.stream(tokens).filter(t->t.length()>2&&name.contains(t)).count();}
}
