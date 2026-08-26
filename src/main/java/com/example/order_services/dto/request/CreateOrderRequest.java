package com.example.order_services.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    @NotBlank
    private String userId;

    @NotBlank
    private String addressId;

    @NotBlank
    private String paymentId;

    private String discountId;
}
