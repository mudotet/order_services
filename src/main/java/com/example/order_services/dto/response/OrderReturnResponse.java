package com.example.order_services.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    /** Số phút nguyên từ lúc tạo lượt trả đến hiện tại; thiếu thời điểm tạo thì trả 0. */
    public Integer calculateInitialTime(LocalDateTime createdAt){
        if (createdAt == null) {
            return 0;
        }
        long minutes = Duration.between(createdAt, LocalDateTime.now()).toMinutes();
        return (int) minutes;
    }
}
