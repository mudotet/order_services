package com.example.order_services.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturnResponse {
    private String returnId;
    private Integer initialTime;
    private String customerName;
    private String reasonReturn;
    private String originType;
    private String orderReturnStatus;
}
