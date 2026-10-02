package com.example.order_services.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AddCartItemRequest(@NotBlank String productVariantId, @NotNull @Min(1) Integer quantity) {}
