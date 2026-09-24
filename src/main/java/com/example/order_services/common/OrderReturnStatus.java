package com.example.order_services.common;

public enum OrderReturnStatus {
    PENDING,
    IN_TRANSIT,
    WAREHOUSE_RECEIVED,
    INSPECTING,
    RESTOCKED,
    REFUNDED,
    REJECTED
}
