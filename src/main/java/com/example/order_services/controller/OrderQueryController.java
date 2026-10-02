package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.response.OrderViewResponse;
import com.example.order_services.service.OrderQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class OrderQueryController {
    private final OrderQueryService service;

    @GetMapping("/orders")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<Page<OrderViewResponse>> orders(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String state,
            @RequestParam(required = false) String query) {
        return BaseResponse.success(service.orders(page, size, state, query, "ADMIN"));
    }

    @GetMapping("/orders/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<OrderViewResponse> detail(@PathVariable String id) {
        return BaseResponse.success(service.detail(id, false));
    }

    @GetMapping("/orders/mine")
    @PreAuthorize("hasRole('USER')")
    public BaseResponse<Page<OrderViewResponse>> mine(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String state,
            @RequestParam(required = false) String query) {
        return BaseResponse.success(service.orders(page, size, state, query, "USER"));
    }

    @GetMapping("/shipper/orders")
    @PreAuthorize("hasRole('SHIPPER')")
    public BaseResponse<Page<OrderViewResponse>> assigned(@RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, @RequestParam(required = false) String state,
            @RequestParam(required = false) String query) {
        return BaseResponse.success(service.orders(page, size, state, query, "SHIPPER"));
    }

    @GetMapping("/shipper/orders/{id}")
    @PreAuthorize("hasRole('SHIPPER')")
    public BaseResponse<OrderViewResponse> assignedDetail(@PathVariable String id) {
        return BaseResponse.success(service.detail(id, true));
    }

    @GetMapping("/shippers")
    @PreAuthorize("hasRole('ADMIN')")
    public BaseResponse<List<OrderViewResponse.Shipper>> shippers() {
        return BaseResponse.success(service.shippers());
    }

    @GetMapping("/catalog")
    @PreAuthorize("hasRole('USER')")
    public BaseResponse<List<OrderViewResponse.Catalog>> catalog() {
        return BaseResponse.success(service.catalog());
    }

    @GetMapping("/addresses")
    @PreAuthorize("hasRole('USER')")
    public BaseResponse<List<OrderViewResponse.AddressOption>> addresses() {
        return BaseResponse.success(service.addresses());
    }

    @GetMapping("/payments")
    @PreAuthorize("hasRole('USER')")
    public BaseResponse<List<OrderViewResponse.PaymentOption>> payments() {
        return BaseResponse.success(service.payments());
    }
}
