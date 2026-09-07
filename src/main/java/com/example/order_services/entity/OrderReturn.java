package com.example.order_services.entity;

import com.example.order_services.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_returns",
        uniqueConstraints = @UniqueConstraint(name = "uk_order_returns_return_code", columnNames = "return_code"),
        check = @CheckConstraint(name = "chk_order_returns_refund_amount", constraint = "refund_amount >= 0"))
public class OrderReturn extends BaseEntity {
    @Column(name = "return_code", nullable = false, length = 50)
    private String returnCode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_returns_order"))
    private Order order;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "origin_type", nullable = false, length = 50)
    private String originType;

    @Builder.Default
    @Column(name = "refund_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundAmount = new BigDecimal("0.00");

    @Builder.Default
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt = LocalDateTime.now();

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "inspected_at")
    private LocalDateTime inspectedAt;

    @Column(name = "restocked_at")
    private LocalDateTime restockedAt;

    @Column(name = "refunded_at")
    private LocalDateTime refundedAt;
}
