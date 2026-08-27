package com.example.order_services.service.impl;

import com.example.order_services.common.EnumCode;
import com.example.order_services.dto.response.CartDetailResponse;
import com.example.order_services.dto.response.CartItemResponse;
import com.example.order_services.entity.Cart;
import com.example.order_services.entity.CartItem;
import com.example.order_services.entity.ProductVariant;
import com.example.order_services.exception.ApplicationException;
import com.example.order_services.repository.CartItemRepository;
import com.example.order_services.repository.CartRepository;
import com.example.order_services.repository.ProductVariantRepository;
import com.example.order_services.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductVariantRepository productVariantRepository;

    @Override
    public CartDetailResponse getCart(String userId) {
        return cartRepository.findByUserId(userId)
                .map(this::toCartDetail)
                .orElseGet(() -> new CartDetailResponse(
                        userId,
                        List.of(),
                        new BigDecimal("0.00")
                ));
    }

    private CartDetailResponse toCartDetail(Cart cart) {
        List<CartItemResponse> items = cartItemRepository
                .findAllByCartId(cart.getId())
                .stream()
                .map(this::toCartItemResponse)
                .toList();
        BigDecimal subtotal = items.stream()
                .map(CartItemResponse::getLineTotal)
                .reduce(new BigDecimal("0.00"), BigDecimal::add);
        return new CartDetailResponse(cart.getUserId(), items, subtotal);
    }

    private CartItemResponse toCartItemResponse(CartItem item) {
        ProductVariant variant = productVariantRepository
                .findById(item.getProductVariantId())
                .orElseThrow(() -> new ApplicationException(
                        EnumCode.NOT_FOUND,
                        "Product variant not found"
                ));
        BigDecimal lineTotal = variant.getPrice()
                .multiply(BigDecimal.valueOf(item.getProductQuantity()));
        return new CartItemResponse(
                variant.getId(),
                variant.getProductVariant(),
                variant.getPrice(),
                item.getProductQuantity(),
                lineTotal
        );
    }
}
