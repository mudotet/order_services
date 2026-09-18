package com.example.order_services.dto.response;

import com.alibaba.excel.annotation.ExcelIgnore;
import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@RequiredArgsConstructor
public class ExportOrderReturn {
    @ExcelProperty(value = "returnId", index = 0)
    private final String returnId;

    @ExcelIgnore
    private final LocalDateTime createdAt;

    @ExcelProperty(value = "customerName", index = 2)
    private final String customerName;

    @ExcelProperty(value = "originType", index = 4)
    private final String originType;

    @ExcelProperty(value = "orderReturnStatus", index = 5)
    private final String orderReturnStatus;

    @ExcelProperty(value = "initialTime", index = 1)
    private Long initialTime;

    @ExcelProperty(value = "reasonReturn", index = 3)
    private String reasonReturn;
}
