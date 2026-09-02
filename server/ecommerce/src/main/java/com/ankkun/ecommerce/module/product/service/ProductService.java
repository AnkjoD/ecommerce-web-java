package com.ankkun.ecommerce.module.product.service;

import com.ankkun.ecommerce.common.dto.PaginatedResponse;
import com.ankkun.ecommerce.common.exception.ConflictException;
import com.ankkun.ecommerce.common.exception.NotFoundException;
import com.ankkun.ecommerce.module.category.repository.CategoryRepository;
import com.ankkun.ecommerce.module.category.service.CategoryService;
import com.ankkun.ecommerce.module.product.dto.*;
import com.ankkun.ecommerce.module.product.entity.Product;
import com.ankkun.ecommerce.module.product.entity.ProductVariant;
import com.ankkun.ecommerce.module.product.repository.ProductRepository;
import com.ankkun.ecommerce.module.product.repository.ProductVariantRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final CategoryRepository categoryRepository;
    private final CategoryService categoryService;

    public PaginatedResponse<ProductResponse> findAll(Map<String, String> params) {
        int page = Integer.parseInt(params.getOrDefault("page", "1"));
        int limit = Integer.parseInt(params.getOrDefault("limit", "20"));
        String q = params.get("q");
        String brand = params.get("brand");
        String categorySlug = params.get("category");
        String sort = params.getOrDefault("sort", "created_at");
        String order = params.getOrDefault("order", "desc");
        BigDecimal minPrice = params.containsKey("min_price") ? new BigDecimal(params.get("min_price")) : null;
        BigDecimal maxPrice = params.containsKey("max_price") ? new BigDecimal(params.get("max_price")) : null;
        Double ratingFilter = params.containsKey("rating_filter") ? Double.parseDouble(params.get("rating_filter")) : null;

        // Category filter: collect all descendant IDs
        List<String> categoryIds = null;
        if (categorySlug != null) {
            var cat = categoryRepository.findBySlug(categorySlug).orElse(null);
            if (cat != null) {
                categoryIds = new ArrayList<>();
                categoryIds.add(cat.getId());
                categoryIds.addAll(categoryService.getDescendantIds(cat.getId()));
            } else {
                categoryIds = List.of("non-existent");
            }
        }

        final List<String> finalCategoryIds = categoryIds;
        final BigDecimal fMin = minPrice, fMax = maxPrice;

        Specification<Product> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), "active"));

            if (q != null && !q.isBlank()) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), "%" + q.toLowerCase() + "%"),
                        cb.like(cb.lower(root.get("brand")), "%" + q.toLowerCase() + "%")
                ));
            }
            if (brand != null) predicates.add(cb.like(root.get("brand"), "%" + brand + "%"));
            if (ratingFilter != null) predicates.add(cb.ge(root.get("ratingAvg"), ratingFilter));
            if (finalCategoryIds != null) predicates.add(root.get("categoryId").in(finalCategoryIds));
            if (fMin != null) predicates.add(cb.ge(root.get("minPrice"), fMin));
            if (fMax != null) predicates.add(cb.le(root.get("minPrice"), fMax));

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        String sortField = switch (sort) {
            case "price" -> "minPrice";
            case "rating" -> "ratingAvg";
            case "sold_count" -> "soldCount";
            case "view_count" -> "viewCount";
            default -> "createdAt";
        };
        Sort.Direction dir = "asc".equalsIgnoreCase(order) ? Sort.Direction.ASC : Sort.Direction.DESC;

        Page<Product> result = productRepository.findAll(spec, PageRequest.of(page - 1, limit, Sort.by(dir, sortField)));
        List<ProductResponse> dtos = result.getContent().stream().map(this::toResponse).collect(Collectors.toList());
        return PaginatedResponse.of(dtos, page, limit, result.getTotalElements());
    }

    public ProductResponse findBySlug(String slug) {
        return toResponse(productRepository.findBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại")));
    }

    public Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại"));
    }

    @Transactional
    public ProductResponse create(ProductCreateDto dto) {
        if (productRepository.existsBySlug(dto.getSlug())) throw new ConflictException("Slug đã tồn tại");

        Product product = Product.builder()
                .name(dto.getName())
                .slug(dto.getSlug())
                .brand(dto.getBrand())
                .categoryId(dto.getCategoryId())
                .status(dto.getStatus())
                .description(dto.getDescription())
                .attributes(dto.getAttributes())
                .media(dto.getMedia())
                .seo(dto.getSeo())
                .build();
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(String id, ProductUpdateDto dto) {
        Product product = findById(id);
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getBrand() != null) product.setBrand(dto.getBrand());
        if (dto.getStatus() != null) product.setStatus(dto.getStatus());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getAttributes() != null) product.setAttributes(dto.getAttributes());
        if (dto.getMedia() != null) product.setMedia(dto.getMedia());
        if (dto.getCategoryId() != null) product.setCategoryId(dto.getCategoryId());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(String id) {
        productRepository.deleteById(id);
    }

    // Variant ops
    @Transactional
    public ProductVariantResponse addVariant(String productId, VariantCreateDto dto) {
        findById(productId); // ensure exists
        ProductVariant v = ProductVariant.builder()
                .productId(productId)
                .sku(dto.getSku())
                .color(dto.getColor())
                .size(dto.getSize())
                .options(dto.getOptions())
                .price(dto.getPrice())
                .priceBeforeDiscount(dto.getPriceBeforeDiscount())
                .stockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                .imageUrl(dto.getImageUrl())
                .build();
        v = variantRepository.save(v);
        updatePriceRange(productId);
        return toVariantResponse(v);
    }

    private void updatePriceRange(String productId) {
        List<ProductVariant> variants = variantRepository.findByProductIdOrderByPriceAsc(productId);
        if (variants.isEmpty()) return;
        BigDecimal min = variants.get(0).getPrice();
        BigDecimal max = variants.get(variants.size() - 1).getPrice();
        Product p = findById(productId);
        p.setMinPrice(min);
        p.setMaxPrice(max);
        productRepository.save(p);
    }

    private ProductResponse toResponse(Product p) {
        ProductResponse r = new ProductResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setSlug(p.getSlug());
        r.setBrand(p.getBrand());
        r.setCategoryId(p.getCategoryId());
        r.setStatus(p.getStatus());
        r.setRatingAvg(p.getRatingAvg());
        r.setRatingCount(p.getRatingCount());
        r.setSoldCount(p.getSoldCount());
        r.setMinPrice(p.getMinPrice());
        r.setMaxPrice(p.getMaxPrice());
        r.setDescription(p.getDescription());
        r.setAttributes(p.getAttributes());
        r.setMedia(p.getMedia());
        r.setSeo(p.getSeo());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        if (p.getVariants() != null) {
            r.setVariants(p.getVariants().stream().map(this::toVariantResponse).collect(Collectors.toList()));
        }
        return r;
    }

    private ProductVariantResponse toVariantResponse(ProductVariant v) {
        ProductVariantResponse r = new ProductVariantResponse();
        r.setId(v.getId());
        r.setSku(v.getSku());
        r.setColor(v.getColor());
        r.setSize(v.getSize());
        r.setOptions(v.getOptions());
        r.setPrice(v.getPrice());
        r.setPriceBeforeDiscount(v.getPriceBeforeDiscount());
        r.setStockQuantity(v.getStockQuantity());
        r.setImageUrl(v.getImageUrl());
        return r;
    }
}
