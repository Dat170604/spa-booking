package com.spa.spabookingservice.repository;

import com.spa.spabookingservice.entity.BranchEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BranchRepository extends JpaRepository<BranchEntity, Long> {

    List<BranchEntity> findByActiveTrue();

    boolean existsByName(String name);

    @Query("SELECT b FROM BranchEntity b WHERE " +
            "(:keyword IS NULL OR LOWER(b.name) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR LOWER(b.address) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
            "OR b.phone LIKE CONCAT('%', :keyword, '%'))")
    Page<BranchEntity> searchBranches(@Param("keyword") String keyword, Pageable pageable);
}