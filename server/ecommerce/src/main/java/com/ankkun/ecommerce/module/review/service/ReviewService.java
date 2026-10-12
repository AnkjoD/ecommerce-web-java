package com.ankkun.ecommerce.module.review.service;
import com.ankkun.ecommerce.common.dto.*;
import com.ankkun.ecommerce.common.exception.*;
import com.ankkun.ecommerce.module.review.entity.Review;
import com.ankkun.ecommerce.module.review.repository.ReviewRepository;
import com.ankkun.ecommerce.module.order.repository.OrderRepository;
import com.ankkun.ecommerce.module.product.repository.ProductRepository;
import com.ankkun.ecommerce.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import java.util.*;
@Service @RequiredArgsConstructor @Transactional
public class ReviewService {
    private final ReviewRepository reviews;private final OrderRepository orders;private final ProductRepository products;private final UserRepository users;
    public Object list(String productId,Integer rating,int page,int limit,boolean admin) {
        page=Math.max(1,page);limit=Math.max(1,Math.min(100,limit));var pageable=PageRequest.of(page-1,limit,Sort.by(Sort.Direction.DESC,"createdAt"));
        var result=admin?reviews.findAll(pageable):rating==null?reviews.findByProductId(productId,pageable):reviews.findByProductIdAndRating(productId,rating,pageable);
        Map<String,Object> r=new LinkedHashMap<>();r.put("data",result.getContent().stream().map(this::response).toList());r.put("total",result.getTotalElements());r.put("page",page);r.put("limit",limit);r.put("total_pages",result.getTotalPages());
        if(!admin){Map<Integer,Long>d=new LinkedHashMap<>();for(int i=1;i<=5;i++)d.put(i,0L);for(var row:reviews.countByRatingForProduct(productId))d.put(((Number)row[0]).intValue(),((Number)row[1]).longValue());long n=d.values().stream().mapToLong(Long::longValue).sum();double avg=n==0?0:d.entrySet().stream().mapToDouble(e->e.getKey()*e.getValue()).sum()/n;r.put("stats",Map.of("avg",avg,"distribution",d));}
        return r;
    }
    public Object create(String userId,String productId,int rating,String comment) {
        users.findLocked(userId).orElseThrow();var delivered=orders.findDeliveredContaining(userId,productId);
        if(delivered.isEmpty())throw new BadRequestException("Chỉ được đánh giá sau khi đã nhận sản phẩm");
        if(reviews.existsByUserIdAndProductId(userId,productId))throw new ConflictException("Bạn đã đánh giá sản phẩm này");
        var r=reviews.saveAndFlush(Review.builder().userId(userId).productId(productId).orderId(delivered.getFirst().getId()).rating(rating).comment(comment).isVerified(true).build());
        recalculate(productId);return response(r);
    }
    public Object update(String id,String userId,Integer rating,String comment,boolean admin,String reply,Boolean verified) {
        var r=reviews.findById(id).orElseThrow(()->new NotFoundException("Review not found"));
        if(!admin && !r.getUserId().equals(userId))throw new NotFoundException("Review not found");
        if(rating!=null)r.setRating(rating);if(comment!=null)r.setComment(comment);
        if(admin){if(reply!=null){r.setAdminReply(reply);r.setAdminReplyAt(java.time.Instant.now());}if(verified!=null)r.setIsVerified(verified);}
        reviews.saveAndFlush(r);recalculate(r.getProductId());return response(r);
    }
    public void delete(String id,String userId,boolean admin) {
        var r=reviews.findById(id).orElseThrow(()->new NotFoundException("Review not found"));if(!admin&&!r.getUserId().equals(userId))throw new NotFoundException("Review not found");
        reviews.delete(r);reviews.flush();recalculate(r.getProductId());
    }
    private void recalculate(String productId) {var p=products.findLocked(productId).orElseThrow();var all=reviews.findByProductId(productId);p.setRatingCount(all.size());p.setRatingAvg(all.stream().mapToInt(Review::getRating).average().orElse(0));}
    public Map<String,Object> response(Review r) {
        var u=users.findById(r.getUserId()).orElseThrow();var p=products.findById(r.getProductId()).orElseThrow();Map<String,Object> row=new LinkedHashMap<>();
        row.put("id",r.getId());row.put("user_id",r.getUserId());row.put("product_id",r.getProductId());row.put("order_id",r.getOrderId());row.put("rating",r.getRating());row.put("comment",r.getComment());row.put("is_verified",r.getIsVerified());row.put("created_at",r.getCreatedAt());row.put("admin_reply",r.getAdminReply());row.put("admin_reply_at",r.getAdminReplyAt());row.put("user",Map.of("id",u.getId(),"full_name",u.getFullName()));row.put("product",Map.of("id",p.getId(),"name",p.getName()));return row;
    }
}
