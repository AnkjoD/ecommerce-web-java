package com.ankkun.ecommerce.module.cart.service;
import com.ankkun.ecommerce.common.exception.*;
import com.ankkun.ecommerce.module.cart.dto.*;
import com.ankkun.ecommerce.module.cart.entity.*;
import com.ankkun.ecommerce.module.cart.repository.*;
import com.ankkun.ecommerce.module.product.repository.ProductVariantRepository;
import com.ankkun.ecommerce.module.product.service.ProductService;
import com.ankkun.ecommerce.module.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.ArrayList;
@Service
@RequiredArgsConstructor
@Transactional
public class CartService {
    private final CartRepository carts;
    private final CartItemRepository items;
    private final ProductVariantRepository variants;
    private final ProductService products;
    private final UserRepository users;
    private Cart cart(String userId) {
        users.findLocked(userId).orElseThrow(() -> new UnauthorizedException("User not found"));
        return carts.findByUserId(userId).orElseGet(() -> carts.save(Cart.builder().userId(userId).build()));
    }
    public CartResponse getCart(String userId) { return response(cart(userId)); }
    public CartResponse addItem(String userId, CartItemRequest req) {
        Cart cart = cart(userId);
        var variant = variants.findById(req.getVariantId()).orElseThrow(() -> new NotFoundException("Variant not found"));
        if (!variant.getIsActive() || !"active".equals(variant.getProduct().getStatus())) throw new BadRequestException("Sản phẩm không còn bán");
        var item = items.findByCartIdAndVariantId(cart.getId(), variant.getId()).orElse(null);
        int quantity = Math.addExact(item == null ? 0 : item.getQuantity(), req.getQuantity());
        if (quantity > variant.getStockQuantity() - variant.getReservedQuantity()) throw new BadRequestException("Không đủ hàng trong kho");
        if (item == null) {
            item = CartItem.builder().cartId(cart.getId()).variantId(variant.getId()).variant(variant).quantity(quantity).build();
            cart.getItems().add(item);
        } else item.setQuantity(quantity);
        items.saveAndFlush(item);
        return response(cart);
    }
    public CartResponse updateItem(String userId, String variantId, int quantity) {
        Cart cart = cart(userId);
        var item = items.findByCartIdAndVariantId(cart.getId(), variantId).orElseThrow(() -> new NotFoundException("Item not found"));
        if (quantity < 0) throw new BadRequestException("Số lượng không hợp lệ");
        if (quantity == 0) { cart.getItems().remove(item); items.delete(item); }
        else {
            if (quantity > item.getVariant().getStockQuantity() - item.getVariant().getReservedQuantity()) throw new BadRequestException("Không đủ hàng trong kho");
            item.setQuantity(quantity);
        }
        return response(cart);
    }
    public void removeItem(String userId, String variantId) {
        Cart cart = cart(userId);
        items.findByCartIdAndVariantId(cart.getId(), variantId).ifPresent(item -> { cart.getItems().remove(item); items.delete(item); });
    }
    private CartResponse response(Cart cart) {
        CartResponse r = new CartResponse(); r.setId(cart.getId()); r.setUserId(cart.getUserId());
        var responseItems = new ArrayList<CartItemResponse>();
        BigDecimal subtotal = BigDecimal.ZERO; int count = 0;
        for (CartItem item : cart.getItems()) {
            CartItemResponse row = new CartItemResponse(); row.setId(item.getId()); row.setVariantId(item.getVariantId()); row.setQuantity(item.getQuantity());
            row.setVariant(products.toVariantResponse(item.getVariant())); responseItems.add(row);
            subtotal = subtotal.add(item.getVariant().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()))); count += item.getQuantity();
        }
        r.setItems(responseItems); r.setSubtotal(subtotal); r.setTotalItems(count); return r;
    }
}
