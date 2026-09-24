package com.example.order_services.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ViewOrderDetailResponse extends OrderReturnResponse {
    private List<OrderReturnItemResponse> items;
}
