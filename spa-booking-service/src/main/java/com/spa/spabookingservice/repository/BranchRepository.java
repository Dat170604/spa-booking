package com.spa.spabookingservice.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.spa.spabookingservice.entity.BranchEntity;

public interface BranchRepository extends JpaRepository<BranchEntity, Long> {
}