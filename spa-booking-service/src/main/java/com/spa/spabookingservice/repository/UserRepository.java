package com.spa.spabookingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.spa.spabookingservice.entity.UserEntity;

import java.util.Optional;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByEmail(String email);
    Boolean existsByEmail(String email);
    Boolean existsByPhone(String phone);
}