package com.spa.spabookingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.spa.spabookingservice.entity.ServiceEntity;

import java.util.List;

public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByStatus(String status);
}