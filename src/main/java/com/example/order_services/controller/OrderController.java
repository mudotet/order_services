package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.request.OrderSummaryRequest;
import com.example.order_services.dto.request.UpdateOrderStateRequest;
import com.example.order_services.dto.request.AssignShipperRequest;
import com.example.order_services.dto.response.*;
import com.example.order_services.service.OrderService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;

    // api calculate summary before make order
    @PostMapping("/orders/summary")
    @PreAuthorize("hasRole('USER')")
    public BaseResponse<OrderSummaryResponse> calculateOrderSummary(@Valid @RequestBody OrderSummaryRequest request) {
        return BaseResponse.success(orderService.calculateOrderSummary(request.getDiscountId()));
    }

    // api create order
    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public BaseResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return BaseResponse.success(orderService.createOrder(request));
    }

    // api get summary of return orders with total active return, await, refunds
    @GetMapping("/returns/summary!")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<OrderReturnsSummaryResponse> calculateOrderReturnSummary() {
        return BaseResponse.success(orderService.calculateOrderReturnSummary());
    }

    // API to retrieve the list of order returns.
    @GetMapping("/returns")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Page<OrderReturnResponse>> getOrderReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(defaultValue = "all requests") String filterBy
    ) {
        return BaseResponse.success(orderService.getOrderReturns(page, size, filterBy));
    }

    // API to retrieve order return details by returnId.
    @GetMapping("/returns/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<ViewOrderDetailResponse> viewOrderReturnDetail(@PathVariable String id) {
        return BaseResponse.success(orderService.viewOrderReturnDetail(id));
    }

    @GetMapping("/return/export")
    @PreAuthorize("hasRole('ADMIN')")
    public void exportOrderReturns(@RequestParam(defaultValue = "ALL_REQUESTS") String filterBy,
                                   HttpServletResponse response) throws IOException {
        Path file = orderService.exportOrderReturns(filterBy);
        try {
            response.setContentType("text/csv;charset=UTF-8");
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=order-returns.csv");
            response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
            response.setContentLengthLong(Files.size(file));
            Files.copy(file, response.getOutputStream());
        } finally {
            Files.deleteIfExists(file);
        }
    }

    // get tracking order information
    @GetMapping("/tracking/{id}")
    @PreAuthorize("hasRole('USER')")
    public BaseResponse<TrackingOrderDetailResponse> getTrackingOrderInfo(@PathVariable String id) {
        return BaseResponse.success(orderService.getTrackingOrderInfo(id));
    }

    @PatchMapping("/{id}/shipper")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<OrderDeliveryResponse> assignShipper(@PathVariable String id,
                                                            @Valid @RequestBody AssignShipperRequest request) {
        return BaseResponse.success(orderService.assignShipper(id, request));
    }

    // Admins manage fulfillment; assigned shippers can record delivery outcomes.
    @PatchMapping("/tracking/{id}/state")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<OrderDeliveryResponse> updateOrderState(@PathVariable String id,
                                             @Valid @RequestBody UpdateOrderStateRequest request) {
        return BaseResponse.success(orderService.updateOrderState(id, request));
    }
}
