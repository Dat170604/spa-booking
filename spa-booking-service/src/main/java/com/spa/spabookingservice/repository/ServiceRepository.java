package com.spa.spabookingservice.repository;

import com.spa.spabookingservice.entity.ServiceEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ServiceRepository extends JpaRepository<ServiceEntity, Long> {
    List<ServiceEntity> findByStatus(String status);

    @Query("SELECT s FROM ServiceEntity s WHERE " +
            "(:keyword IS NULL OR :keyword = '' OR LOWER(s.name) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
            "AND (:category IS NULL OR :category = '' OR s.category = :category)")
    Page<ServiceEntity> searchServices(@Param("keyword") String keyword, @Param("category") String category, Pageable pageable);
}