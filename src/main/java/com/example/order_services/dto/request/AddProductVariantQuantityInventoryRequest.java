package com.example.order_services.dto.request;

import lombok.*;
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddProductVariantQuantityInventoryRequest {
    @NonNull
    private Integer quantity;
}
