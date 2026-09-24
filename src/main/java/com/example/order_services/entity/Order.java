package com.example.order_services.entity;

import com.example.order_services.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@Entity
@AllArgsConstructor
@Builder
@Table(name = "orders", check = {
        @CheckConstraint(name = "chk_orders_subtotal", constraint = "subtotal >= 0"),
        @CheckConstraint(name = "chk_orders_discount_amount", constraint = "discount_amount >= 0"),
        @CheckConstraint(name = "chk_orders_discount_not_over_subtotal", constraint = "discount_amount <= subtotal"),
        @CheckConstraint(name = "chk_orders_shipping_fee", constraint = "shipping_fee >= 0"),
        @CheckConstraint(name = "chk_orders_total", constraint = "total >= 0")
})
public class Order extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "discount_id")
    private Discount discount;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(name = "address_id", nullable = false, length = 36)
    private String addressId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_state_id", nullable = false)
    private OrderState orderState;

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal = new BigDecimal("0.00");

    @Builder.Default
    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount = new BigDecimal("0.00");

    @Builder.Default
    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee = new BigDecimal("0.00");

    @Builder.Default
    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total = new BigDecimal("0.00");

    @Column(name = "estimated_delivery")
    private LocalDate estimatedDelivery;
}
