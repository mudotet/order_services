package com.example.order_services.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryResponse {
    private String id;
    private String productVariantId;
    private Integer quantityInStock;
}
