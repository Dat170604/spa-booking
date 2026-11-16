package com.spa.spabookingservice.service;

import com.spa.spabookingservice.entity.AppointmentEntity;

public interface EmailNotificationService {
    void sendBookingCreatedEmail(AppointmentEntity appointment);

    void sendStatusChangedEmail(AppointmentEntity appointment, String reason);

    void sendAppointmentReminderEmail(AppointmentEntity appointment);


}
