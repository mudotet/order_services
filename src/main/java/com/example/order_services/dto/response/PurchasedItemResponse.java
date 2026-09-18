package com.example.order_services.dto.response;


import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchasedItemResponse {
    private String purchasedItemName;
    private String purchasedItemDescription;
    private Integer purchasedItemQuantity;
    private BigDecimal purchasedItemPrice;
}
