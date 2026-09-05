package com.example.order_services.repository;

import com.example.order_services.entity.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface UserRoleRepository extends JpaRepository<UserRole, String> {
    @EntityGraph(attributePaths = "role")
    List<UserRole> findAllByUser_IdAndDeletedFalseAndRole_DeletedFalse(String userId);
}
