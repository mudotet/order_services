package com.example.order_services.dto.request;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddProductRequest {
    @NonNull
    private String productId;
    @NonNull
    private String productName;
    @NonNull
    private String productType;
}
