package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.request.OrderSummaryRequest;
import com.example.order_services.dto.request.UpdateOrderStateRequest;
import com.example.order_services.dto.response.*;
import com.example.order_services.service.OrderService;
import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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
    public BaseResponse<OrderSummaryResponse> calculateOrderSummary(@Valid @RequestBody OrderSummaryRequest request) {
        return BaseResponse.success(orderService.calculateOrderSummary(request.getDiscountId()));
    }

    // api create order
    @PostMapping
    public BaseResponse<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        return BaseResponse.success(orderService.createOrder(request));
    }

    // api get summary of return orders with total active return, await, refunds
    @GetMapping("/returns/summary!")
    public BaseResponse<OrderReturnsSummaryResponse> calculateOrderReturnSummary() {
        return BaseResponse.success(orderService.calculateOrderReturnSummary());
    }

    // API lấy danh sách đơn trả hàng.
    @GetMapping("/returns")
    public BaseResponse<Page<OrderReturnResponse>> getOrderReturns(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "4") int size,
            @RequestParam(defaultValue = "all requests") String filterBy
    ) {
        return BaseResponse.success(orderService.getOrderReturns(page, size, filterBy));
    }

    // API lấy chi tiết đơn trả hàng theo returnId.
    @GetMapping("/returns/{id}")
    public BaseResponse<ViewOrderDetailResponse> viewOrderReturnDetail(@PathVariable String id) {
        return BaseResponse.success(orderService.viewOrderReturnDetail(id));
    }

    @GetMapping("/return/export")
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
    public BaseResponse<TrackingOrderDetailResponse> getTrackingOrderInfo(@PathVariable String id) {
        return BaseResponse.success(orderService.getTrackingOrderInfo(id));
    }

    // Admin cập nhật trạng thái đơn, bắt đầu xử lý sẽ ghi ngày giao dự kiến.
    @PatchMapping("/tracking/{id}/state")
    public BaseResponse<Void> updateOrderState(@PathVariable String id,
                                             @Valid @RequestBody UpdateOrderStateRequest request) {
        orderService.updateOrderState(id, request);
        return BaseResponse.success(null);
    }
}
