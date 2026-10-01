package com.example.order_services.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class ShipperOrderResponse {
    private String orderId;
    private String state;
    private String deliveryAttemptId;
    private String customerName;
    private String phoneNumber;
    private String address;
    private String city;
    private Long quantity;
    private LocalDateTime assignedAt;
    private LocalDate estimatedDelivery;
    private Integer daysRemaining;
    private String recipientName;
}
