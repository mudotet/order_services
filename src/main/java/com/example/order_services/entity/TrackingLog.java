package com.example.order_services.entity;

import com.example.order_services.common.DeliveryFailureReason;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "tracking_logs")
public class TrackingLog {
    @Id
    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_status")
    private OrderState oldStatus;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_status", nullable = false)
    private OrderState newStatus;

    @Column(name = "take_note", length = 500)
    private String takeNote;

    @Column(name = "recipient_name", length = 255, updatable = false)
    private String recipientName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(name = "failure_reason", length = 50)
    private DeliveryFailureReason failureReason;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "delivery_attempt_id", length = 36)
    private String deliveryAttemptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location")
    private Address location;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "created_by", length = 36, updatable = false)
    private String createdBy;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @JdbcTypeCode(SqlTypes.TINYINT)
    @Column(nullable = false)
    private boolean deleted;

    @PrePersist
    void createAuditFields() {
        if (id == null) {
            id = UUID.randomUUID().toString();
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
