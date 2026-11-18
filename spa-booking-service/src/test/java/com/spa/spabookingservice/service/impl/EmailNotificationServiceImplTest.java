package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.entity.*;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Properties;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceImplTest {

    @Mock private JavaMailSender mailSender;

    @InjectMocks
    private EmailNotificationServiceImpl emailService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(emailService, "fromEmail", "noreply@lotusspa.com");
    }

    @Test
    @DisplayName("Gửi email HTML xác nhận đặt lịch thành công qua JavaMailSender")
    void sendBookingCreatedEmail_Success() {
        MimeMessage mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(101L)
                .customer(UserEntity.builder().fullName("Lan Anh").email("lananh@gmail.com").build())
                .branch(BranchEntity.builder().name("Lotus Q1").address("Quận 1").build())
                .appointmentDate(LocalDate.of(2026, 12, 1))
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .totalAmount(new BigDecimal("400000"))
                .details(List.of(AppointmentDetailEntity.builder()
                        .service(ServiceEntity.builder().name("Chăm sóc da mặt").build())
                        .build()))
                .build();

        emailService.sendBookingCreatedEmail(appointment);

        verify(mailSender, times(1)).send(mimeMessage);
    }
}