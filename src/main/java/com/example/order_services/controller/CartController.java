package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.AdjustCartItemQuantityRequest;
import com.example.order_services.dto.response.CartDetailResponse;
import com.example.order_services.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
@PreAuthorize("hasRole('USER')")
public class CartController {
    private final CartService cartService;

    @PostMapping("/items")
    public BaseResponse<CartDetailResponse> addItem(
            @Valid @RequestBody com.example.order_services.dto.request.AddCartItemRequest request) {
        return BaseResponse.success(cartService.addItem(request));
    }

    // api getting cartDetail
    @GetMapping
    public BaseResponse<CartDetailResponse> getCartDetail() {
        return BaseResponse.success(cartService.getCartDetail());
    }

    // api update quantity item in cart
    @PatchMapping("/items/{cartItemId}/quantity")
    public BaseResponse<Integer> adjustCartItemQuantity(@PathVariable String cartItemId,
                                                      @Valid @RequestBody AdjustCartItemQuantityRequest request) {
        return BaseResponse.success(cartService.adjustCartItemQuantity(cartItemId, request));
    }
}
