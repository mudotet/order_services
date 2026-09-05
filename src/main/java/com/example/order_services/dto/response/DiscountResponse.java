package com.example.order_services.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscountResponse {
    private String discountId;
    private String discountType;
    private BigDecimal discountValue;
}
