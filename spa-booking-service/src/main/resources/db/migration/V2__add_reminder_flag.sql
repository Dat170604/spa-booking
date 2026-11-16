-- V2__add_reminder_flag.sql
ALTER TABLE appointments
    ADD COLUMN is_reminded BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_appointments_reminder
    ON appointments (status, is_reminded, appointment_date, start_time);