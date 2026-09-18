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
@Table(name = "user_roles")
public class UserRole extends AuditEntity {
    @EmbeddedId
    @Builder.Default
    private UserRoleId id = new UserRoleId();

    @MapsId("userId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @MapsId("roleId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false)
    private Role role;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Embeddable
    public static class UserRoleId implements Serializable {
        @JdbcTypeCode(SqlTypes.CHAR)
        @Column(name = "user_id", length = 36)
        private String userId;
        @JdbcTypeCode(SqlTypes.CHAR)
        @Column(name = "role_id", length = 36)
        private String roleId;
    }
}
