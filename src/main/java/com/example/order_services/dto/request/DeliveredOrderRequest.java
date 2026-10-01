package com.example.order_services.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class DeliveredOrderRequest {
    @NotBlank
    private String orderId;

    @NotBlank
    @Size(max = 255)
    private String customerName;

    @NotBlank
    @Size(max = 500)
    private String address;

    @NotBlank
    private String deliveryAttemptId;
}
