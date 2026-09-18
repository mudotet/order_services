package com.example.order_services.entity;

import com.example.order_services.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "inventories",
        uniqueConstraints = @UniqueConstraint(name = "uk_inventory_product_variant", columnNames = "product_variant_id"),
        check = @CheckConstraint(name = "chk_inventory_quantity", constraint = "quantity_in_stock >= 0"))
public class Inventory extends BaseEntity {
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_variant_id", nullable = false)
    private ProductVariant productVariant;

    @Builder.Default
    @Column(name = "quantity_in_stock", nullable = false)
    private Integer quantityInStock = 0;
}
