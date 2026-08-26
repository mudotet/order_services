package com.example.order_services.entity;

import com.example.order_services.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "inventories")
public class Inventory extends BaseEntity {
    @Column(name = "product_variant_id", nullable = false, length = 36)
    private String productVariantId;

    @Column(name = "quantity_in_stock", nullable = false)
    private Integer quantityInStock;
}
