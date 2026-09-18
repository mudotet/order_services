package com.example.order_services.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackingOrderDetailResponse {
    private String orderTrackingId;
    private String orderTrackingStatus;
    private List<PurchasedItemResponse> purchasedItems;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private String paymentMethodInfo;
    private String shippingCity;
    private LocalDate estimatedDelivery;
    private Integer daysRemaining;
}
