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

    // Chạy mỗi 15 phút (900,000 ms)
    @Scheduled(fixedRate = 900000)
    @Transactional
    public void sendUpcomingAppointmentReminders() {
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        LocalTime twoHoursLater = now.plusHours(2);

        List<AppointmentEntity> upcomingList = appointmentRepository
                .findAppointmentsToRemind(today, now, twoHoursLater);

        if (!upcomingList.isEmpty()) {
            for (AppointmentEntity appointment : upcomingList) {
                emailNotificationService.sendAppointmentReminderEmail(appointment);
                appointment.setReminded(true);
            }
            appointmentRepository.saveAll(upcomingList);
            log.info("Đã kích hoạt gửi email nhắc lịch cho {} khách hàng.", upcomingList.size());
        }
    }
}