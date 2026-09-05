package com.example.order_services.repository;

import com.example.order_services.entity.UserDiscount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

import java.util.List;

public interface UserDiscountRepository extends JpaRepository<UserDiscount, String> {
    // ponytail: date validation deferred by request; add it to both lookups when implemented.
    @Query("""
            select assignment from UserDiscount assignment
            join fetch assignment.discount discount
            where assignment.user.id = :userId and assignment.deleted = false
              and discount.deleted = false and assignment.used = false
              and assignment.status = 'AVAILABLE'
            """)
    List<UserDiscount> findAvailableByUserId(@Param("userId") String userId);

    @Query("""
            select assignment from UserDiscount assignment
            join fetch assignment.discount discount
            where assignment.user.id = :userId and discount.id = :discountId
              and assignment.deleted = false and discount.deleted = false
              and assignment.used = false and assignment.status = 'AVAILABLE'
            """)
    Optional<UserDiscount> findAvailableAssignment(@Param("userId") String userId,
                                                  @Param("discountId") String discountId);
}
