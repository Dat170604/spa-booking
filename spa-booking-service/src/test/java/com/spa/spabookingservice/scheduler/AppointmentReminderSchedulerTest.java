package com.spa.spabookingservice.scheduler;

import com.spa.spabookingservice.entity.AppointmentEntity;
import com.spa.spabookingservice.repository.AppointmentRepository;
import com.spa.spabookingservice.service.EmailNotificationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentReminderSchedulerTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private EmailNotificationService emailNotificationService;

    @InjectMocks
    private AppointmentReminderScheduler scheduler;

    @Test
    @DisplayName("1. Gửi nhắc hẹn thành công và đánh dấu reminded = true")
    void sendUpcomingAppointmentReminders_Success() {
        AppointmentEntity appt = AppointmentEntity.builder().id(1L).isReminded(false).build();
        when(appointmentRepository.findAppointmentsToRemind(any(), any(), any()))
                .thenReturn(List.of(appt));

        scheduler.sendUpcomingAppointmentReminders();

        assertTrue(appt.isReminded());
        verify(emailNotificationService, times(1)).sendAppointmentReminderEmail(appt);
        verify(appointmentRepository).saveAll(List.of(appt));
    }

    @Test
    @DisplayName("2. Tự động hủy các lịch hẹn quá hạn và ghi chú rõ lý do")
    void autoCancelOverdueAppointments_Success() {
        AppointmentEntity overdue = AppointmentEntity.builder().id(2L).status("PENDING").notes("").build();
        when(appointmentRepository.findOverdueAppointments(any(), any()))
                .thenReturn(List.of(overdue));

        scheduler.autoCancelOverdueAppointments();

        assertEquals("CANCELLED", overdue.getStatus());
        assertTrue(overdue.getNotes().contains("Hệ thống tự động hủy"));
        verify(appointmentRepository).saveAll(List.of(overdue));
    }
}