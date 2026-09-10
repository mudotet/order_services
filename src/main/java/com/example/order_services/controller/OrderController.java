package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.request.OrderSummaryRequest;
import com.example.order_services.dto.response.*;
import com.example.order_services.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    // api calculate summary before make order
    @PostMapping("/orders/summary")
    public BaseResponse<OrderSummaryResponse> calculateOrderSummary(@Valid @RequestBody OrderSummaryRequest request) {
        return BaseResponse.success(orderService.calculateOrderSummary(request.getDiscountId()));
    }

    // api create order
    @PostMapping
    public BaseResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return BaseResponse.success(orderService.createOrder(request));
    }

    // api get summary of return orders with total active return, await, refunds
    @GetMapping("/returns/summary")
    public BaseResponse<OrderReturnsSummaryResponse> calculateOrderReturnSummary() {
        return BaseResponse.success(orderService.calculateOrderReturnSummary());
    }

    @GetMapping("/returns")
    public BaseResponse<Page<OrderReturnResponse>> getOrderReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam String filterBy
    ){
        return BaseResponse.success(orderService.getOrderReturns(page, size, filterBy));
    }

    @GetMapping("/returns/{id}")
    public BaseResponse<ViewOrderDetailResponse> viewOrderReturnDetail(@PathVariable String id){
        return BaseResponse.success(orderService.viewOrderReturnDetail(id));
    }

    @GetMapping("/returns/export-csv")
    public BaseResponse<String> exportOrderReturnsToCsv() {
        return BaseResponse.success(orderService.exportOrderReturnsToCsv());
    }
}
