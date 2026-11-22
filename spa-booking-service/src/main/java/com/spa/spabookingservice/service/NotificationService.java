package com.spa.spabookingservice.service;

import com.spa.spabookingservice.entity.NotificationEntity;

import java.util.List;

public interface NotificationService {
    void sendToUser(String recipientEmail, String title, String message, String type, String targetUrl);

    void sendToAllAdmins(String title, String message, String type, String targetUrl);

    List<NotificationEntity> getMyNotifications(String email);

    long getUnreadCount(String email);

    void markAsRead(Long id, String email);

    void markAllAsRead(String email);
}
