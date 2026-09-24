package com.example.order_services.entity;

import com.example.order_services.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "product_variants",
        check = @CheckConstraint(name = "chk_product_variant_price", constraint = "price >= 0"))
public class ProductVariant extends BaseEntity {
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "product_variant", nullable = false)
    private String productVariant;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;
}
