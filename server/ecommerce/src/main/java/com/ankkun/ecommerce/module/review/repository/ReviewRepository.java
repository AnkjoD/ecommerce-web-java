package com.ankkun.ecommerce.module.review.repository;

import com.ankkun.ecommerce.module.review.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<Review, String> {
    Page<Review> findByProductId(String productId, Pageable pageable);
    Page<Review> findByProductIdAndRating(String productId, Integer rating, Pageable pageable);
    boolean existsByUserIdAndProductId(String userId, String productId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.productId = :productId GROUP BY r.rating")
    List<Object[]> countByRatingForProduct(String productId);
}
