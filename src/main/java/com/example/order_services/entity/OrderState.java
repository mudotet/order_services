package com.example.order_services.entity;

import com.example.order_services.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "order_states", uniqueConstraints = @UniqueConstraint(name = "uk_order_state", columnNames = "state"))
public class OrderState extends BaseEntity {
    @Column(name = "state", nullable = false, length = 50)
    private String state;
}
