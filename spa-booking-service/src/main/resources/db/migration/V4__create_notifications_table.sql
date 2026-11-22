CREATE TABLE notifications (
                               id BIGSERIAL PRIMARY KEY,
                               recipient_email VARCHAR(255) NOT NULL,
                               title VARCHAR(255) NOT NULL,
                               message TEXT NOT NULL,
                               type VARCHAR(50) NOT NULL, -- BOOKING_CREATED, BOOKING_STATUS, LEAVE_REQUEST, LEAVE_STATUS
                               target_url VARCHAR(255),
                               is_read BOOLEAN NOT NULL DEFAULT FALSE,
                               created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notifications_recipient ON notifications(recipient_email, is_read, created_at DESC);