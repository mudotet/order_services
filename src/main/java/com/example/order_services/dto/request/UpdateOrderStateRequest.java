package com.example.order_services.dto.request;

import com.example.order_services.common.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateOrderStateRequest {
    @NotNull
    private OrderStatus state;
}
