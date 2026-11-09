package com.spa.spabookingservice.repository;

import com.spa.spabookingservice.entity.AppointmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface AppointmentRepository extends JpaRepository<AppointmentEntity, Long> {
    List<AppointmentEntity> findByCustomerIdOrderByAppointmentDateDescStartTimeDesc(Long customerId);
    List<AppointmentEntity> findByBranchId(Long branchId);
    List<AppointmentEntity> findByStatus(String status);
}