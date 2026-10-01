package com.example.order_services.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class OrderDeliveryResponse {
    private String orderId;
    private String state;
    private String shipperId;
    private String deliveryAttemptId;
    private LocalDateTime assignedAt;
    private String recipientName;
}
