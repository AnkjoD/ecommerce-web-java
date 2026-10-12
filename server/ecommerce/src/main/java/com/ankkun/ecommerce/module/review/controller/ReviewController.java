package com.ankkun.ecommerce.module.review.controller;
import com.ankkun.ecommerce.common.dto.ApiResponse;
import com.ankkun.ecommerce.module.review.service.ReviewService;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
@RestController @RequiredArgsConstructor
public class ReviewController {
    private final ReviewService service;
    @Data public static class Input {@JsonProperty("product_id") private String productId;@Min(1)@Max(5) private Integer rating;@Size(max=4000)private String comment;@JsonProperty("admin_reply")@Size(max=4000)private String reply;@JsonProperty("is_verified")private Boolean verified;}
    @GetMapping("/api/reviews/product/{id}") public Object feed(@PathVariable String id,@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int limit,@RequestParam(required=false)Integer rating){return ApiResponse.ok(service.list(id,rating,page,limit,false));}
    @PostMapping("/api/reviews") public Object create(@AuthenticationPrincipal String userId,@Valid @RequestBody Input req){if(req.productId==null||req.productId.isBlank()||req.rating==null)throw new com.ankkun.ecommerce.common.exception.BadRequestException("Thiếu sản phẩm hoặc số sao");return ApiResponse.ok(service.create(userId,req.productId,req.rating,req.comment));}
    @PatchMapping("/api/reviews/{id}")public Object update(@PathVariable String id,@AuthenticationPrincipal String userId,@Valid @RequestBody Input x){return ApiResponse.ok(service.update(id,userId,x.rating,x.comment,false,null,null));}
    @DeleteMapping("/api/reviews/{id}")public Object delete(@PathVariable String id,@AuthenticationPrincipal String userId){service.delete(id,userId,false);return ApiResponse.ok(null);}
    @PreAuthorize("hasRole('ADMIN')")@GetMapping("/api/admin/reviews")public Object admin(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int limit){return ApiResponse.ok(service.list(null,null,page,limit,true));}
    @PreAuthorize("hasRole('ADMIN')")@PatchMapping("/api/admin/reviews/{id}")public Object moderate(@PathVariable String id,@AuthenticationPrincipal String userId,@Valid @RequestBody Input x){return ApiResponse.ok(service.update(id,userId,x.rating,x.comment,true,x.reply,x.verified));}
    @PreAuthorize("hasRole('ADMIN')")@DeleteMapping("/api/admin/reviews/{id}")public Object remove(@PathVariable String id,@AuthenticationPrincipal String userId){service.delete(id,userId,true);return ApiResponse.ok(null);}
}
