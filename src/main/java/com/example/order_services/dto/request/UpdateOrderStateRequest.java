package com.example.order_services.dto.request;

import com.example.order_services.common.OrderStatus;
import com.example.order_services.common.DeliveryFailureReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStateRequest {
    @NotNull
    private OrderStatus state;

    @Pattern(regexp = "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}")
    private String deliveryAttemptId;

    private DeliveryFailureReason failureReason;

    @Size(max = 500)
    private String note;

    @Size(max = 255)
    private String recipientName;
}
