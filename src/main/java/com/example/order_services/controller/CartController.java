package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.response.CartDetailResponse;
import com.example.order_services.service.CartService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/carts")
public class CartController {
    private final CartService cartService;

    @GetMapping("/{userId}")
    public BaseResponse<CartDetailResponse> getCart(@PathVariable String userId) {
        return BaseResponse.success(cartService.getCart(userId));
    }
}
