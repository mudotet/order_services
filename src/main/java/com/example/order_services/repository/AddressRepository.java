package com.example.order_services.repository;

import com.example.order_services.entity.Address;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, String> {
    Optional<Address> findByIdAndDeletedFalse(String id);
}
