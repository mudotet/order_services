package com.example.order_services.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private String productVariantId;
    private String productVariant;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal lineTotal;
}
