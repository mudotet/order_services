package com.example.order_services.service;

import com.example.order_services.dto.request.AdjustCartItemQuantityRequest;
import com.example.order_services.dto.response.CartDetailResponse;

/** Các thao tác xem và cập nhật giỏ hàng của người dùng. */
public interface CartService {
    CartDetailResponse getCartDetail();
    Integer adjustCartItemQuantity(String cartItemId, AdjustCartItemQuantityRequest request);
}
