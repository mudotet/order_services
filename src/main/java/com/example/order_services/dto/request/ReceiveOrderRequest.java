package com.example.order_services.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReceiveOrderRequest {
    @NotBlank
    private String orderId;
}
