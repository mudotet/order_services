package com.example.order_services.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItemResponse {
    private String cartItemId;
    private String productVariantId;
    private String productName;
    private BigDecimal lineTotal;
    private Integer quantity;
    private String stockStatus;
}
