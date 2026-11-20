CREATE TABLE staff_leaves (
                              id BIGSERIAL PRIMARY KEY,
                              staff_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                              leave_date DATE NOT NULL,
                              start_time TIME, -- NULL nếu nghỉ cả ngày
                              end_time TIME,   -- NULL nếu nghỉ cả ngày
                              full_day BOOLEAN NOT NULL DEFAULT TRUE,
                              reason VARCHAR(255),
                              status VARCHAR(20) NOT NULL DEFAULT 'PENDING', -- PENDING, APPROVED, REJECTED
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_staff_leaves_staff_date ON staff_leaves(staff_id, leave_date);