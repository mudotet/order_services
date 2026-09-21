package com.example.order_services.dto.request;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddProductVariantRequest {
    @NonNull
    private String productId;
    @NonNull
    private String productVariant;
    @NonNull
    private Double price;
}
