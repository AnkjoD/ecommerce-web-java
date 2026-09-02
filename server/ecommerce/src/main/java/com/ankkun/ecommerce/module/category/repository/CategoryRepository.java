package com.ankkun.ecommerce.module.category.repository;

import com.ankkun.ecommerce.module.category.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, String> {
    Optional<Category> findBySlug(String slug);
    boolean existsBySlug(String slug);
    List<Category> findByParentId(String parentId);
    List<Category> findByParentIdIsNullOrderBySortOrderAsc();
}
