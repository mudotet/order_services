package com.example.order_services.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductInventoryResponse {
    private String productName;
    private String productVariantId;
    private BigDecimal productPrice;
    private Integer productStockQuantity;
    private String productStockState;
}
