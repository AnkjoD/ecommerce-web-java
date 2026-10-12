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
import com.ankkun.ecommerce.common.dto.JsonData;
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

    public PaginatedResponse<ProductResponse> findAll(Map<String, String> params) { return findAll(params, false); }

    public PaginatedResponse<ProductResponse> findAll(Map<String, String> params, boolean admin) {
        int page = Math.max(1, Integer.parseInt(params.getOrDefault("page", "1")));
        int limit = Math.max(1, Math.min(100, Integer.parseInt(params.getOrDefault("limit", "20"))));
        String q = params.getOrDefault("q", params.get("name"));
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
            if (!admin){predicates.add(cb.equal(root.get("status"), "active"));var v=root.join("variants");predicates.add(cb.isTrue(v.get("isActive")));query.distinct(true);}

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

    public ProductVariantResponse findPublicVariant(String id) {
        return toVariantResponse(variantRepository.findById(id)
                .filter(v -> v.getIsActive() && "active".equals(v.getProduct().getStatus()))
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại")));
    }

    public List<ProductVariantResponse> publicVariants(String productId) {
        if (!"active".equals(findById(productId).getStatus())) throw new NotFoundException("Sản phẩm không tồn tại");
        return variantRepository.findByProductIdOrderByPriceAsc(productId).stream()
                .filter(ProductVariant::getIsActive).map(this::toVariantResponse).toList();
    }

    public Product findById(String id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại"));
    }

    @Transactional
    public ProductResponse create(ProductCreateDto dto) {
        if (dto.getSlug() == null || dto.getSlug().isBlank()) dto.setSlug("product-" + java.util.UUID.randomUUID());
        if (productRepository.existsBySlug(dto.getSlug())) throw new ConflictException("Slug đã tồn tại");

        Product product = Product.builder()
                .name(dto.getName())
                .slug(dto.getSlug())
                .brand(dto.getBrand())
                .categoryId(dto.getCategoryId() == null || dto.getCategoryId().isBlank() ? null : dto.getCategoryId())
                .status(dto.getStatus())
                .description(JsonData.store(dto.getDescription()))
                .attributes(JsonData.store(dto.getAttributes()))
                .media(JsonData.store(dto.getMedia()))
                .seo(JsonData.store(dto.getSeo()))
                .build();
        product = productRepository.saveAndFlush(product);
        if (dto.getVariants() != null) for (var variant : dto.getVariants()) addVariant(product.getId(), variant);
        applyMetadata(product,dto.getTags(),dto.getAttributeOrder(),dto.getPrimaryVariantId());
        return toResponse(product);
    }

    @Transactional
    public ProductResponse update(String id, ProductUpdateDto dto) {
        Product product = productRepository.findLocked(id).orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại"));
        if (dto.getVariants()!=null) for(var input:dto.getVariants()){
            if(input.getId()==null||input.getId().isBlank()){addVariant(id,input);continue;}
            var v=variantRepository.findLocked(input.getId()).filter(x->x.getProductId().equals(id)&&x.getIsActive()).orElseThrow(()->new NotFoundException("Biến thể không tồn tại"));
            if(input.getStockQuantity()<v.getReservedQuantity())throw new com.ankkun.ecommerce.common.exception.BadRequestException("Tồn kho nhỏ hơn số lượng đang giữ");
            v.setSku(input.getSku());v.setPrice(input.getPrice());v.setPriceBeforeDiscount(input.getPriceBeforeDiscount());v.setStockQuantity(input.getStockQuantity());v.setWeight(input.getWeight());v.setImageUrl(input.getImageUrl());v.setOptions(JsonData.store(input.getOptions()));v.setColor(input.getColor());v.setSize(input.getSize());
        }
        variantRepository.flush();updatePriceRange(id);
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getBrand() != null) product.setBrand(dto.getBrand());
        if (dto.getStatus() != null) product.setStatus(dto.getStatus());
        if (dto.getDescription() != null) product.setDescription(JsonData.store(dto.getDescription()));
        if (dto.getAttributes() != null) product.setAttributes(JsonData.store(dto.getAttributes()));
        if (dto.getMedia() != null) product.setMedia(JsonData.store(dto.getMedia()));
        if (dto.getCategoryId() != null) product.setCategoryId(dto.getCategoryId().isBlank()?null:dto.getCategoryId());
        if(dto.getSeo()!=null)product.setSeo(JsonData.store(dto.getSeo()));
        applyMetadata(product,dto.getTags(),dto.getAttributeOrder(),dto.getPrimaryVariantId());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(String id) {
        Product product = findById(id); product.setStatus("archived"); productRepository.save(product);
    }

    // Variant ops
    @Transactional
    public ProductVariantResponse addVariant(String productId, VariantCreateDto dto) {
        Product product = findById(productId);
        product.getVariants().size();
        ProductVariant v = ProductVariant.builder()
                .productId(productId)
                .product(product)
                .weight(dto.getWeight())
                .sku(dto.getSku())
                .color(dto.getColor())
                .size(dto.getSize())
                .options(JsonData.store(dto.getOptions()))
                .price(dto.getPrice())
                .priceBeforeDiscount(dto.getPriceBeforeDiscount())
                .stockQuantity(dto.getStockQuantity() != null ? dto.getStockQuantity() : 0)
                .imageUrl(dto.getImageUrl())
                .build();
        v = variantRepository.saveAndFlush(v);
        product.getVariants().add(v);
        updatePriceRange(productId);
        return toVariantResponse(v);
    }

    private void updatePriceRange(String productId) {
        List<ProductVariant> variants = variantRepository.findByProductIdOrderByPriceAsc(productId).stream().filter(ProductVariant::getIsActive).toList();
        if (variants.isEmpty()) {var p=findById(productId);p.setMinPrice(BigDecimal.ZERO);p.setMaxPrice(BigDecimal.ZERO);p.setPrimaryVariantId(null);return;}
        BigDecimal min = variants.get(0).getPrice();
        BigDecimal max = variants.get(variants.size() - 1).getPrice();
        Product p = findById(productId);
        p.setMinPrice(min);
        p.setMaxPrice(max);
        productRepository.save(p);
    }

    public ProductResponse toResponse(Product p) {
        return toResponse(p, true);
    }

    private ProductResponse toResponse(Product p, boolean includeVariants) {
        ProductResponse r = new ProductResponse();
        r.setId(p.getId());
        r.setName(p.getName());
        r.setSlug(p.getSlug());
        r.setBrand(p.getBrand());
        r.setCategoryId(p.getCategoryId());
        if (p.getCategory() != null) r.setCategory(categoryService.findBySlug(p.getCategory().getSlug()));
        r.setStatus(p.getStatus());
        r.setRatingAvg(p.getRatingAvg());
        r.setRatingCount(p.getRatingCount());
        r.setSoldCount(p.getSoldCount());
        r.setMinPrice(p.getMinPrice());
        r.setMaxPrice(p.getMaxPrice());
        r.setDescription(JsonData.parse(p.getDescription()));
        r.setAttributes(JsonData.parse(p.getAttributes()));
        r.setMedia(JsonData.parse(p.getMedia()));
        r.setSeo(JsonData.parse(p.getSeo()));
        r.setTags(p.getTags() == null ? new String[0] : p.getTags());
        r.setViewCount(p.getViewCount());
        r.setAttributeOrder(p.getAttributeOrder());
        r.setPrimaryVariantId(p.getPrimaryVariantId());
        r.setCreatedAt(p.getCreatedAt());
        r.setUpdatedAt(p.getUpdatedAt());
        if (includeVariants && p.getVariants() != null) {
            r.setVariants(p.getVariants().stream().filter(ProductVariant::getIsActive).map(this::toVariantResponse).collect(Collectors.toList()));
        }
        return r;
    }

    public ProductVariantResponse toVariantResponse(ProductVariant v) {
        ProductVariantResponse r = new ProductVariantResponse();
        r.setId(v.getId());
        r.setSku(v.getSku());
        r.setColor(v.getColor());
        r.setSize(v.getSize());
        r.setOptions(JsonData.parse(v.getOptions()));
        r.setPrice(v.getPrice());
        r.setPriceBeforeDiscount(v.getPriceBeforeDiscount());
        r.setStockQuantity(v.getStockQuantity());
        r.setImageUrl(v.getImageUrl());
        r.setReservedQuantity(v.getReservedQuantity());
        r.setSoldCount(v.getSoldCount());
        r.setViewCount(v.getViewCount());
        r.setProduct(toResponse(v.getProduct() != null ? v.getProduct() : findById(v.getProductId()), false));
        return r;
    }

    private void applyMetadata(Product p,com.fasterxml.jackson.databind.JsonNode tags,String[] order,String primary) {
        if(tags!=null){if(tags.isTextual())p.setTags(java.util.Arrays.stream(tags.asText().split(",")).map(String::trim).filter(t->!t.isBlank()).distinct().toArray(String[]::new));else if(tags.isArray()){var list=new ArrayList<String>();tags.forEach(t->list.add(t.asText()));p.setTags(list.toArray(String[]::new));}else throw new com.ankkun.ecommerce.common.exception.BadRequestException("Tags không hợp lệ");}
        if(order!=null)p.setAttributeOrder(order);
        if(primary!=null&&!primary.isBlank()){var v=variantRepository.findById(primary).orElseThrow(()->new NotFoundException("Variant not found"));if(!v.getProductId().equals(p.getId())||!v.getIsActive())throw new com.ankkun.ecommerce.common.exception.BadRequestException("Biến thể chính không hợp lệ");p.setPrimaryVariantId(primary);}
    }
    @Transactional public void deleteVariant(String id){var v=variantRepository.findById(id).orElseThrow(()->new NotFoundException("Variant not found"));v.setIsActive(false);variantRepository.saveAndFlush(v);updatePriceRange(v.getProductId());}
    public List<String> uniqueTags(){return productRepository.findAll().stream().filter(p->"active".equals(p.getStatus())).flatMap(p->java.util.Arrays.stream(p.getTags()==null?new String[0]:p.getTags())).distinct().sorted().toList();}
    public Object sync(){return Map.of("message","Danh mục tìm kiếm local đã cập nhật","products",productRepository.count(),"mode","local");}
}
