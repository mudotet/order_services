package com.example.order_services.controller;

import com.example.order_services.common.BaseResponse;
import com.example.order_services.dto.request.DeliveredOrderRequest;
import com.example.order_services.dto.request.FailedOrderRequest;
import com.example.order_services.dto.request.ReceiveOrderRequest;
import com.example.order_services.dto.response.OrderDeliveryResponse;
import com.example.order_services.service.ShipperService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/shipper/orders")
@PreAuthorize("hasRole('SHIPPER')")
public class ShipperController {
    private final ShipperService shipperService;

    @PostMapping("/receive")
    public BaseResponse<OrderDeliveryResponse> receive(@Valid @RequestBody ReceiveOrderRequest request) {
        return BaseResponse.success(shipperService.receive(request));
    }

    @PostMapping("/delivered")
    public BaseResponse<OrderDeliveryResponse> delivered(@Valid @RequestBody DeliveredOrderRequest request) {
        return BaseResponse.success(shipperService.delivered(request));
    }

    @PostMapping("/failed")
    public BaseResponse<OrderDeliveryResponse> failed(@Valid @RequestBody FailedOrderRequest request) {
        return BaseResponse.success(shipperService.failed(request));
    }
}
