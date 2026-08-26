package com.example.order_services.entity;

import com.example.order_services.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "cart_items",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_cart_item",
                columnNames = {"cart_id", "product_variant_id"}
        )
)
public class CartItem extends BaseEntity {
    @Column(name = "cart_id", nullable = false, length = 36)
    private String cartId;

    @Column(name = "product_variant_id", nullable = false, length = 36)
    private String productVariantId;

    @Column(name = "product_quantity", nullable = false)
    private Integer productQuantity;
}
