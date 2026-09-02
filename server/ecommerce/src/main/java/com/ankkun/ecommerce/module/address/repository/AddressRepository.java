package com.ankkun.ecommerce.module.address.repository;

import com.ankkun.ecommerce.module.address.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, String> {
    List<Address> findByUserIdOrderByIsDefaultDesc(String userId);
    Optional<Address> findByIdAndUserId(String id, String userId);
    long countByUserId(String userId);

    @Modifying
    @Query("UPDATE Address a SET a.isDefault = false WHERE a.userId = :userId AND a.isDefault = true")
    int clearDefault(String userId);
}
