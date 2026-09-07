package com.example.order_services.service;

import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.response.OrderResponse;
import com.example.order_services.dto.response.OrderReturnResponse;
import com.example.order_services.dto.response.OrderReturnsSummaryResponse;
import com.example.order_services.dto.response.OrderSummaryResponse;
import org.springframework.data.domain.Page;

public interface OrderService {
    OrderSummaryResponse calculateOrderSummary(String discountId);
    OrderResponse createOrder(CreateOrderRequest request);

    OrderReturnsSummaryResponse calculateOrderReturnSummary();

    Page<OrderReturnResponse> getOrderReturns(int page, int size);
}
