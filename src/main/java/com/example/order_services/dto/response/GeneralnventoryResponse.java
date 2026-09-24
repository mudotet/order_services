package com.example.order_services.dto.response;
import lombok.*;

import java.math.BigDecimal;
import java.math.BigInteger;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeneralnventoryResponse {
    private BigDecimal totalInventoryValue;
    private BigInteger totalProductsInStock;
    private BigInteger totalProductsPrepareToOutOfStock;
}
