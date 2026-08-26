package com.example.order_services.service;

import com.example.order_services.dto.response.CartDetailResponse;

public interface CartService {
    CartDetailResponse getCart(String userId);
}
