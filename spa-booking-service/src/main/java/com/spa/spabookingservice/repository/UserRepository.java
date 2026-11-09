package com.spa.spabookingservice.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import com.spa.spabookingservice.entity.UserEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Boolean existsByEmail(String email);
    Boolean existsByPhone(String phone);

    @Query("SELECT u FROM UserEntity u JOIN u.roles r WHERE u.branch.id = :branchId AND r.name = 'ROLE_STAFF' AND u.status = 'ACTIVE'")
    List<UserEntity> findActiveStaffByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT DISTINCT u FROM UserEntity u LEFT JOIN u.roles r WHERE " +
            "(:keyword IS NULL OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR u.phone LIKE CONCAT('%', :keyword, '%')) " +
            "AND (:roleName IS NULL OR r.name = :roleName)")
    Page<UserEntity> searchUsers(@Param("keyword") String keyword, @Param("roleName") String roleName, Pageable pageable);


}