package com.example.order_services.entity;

import com.example.order_services.common.AuditEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Builder
@AllArgsConstructor
@Table(name = "role_permissions")
public class RolePermission extends AuditEntity {
    @EmbeddedId
    @Builder.Default
    private RolePermissionId id = new RolePermissionId();

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @MapsId("permissionId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id", nullable = false)
    private Permission permission;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class RolePermissionId implements Serializable {
        @JdbcTypeCode(SqlTypes.CHAR)
        @Column(name = "role_id", length = 36)
        private String roleId;
        @JdbcTypeCode(SqlTypes.CHAR)
        @Column(name = "permission_id", length = 36)
        private String permissionId;
    }
}
