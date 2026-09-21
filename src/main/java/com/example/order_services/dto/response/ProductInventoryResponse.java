package com.example.order_services.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.BigInteger;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductInventoryResponse {
    private String productName;
    private String productVariantId;
    private BigDecimal productPrice;
    private BigInteger productStockQuantity;
    private String productStockState;
}
