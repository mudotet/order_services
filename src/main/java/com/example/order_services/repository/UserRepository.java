package com.example.order_services.repository;

import com.example.order_services.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmailAndDeletedFalse(String email);

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    Optional<User> findByIdAndDeletedFalse(String id);

    @org.springframework.data.jpa.repository.Query("""
            select distinct u from User u, UserRole ur where ur.user = u
              and u.deleted = false and ur.deleted = false and ur.role.deleted = false
              and ur.role.roleName = 'SHIPPER' order by u.userName, u.id
            """)
    java.util.List<User> findActiveShippers();
}
