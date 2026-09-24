package com.example.order_services.dto.request;

import lombok.*;

import java.math.BigDecimal;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateProductInStockRequest {
    private String productName;
    private String productVariantId;
    private String productType;
    @DecimalMin("0")
    private BigDecimal productPrice;
    @Min(0)
    private Integer quantityInStock;
    private String productDescription;

}
