-- Bảng 1: Vai trò người dùng (Roles)
CREATE TABLE roles (
                       id BIGSERIAL PRIMARY KEY,
                       name VARCHAR(50) NOT NULL UNIQUE
);

-- Bảng 2: Chi nhánh Spa (Branches)
CREATE TABLE branches (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(150) NOT NULL,
                          address VARCHAR(255) NOT NULL,
                          phone VARCHAR(20) NOT NULL,
                          email VARCHAR(100),
                          description TEXT,
                          image_url VARCHAR(255),
                          open_time TIME,
                          close_time TIME,
                          is_active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Bảng 3: Người dùng (Users - Khách hàng, KTV, Admin)
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       branch_id BIGINT NULL,
                       full_name VARCHAR(100) NOT NULL,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       phone VARCHAR(20) NOT NULL UNIQUE,
                       password VARCHAR(255) NOT NULL,
                       status VARCHAR(20) DEFAULT 'ACTIVE', -- ACTIVE, INACTIVE
                       created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT fk_user_branch FOREIGN KEY (branch_id) REFERENCES branches(id) ON DELETE SET NULL
);

-- Bảng 4: Bảng trung gian User - Role (Quan hệ N-N)
CREATE TABLE user_roles (
                            user_id BIGINT NOT NULL,
                            role_id BIGINT NOT NULL,
                            PRIMARY KEY (user_id, role_id),
                            CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
                            CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

-- Bảng 5: Quản lý Refresh Token (Dùng cho JWT Refresh Token Flow)
CREATE TABLE refresh_tokens (
                                id BIGSERIAL PRIMARY KEY,
                                user_id BIGINT NOT NULL,
                                token VARCHAR(255) NOT NULL UNIQUE,
                                expiry_date TIMESTAMP WITH TIME ZONE NOT NULL,
                                revoked BOOLEAN DEFAULT FALSE,
                                CONSTRAINT fk_refresh_token_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Bảng 6: Dịch vụ Spa (Services)
CREATE TABLE services (
                          id BIGSERIAL PRIMARY KEY,
                          name VARCHAR(150) NOT NULL,
                          category VARCHAR(50),
                          image_url VARCHAR(500),
                          description TEXT,
                          price NUMERIC(12, 2) NOT NULL,
                          duration_minutes INT NOT NULL, -- Thời lượng dịch vụ (phút)
                          status VARCHAR(20) DEFAULT 'ACTIVE',
                          created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Bảng 7: Đặt lịch (Appointments)
CREATE TABLE appointments (
                              id BIGSERIAL PRIMARY KEY,
                              customer_id BIGINT NOT NULL,
                              branch_id BIGINT NOT NULL,
                              appointment_date DATE NOT NULL,
                              start_time TIME NOT NULL,
                              end_time TIME NOT NULL,
                              total_amount NUMERIC(12, 2) NOT NULL,
                              status VARCHAR(30) DEFAULT 'PENDING', -- PENDING, CONFIRMED, COMPLETED, CANCELLED
                              notes TEXT,
                              created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
                              CONSTRAINT fk_appointment_customer FOREIGN KEY (customer_id) REFERENCES users(id),
                              CONSTRAINT fk_appointment_branch FOREIGN KEY (branch_id) REFERENCES branches(id)
);

-- Bảng 8: Chi tiết Đặt lịch & Phân công KTV (Appointment Details)
CREATE TABLE appointment_details (
                                     id BIGSERIAL PRIMARY KEY,
                                     appointment_id BIGINT NOT NULL,
                                     service_id BIGINT NOT NULL,
                                     staff_id BIGINT NULL, -- Kỹ thuật viên thực hiện (gán sau)
                                     price NUMERIC(12, 2) NOT NULL,
                                     CONSTRAINT fk_detail_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id) ON DELETE CASCADE,
                                     CONSTRAINT fk_detail_service FOREIGN KEY (service_id) REFERENCES services(id),
                                     CONSTRAINT fk_detail_staff FOREIGN KEY (staff_id) REFERENCES users(id)
);

INSERT INTO roles (name) VALUES
                             ('ROLE_ADMIN'),
                             ('ROLE_STAFF'),
                             ('ROLE_CUSTOMER')
ON CONFLICT (name) DO NOTHING;