package com.example.order_services.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UpdateInventoryQuantityRequest {
    @NotNull
    @Min(0)
    private Integer quantity;
}
