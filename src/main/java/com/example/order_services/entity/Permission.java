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
@Entity
@Builder
@AllArgsConstructor
@Table(name = "permissions", uniqueConstraints = @UniqueConstraint(name = "uk_permissions_name", columnNames = "permission_name"))
public class Permission extends BaseEntity {
    @Column(name = "permission_name", nullable = false, length = 100)
    private String permissionName;
}
