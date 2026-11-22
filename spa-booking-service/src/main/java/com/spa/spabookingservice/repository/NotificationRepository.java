package com.spa.spabookingservice.repository;

import com.spa.spabookingservice.entity.NotificationEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {

    List<NotificationEntity> findTop20ByRecipientEmailOrderByCreatedAtDesc(String recipientEmail);

    long countByRecipientEmailAndReadFalse(String recipientEmail);

    @Modifying
    @Query("UPDATE NotificationEntity n SET n.read = true WHERE n.recipientEmail = :email AND n.read = false")
    void markAllAsReadByEmail(String email);
}