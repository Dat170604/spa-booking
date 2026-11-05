package com.spa.spabookingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.spa.spabookingservice.entity.RoleEntity;

import java.util.Optional;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
    Optional<RoleEntity> findByName(String name);
}