package com.ankkun.ecommerce.module.cart.service;

import com.ankkun.ecommerce.common.exception.BadRequestException;
import com.ankkun.ecommerce.common.exception.NotFoundException;
import com.ankkun.ecommerce.module.cart.dto.CartItemRequest;
import com.ankkun.ecommerce.module.cart.dto.CartItemResponse;
import com.ankkun.ecommerce.module.cart.dto.CartResponse;
import com.ankkun.ecommerce.module.cart.entity.Cart;
import com.ankkun.ecommerce.module.cart.entity.CartItem;
import com.ankkun.ecommerce.module.cart.repository.CartItemRepository;
import com.ankkun.ecommerce.module.cart.repository.CartRepository;
import com.ankkun.ecommerce.module.product.dto.ProductVariantResponse;
import com.ankkun.ecommerce.module.product.entity.ProductVariant;
import com.ankkun.ecommerce.module.product.repository.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository variantRepository;

    public CartResponse getCart(String userId) {
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = Cart.builder().userId(userId).build();
            return cartRepository.save(newCart);
        });
        return toResponse(cart);
    }

    @Transactional
    public CartResponse addItem(String userId, CartItemRequest dto) {
        Cart cart = cartRepository.findByUserId(userId).orElseGet(() -> {
            Cart newCart = Cart.builder().userId(userId).build();
            return cartRepository.save(newCart);
        });

        ProductVariant variant = variantRepository.findById(dto.getVariantId())
                .orElseThrow(() -> new NotFoundException("Variant not found"));

        if (variant.getStockQuantity() < dto.getQuantity()) {
            throw new BadRequestException("Hết hàng");
        }

        Optional<CartItem> existing = cartItemRepository.findByCartIdAndVariantId(cart.getId(), dto.getVariantId());
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + dto.getQuantity());
            cartItemRepository.save(item);
        } else {
            CartItem item = CartItem.builder()
                    .cartId(cart.getId())
                    .variantId(dto.getVariantId())
                    .quantity(dto.getQuantity())
                    .build();
            cartItemRepository.save(item);
        }

        return getCart(userId);
    }

    @Transactional
    public CartResponse updateItem(String userId, String variantId, int quantity) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow(() -> new NotFoundException("Cart not found"));
        CartItem item = cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId)
                .orElseThrow(() -> new NotFoundException("Item not in cart"));
        
        ProductVariant variant = variantRepository.findById(variantId).orElseThrow();
        if (variant.getStockQuantity() < quantity) {
            throw new BadRequestException("Không đủ hàng trong kho");
        }

        if (quantity <= 0) {
            cartItemRepository.delete(item);
        } else {
            item.setQuantity(quantity);
            cartItemRepository.save(item);
        }

        return getCart(userId);
    }

    @Transactional
    public void removeItem(String userId, String variantId) {
        Cart cart = cartRepository.findByUserId(userId).orElseThrow();
        cartItemRepository.findByCartIdAndVariantId(cart.getId(), variantId)
                .ifPresent(cartItemRepository::delete);
    }

    private CartResponse toResponse(Cart cart) {
        CartResponse r = new CartResponse();
        r.setId(cart.getId());
        r.setUserId(cart.getUserId());
        if (cart.getItems() != null) {
            r.setItems(cart.getItems().stream().map(this::toItemResponse).collect(Collectors.toList()));
        }
        return r;
    }

    private CartItemResponse toItemResponse(CartItem item) {
        CartItemResponse r = new CartItemResponse();
        r.setId(item.getId());
        r.setVariantId(item.getVariantId());
        r.setQuantity(item.getQuantity());

        ProductVariant v = item.getVariant();
        if (v != null) {
            ProductVariantResponse vr = new ProductVariantResponse();
            vr.setId(v.getId());
            vr.setSku(v.getSku());
            vr.setColor(v.getColor());
            vr.setSize(v.getSize());
            vr.setOptions(v.getOptions());
            vr.setPrice(v.getPrice());
            vr.setPriceBeforeDiscount(v.getPriceBeforeDiscount());
            vr.setStockQuantity(v.getStockQuantity());
            vr.setImageUrl(v.getImageUrl());
            r.setVariant(vr);
        }
        return r;
    }
}
