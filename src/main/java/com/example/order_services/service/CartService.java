package com.example.order_services.service;

import com.example.order_services.dto.request.AdjustCartItemQuantityRequest;
import com.example.order_services.dto.response.CartDetailResponse;

public interface CartService {
    CartDetailResponse getCartDetail();
    Integer adjustCartItemQuantity(String cartItemId, AdjustCartItemQuantityRequest request);
}
