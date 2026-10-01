package com.example.order_services.service;

import com.example.order_services.dto.request.DeliveredOrderRequest;
import com.example.order_services.dto.request.FailedOrderRequest;
import com.example.order_services.dto.request.ReceiveOrderRequest;
import com.example.order_services.dto.response.OrderDeliveryResponse;

public interface ShipperService {
    OrderDeliveryResponse receive(ReceiveOrderRequest request);
    OrderDeliveryResponse delivered(DeliveredOrderRequest request);
    OrderDeliveryResponse failed(FailedOrderRequest request);
}
