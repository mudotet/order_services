package com.example.order_services.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturnItemResponse  extends OrderReturnResponse{
    private String orderItemId;
    private String productVariantId;
    private String productName;
    private String reasonType;
    private Integer quantity;
    private String conditionStatus;
    private BigDecimal unitPrice;
    private BigDecimal refundAmount;
}
