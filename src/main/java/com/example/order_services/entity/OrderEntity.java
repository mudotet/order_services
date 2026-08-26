package com.example.order_services.entity;

import com.example.order_services.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "orders")
public class OrderEntity extends BaseEntity {
    @Column(name = "user_id", nullable = false, length = 36)
    private String userId;

    @Column(name = "discount_id", length = 36)
    private String discountId;

    @Column(name = "payment_id", length = 36)
    private String paymentId;

    @Column(name = "address_id", nullable = false, length = 36)
    private String addressId;

    @Column(name = "order_state_id", nullable = false, length = 36)
    private String orderStateId;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal subtotal;

    @Column(name = "discount_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "shipping_fee", nullable = false, precision = 15, scale = 2)
    private BigDecimal shippingFee;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal total;
}
