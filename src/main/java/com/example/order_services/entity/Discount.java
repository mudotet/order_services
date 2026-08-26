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
@Table(name = "discounts")
public class Discount extends BaseEntity {
    @Column(name = "percentage_discount", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentageDiscount;
}
