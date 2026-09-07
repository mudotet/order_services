package com.example.order_services.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ViewOrderDetailResponse extends OrderReturnResponse {
    private List<OrderReturnItemResponse> items;
}