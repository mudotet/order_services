package com.example.order_services.service;

import com.example.order_services.dto.request.CreateOrderRequest;
import com.example.order_services.dto.request.UpdateOrderStateRequest;
import com.example.order_services.dto.response.*;
import org.springframework.data.domain.Page;

import java.io.IOException;
import java.nio.file.Path;

/** Đặt và theo dõi đơn cho người dùng; quản lý đơn trả hàng cho admin. */
public interface OrderService {
    OrderSummaryResponse calculateOrderSummary(String discountId);
    OrderResponse createOrder(CreateOrderRequest request);

    OrderReturnsSummaryResponse calculateOrderReturnSummary();

    Page<OrderReturnResponse> getOrderReturns(int page, int size, String filterBy);

    /** Caller must delete the completed temporary file after sending it. */
    Path exportOrderReturns(String filterBy) throws IOException;

    ViewOrderDetailResponse viewOrderReturnDetail(String id);

    TrackingOrderDetailResponse getTrackingOrderInfo(String id);

    void updateOrderState(String id, UpdateOrderStateRequest request);
}
