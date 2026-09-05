package com.example.order_services.service;

import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.response.OrderResponse;
import com.example.order_services.dto.response.OrderSummaryResponse;

public interface OrderService {
    OrderSummaryResponse calculateOrderSummary(String discountId);
    OrderResponse createOrder(CreateOrderRequest request);
}
