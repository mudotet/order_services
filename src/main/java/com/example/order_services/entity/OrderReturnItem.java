package com.example.order_services.entity;

import com.example.order_services.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_return_items",
        uniqueConstraints = @UniqueConstraint(name = "uk_order_return_items", columnNames = {"order_return_id", "order_item_id"}),
        check = {
                @CheckConstraint(name = "chk_order_return_items_quantity", constraint = "quantity > 0"),
                @CheckConstraint(name = "chk_order_return_items_unit_price", constraint = "unit_price >= 0"),
                @CheckConstraint(name = "chk_order_return_items_refund_amount", constraint = "refund_amount >= 0")
        })
public class OrderReturnItem extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_return_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_return_items_return"))
    private OrderReturn orderReturn;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_item_id", nullable = false, foreignKey = @ForeignKey(name = "fk_order_return_items_order_item"))
    private OrderItem orderItem;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "reason_type", nullable = false, length = 50)
    private String reasonType;

    @Column(name = "reason_detail", length = 500)
    private String reasonDetail;

    @Column(name = "condition_status", length = 50)
    private String conditionStatus;

    @Column(name = "unit_price", nullable = false, precision = 15, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "refund_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal refundAmount;
}
