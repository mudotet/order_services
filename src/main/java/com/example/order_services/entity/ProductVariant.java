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
@Table(name = "product_variants")
public class ProductVariant extends BaseEntity {
    @Column(name = "product_variant", nullable = false)
    private String productVariant;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;
}
