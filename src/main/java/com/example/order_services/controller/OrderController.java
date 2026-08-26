package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.request.OrderSummaryRequest;
import com.example.order_services.dto.response.OrderResponse;
import com.example.order_services.dto.response.OrderSummaryResponse;
import com.example.order_services.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    @PostMapping("/summary")
    public BaseResponse<OrderSummaryResponse> calculateSummary(
            @Valid @RequestBody OrderSummaryRequest request
    ) {
        return BaseResponse.success(orderService.calculateSummary(request));
    }

    @PostMapping
    public BaseResponse<OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest request
    ) {
        return BaseResponse.success(orderService.createOrder(request));
    }
}
