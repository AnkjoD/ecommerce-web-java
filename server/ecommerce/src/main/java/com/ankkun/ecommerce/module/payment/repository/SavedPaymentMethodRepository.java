package com.ankkun.ecommerce.module.payment.repository;
import com.ankkun.ecommerce.module.payment.entity.SavedPaymentMethod;
public interface SavedPaymentMethodRepository extends org.springframework.data.jpa.repository.JpaRepository<SavedPaymentMethod,String>{java.util.List<SavedPaymentMethod> findByUserIdOrderByCreatedAtDesc(String userId);}
