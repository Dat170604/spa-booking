package com.spa.spabookingservice.scheduler;

import com.spa.spabookingservice.entity.AppointmentEntity;
import com.spa.spabookingservice.repository.AppointmentRepository;
import com.spa.spabookingservice.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppointmentReminderScheduler {

    private final AppointmentRepository appointmentRepository;
    private final EmailNotificationService emailNotificationService;

    // 1. Chạy mỗi 15 phút sau khi lần trước hoàn tất: Nhắc hẹn trước 2 tiếng
    @Scheduled(fixedDelay = 900000)
    @Transactional
    public void sendUpcomingAppointmentReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime twoHoursLater = now.plusHours(2);

        List<AppointmentEntity> upcomingList = appointmentRepository
                .findAppointmentsToRemind(today, now, twoHoursLater);

        if (!upcomingList.isEmpty()) {
            for (AppointmentEntity appointment : upcomingList) {
                try {
                    emailNotificationService.sendAppointmentReminderEmail(appointment);
                    appointment.setReminded(true);
                } catch (Exception e) {
                    log.error("Lỗi gửi email nhắc lịch #{}: {}", appointment.getId(), e.getMessage());
                }
            }
            appointmentRepository.saveAll(upcomingList);
            log.info("Đã kích hoạt gửi email nhắc lịch cho {} khách hàng.", upcomingList.size());
        }
    }

    // 2. Chạy lúc 23:30 hằng ngày: Tự động hủy các lịch hẹn đã qua giờ nhưng vẫn PENDING/CONFIRMED
    @Scheduled(cron = "0 30 23 * * *")
    @Transactional
    public void autoCancelOverdueAppointments() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();

        List<AppointmentEntity> overdueList = appointmentRepository.findOverdueAppointments(today, now);
        if (!overdueList.isEmpty()) {
            for (AppointmentEntity appointment : overdueList) {
                appointment.setStatus("CANCELLED");
                String oldNote = (appointment.getNotes() != null && !appointment.getNotes().isBlank())
                        ? appointment.getNotes() + " | "
                        : "";
                appointment.setNotes(oldNote + "Hệ thống tự động hủy do quá giờ hẹn");
            }
            appointmentRepository.saveAll(overdueList);
            log.info("Đã tự động hủy {} lịch hẹn quá hạn.", overdueList.size());
        }
    }
}