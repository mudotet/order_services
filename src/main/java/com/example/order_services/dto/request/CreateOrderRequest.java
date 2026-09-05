package com.example.order_services.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateOrderRequest {
    private String discountId;

    @NotBlank
    private String addressId;

    @NotBlank
    private String paymentId;

}
