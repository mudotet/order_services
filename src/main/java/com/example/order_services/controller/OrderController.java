package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.request.OrderSummaryRequest;
import com.example.order_services.dto.response.OrderResponse;
import com.example.order_services.dto.response.OrderSummaryResponse;
import com.example.order_services.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/summary")
    public BaseResponse<OrderSummaryResponse> calculateOrderSummary(@Valid @RequestBody OrderSummaryRequest request) {
        return BaseResponse.success(orderService.calculateOrderSummary(request.getDiscountId()));
    }

    @PostMapping
    public BaseResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return BaseResponse.success(orderService.createOrder(request));
    }
}
