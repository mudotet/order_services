package com.example.order_services.dto.request;

import com.example.order_services.common.DeliveryFailureReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FailedOrderRequest extends DeliveredOrderRequest {
    @NotNull
    private DeliveryFailureReason failureReason;

    @Size(max = 500)
    private String note;
}
