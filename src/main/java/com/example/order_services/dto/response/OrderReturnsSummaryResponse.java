package com.example.order_services.dto.response;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderReturnsSummaryResponse {
    private Integer activeReturnCount;
    private Integer activeReturnChangePercentage;
    private Integer awaitInspectionCount;
    private Integer averageCycleTime;
    private BigDecimal totalRefunds;
}
