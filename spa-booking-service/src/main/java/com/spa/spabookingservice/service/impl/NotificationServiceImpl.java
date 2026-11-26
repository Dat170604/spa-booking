package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.entity.NotificationEntity;
import com.spa.spabookingservice.entity.UserEntity;
import com.spa.spabookingservice.repository.NotificationRepository;
import com.spa.spabookingservice.repository.UserRepository;
import com.spa.spabookingservice.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * Gửi thông báo cá nhân tới 1 người dùng cụ thể (lưu DB + đẩy real-time qua WebSocket)
     */
    @Transactional
    public void sendToUser(String recipientEmail, String title, String message, String type, String targetUrl) {
        NotificationEntity saved = notificationRepository.save(NotificationEntity.builder()
                .recipientEmail(recipientEmail)
                .title(title)
                .message(message)
                .type(type)
                .targetUrl(targetUrl)
                .build());

        // Đẩy tới kênh riêng của user: /user/queue/notifications
        messagingTemplate.convertAndSendToUser(recipientEmail, "/queue/notifications", saved);
    }

    /**
     * Gửi thông báo tới toàn bộ Quản trị viên (ROLE_ADMIN)
     */
    @Transactional
    public void sendToAllAdmins(String title, String message, String type, String targetUrl) {
        List<UserEntity> admins = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().contains("ADMIN")))
                .toList();

        for (UserEntity admin : admins) {
            sendToUser(admin.getEmail(), title, message, type, targetUrl);
        }
    }

    @Transactional(readOnly = true)
    public List<NotificationEntity> getMyNotifications(String email) {
        return notificationRepository.findTop20ByRecipientEmailOrderByCreatedAtDesc(email);
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {
        return notificationRepository.countByRecipientEmailAndReadFalse(email);
    }

    @Transactional
    public void markAsRead(Long id, String email) {
        notificationRepository.findById(id).ifPresent(n -> {
            if (n.getRecipientEmail().equals(email)) {
                n.setRead(true);
                notificationRepository.save(n);
            }
        });
    }

    @Transactional
    public void markAllAsRead(String email) {
        notificationRepository.markAllAsReadByEmail(email);
    }
}