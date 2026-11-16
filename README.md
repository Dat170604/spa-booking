# 🌸 Lotus Spa — Hệ Thống Quản Lý & Đặt Lịch Chăm Sóc Sức Khỏe

**Lotus Spa** là ứng dụng web Full-stack giải quyết bài toán đặt lịch trực tuyến, điều phối kỹ thuật viên và quản lý vận hành chuỗi chi nhánh Spa. Hệ thống được thiết kế theo chuẩn kiến trúc doanh nghiệp, tập trung vào khả năng **xử lý đồng thời (concurrency)**, **bảo mật phiên đăng nhập**, **giới hạn lưu lượng (rate limiting)** và **tự động hóa tác vụ nền**.

---

## 🏗️ Kiến Trúc Hệ Thống

```text
                        ┌─────────────────────────────┐
                        │     Client (Browser/SPA)    │
                        └──────────────┬──────────────┘
                                       │ HTTP / REST API
                                       ▼
                        ┌─────────────────────────────┐
                        │    Nginx Reverse Proxy      │
                        │   (Angular Frontend :4200)  │
                        └──────────────┬──────────────┘
                                       │ /api/**
                                       ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend Service (:8080)                      │
│                                                                            │
│  [RateLimitFilter] ──► [JwtAuthFilter] ──► [Controllers] ──► [Services]    │
│                                                  │                │        │
│                                      (@Async Email / @Scheduled)  │        │
└──────────────────────────────────────────────────┼────────────────┼────────┘
                                                   │                │
                         ┌─────────────────────────┘                ▼
                         ▼                               ┌─────────────────────┐
          ┌─────────────────────────────┐                │   PostgreSQL 16     │
          │          Redis 7            │                │ (Primary Database & │
          │ • Redisson Distributed Lock │                │  Flyway Migrations) │
          │ • JWT Access Token Blacklist│                └─────────────────────┘
          │ • Brute-force Login Counter │
          └─────────────────────────────┘
```

---

## 🛠️ Công Nghệ Sử Dụng

| Thành phần | Công nghệ / Thư viện | Vai trò trong hệ thống |
| :--- | :--- | :--- |
| **Backend** | Java 21, Spring Boot, Spring Security | Xây dựng RESTful API, phân quyền RBAC (`ADMIN`, `STAFF`, `CUSTOMER`) |
| **Database** | PostgreSQL 16, Spring Data JPA, Flyway | Lưu trữ dữ liệu quan hệ & quản lý phiên bản schema tự động (`V1`, `V2`) |
| **Cache & Lock** | Redis 7, Redisson, StringRedisTemplate | Distributed Lock chống trùng lịch, JWT Blacklist, khóa đăng nhập sai 5 lần |
| **Rate Limiting** | Bucket4j | Giới hạn tần suất request theo IP chống spam & tấn công Brute-force |
| **Async & Jobs** | Spring `@Async`, `@Scheduled`, JavaMailSender | Gửi email HTML xác nhận bất đồng bộ & Cron Job nhắc lịch trước 2 giờ |
| **Frontend** | Angular (Standalone Architecture), SCSS, RxJS | Giao diện đặt lịch, dashboard quản trị, interceptor tự động refresh token |
| **DevOps** | Docker, Docker Compose, Nginx | Đóng gói đa container, reverse proxy và tự động hóa triển khai |

---

## ✨ Điểm Nhấn Kỹ Thuật (Backend Highlights)

1. **Chống trùng lịch tuyệt đối (Race Condition Prevention):**
   - Áp dụng **Redis Distributed Lock (`RedissonClient`)** theo khóa `lock:booking:staff:{staffId}:date:{date}` trên cả luồng khách tự đặt lịch (`createAppointment`) và Admin phân công (`assignStaff`).
   - Sử dụng `TransactionTemplate` nằm **bên trong vùng khóa** thay vì `@Transactional` bọc ngoài nhằm đảm bảo PostgreSQL đã `COMMIT` dữ liệu hoàn tất trước khi giải phóng khóa Redis.
2. **Cơ chế xác thực & thu hồi Token tức thì:**
   - Kết hợp **Access Token (JWT - 15 phút)** và **Refresh Token (PostgreSQL - 7 ngày)**.
   - Khi người dùng đăng xuất (`/api/auth/logout`), hệ thống đồng thời đưa Access Token vào **Redis Blacklist** (với TTL bằng đúng thời gian sống còn lại của token) và đánh dấu `revoked = true` cho Refresh Token trong Database.
   - Tích hợp khóa tài khoản tạm thời 10 phút trên Redis nếu nhập sai mật khẩu 5 lần liên tiếp.
3. **Bảo vệ API với Rate Limiting (`Bucket4j`):**
   - Bộ lọc `RateLimitFilter` đứng ở tầng ưu tiên cao nhất (`HIGHEST_PRECEDENCE`), đọc đúng IP khách hàng qua header `X-Forwarded-For` và giới hạn tối đa **10 requests/phút** cho các endpoint nhạy cảm (`/login`, `/register`, `POST /appointments`).
4. **Xử lý bất đồng bộ & Tác vụ định kỳ:**
   - Gửi email HTML xác nhận đặt lịch và thông báo thay đổi trạng thái qua luồng `@Async` tách biệt, giúp API phản hồi tức thì mà không phải chờ máy chủ SMTP.
   - `AppointmentReminderScheduler` chạy định kỳ mỗi 15 phút, kết hợp `JOIN FETCH` và cờ `is_reminded` để nhắc lịch khách hàng trước 2 tiếng mà không gây lỗi `LazyInitializationException` hay gửi trùng lặp.

---

## 📂 Cấu Trúc Dự Án

```text
spa-managerment/
├── docker-compose.yml              # Khởi chạy toàn bộ stack (PostgreSQL, Redis, Backend, Frontend)
├── .env                            # Quản lý biến môi trường tập trung
│
├── spa-booking-service/            # Backend (Spring Boot)
│   └── src/main/
│       ├── java/com/spa/spabookingservice/
│       │   ├── config/             # Cấu hình Security, CORS, RedissonClient
│       │   ├── controller/         # REST API Endpoints
│       │   ├── dto/                # Request & Response DTOs
│       │   ├── entity/             # JPA Entities
│       │   ├── exception/          # GlobalExceptionHandler & ErrorCode chuẩn hóa
│       │   ├── repository/         # Spring Data JPA Repositories
│       │   ├── scheduler/          # Background Cron Jobs (Nhắc lịch tự động)
│       │   ├── security/           # JwtAuthenticationFilter, RateLimitFilter, JwtTokenProvider
│       │   ├── service/            # Business Logic & EmailNotificationService (@Async)
│       │   └── util/               # Constants, DateUtils, SecurityUtils, TraceIdFilter
│       └── resources/
│           ├── db/migration/       # Flyway SQL Scripts (V1__init_schema.sql, V2__add_reminder_flag.sql)
│           └── application.yml     # Cấu hình ứng dụng đọc từ .env
│
└── spa-booking-web/                # Frontend (Angular)
    └── src/app/
        ├── core/                   # Singleton services, guards, interceptors & models
        │   ├── guards/             # Bảo vệ route theo trạng thái đăng nhập & vai trò (Auth/Role Guards)
        │   ├── interceptors/       # Tự động đính kèm Bearer Token & xử lý Refresh Token khi gặp 401
        │   ├── models/             # TypeScript Interfaces & DTO Models
        │   └── services/           # API Services (AuthService, AppointmentService, TokenService...)
        ├── environments/           # Cấu hình biến môi trường (environment.ts, environment.prod.ts)
        ├── features/               # Các module chức năng độc lập theo nghiệp vụ
        │   ├── admin/              # Dashboard quản trị: duyệt lịch, gán KTV, quản lý dịch vụ/chi nhánh
        │   ├── auth/               # Đăng nhập, đăng ký tài khoản
        │   ├── booking/            # Luồng đặt lịch trực tuyến (chọn chi nhánh, dịch vụ, KTV, khung giờ)
        │   ├── branches/           # Danh sách & thông tin chi tiết các chi nhánh Spa
        │   ├── home/               # Trang chủ giới thiệu dịch vụ nổi bật
        │   ├── my-bookings/        # Lịch sử đặt hẹn & hủy lịch trước 24h dành cho khách hàng
        │   └── services/           # Danh mục dịch vụ chăm sóc & bảng giá
        ├── shared/                 # Các thành phần giao diện dùng chung
        │   └── components/
        │       ├── navbar/         # Thanh điều hướng chính
        │       └── sidebar/        # Thanh menu bên cho khu vực quản trị & người dùng
        ├── app.config.ts           # Cấu hình Providers, HttpClient & Interceptors
        ├── app.routes.ts           # Định tuyến toàn ứng dụng (Lazy loading)
        └── app.html / app.scss     # Layout gốc
```

---

## 🚀 Hướng Dẫn Cài Đặt & Chạy Dự Án

### Yêu cầu hệ thống
- **Docker** & **Docker Compose** (khuyên dùng để chạy toàn bộ hệ thống chỉ với 1 lệnh)
- Hoặc **JDK 21**, **Node.js 20+**, **PostgreSQL 16**, **Redis 7** (nếu chạy trực tiếp trên máy thật)

### Bước 1: Clone repository
```bash
git clone https://github.com/your-username/spa-managerment.git
cd spa-managerment
```

### Bước 2: Cấu hình biến môi trường (`.env`)
Tạo file `.env` tại thư mục gốc `spa-managerment/` (ngang hàng với `docker-compose.yml`) và điền các thông số sau:

```env
DOCKERHUB_USERNAME=dat170604
TZ=Asia/Ho_Chi_Minh
SERVER_PORT=8080

# PostgreSQL
DB_NAME=spa_booking_db
DB_URL=jdbc:postgresql://localhost:5432/spa_booking_db
DB_USERNAME=postgres
DB_PASSWORD=dat170604

# Redis
REDIS_HOST=localhost
REDIS_PORT=6379

# Tài khoản Admin khởi tạo mặc định
ADMIN_FULL_NAME=Administrator
ADMIN_EMAIL=admin@spa.com
ADMIN_PHONE=0999999999
ADMIN_PASSWORD=admin@123

# JWT Security
JWT_SECRET=404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970
JWT_EXPIRATION_MS=900000
REFRESH_TOKEN_EXPIRATION_MS=604800000

# SMTP Email (Dùng App Password 16 ký tự của Gmail)
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your_email@gmail.com
MAIL_PASSWORD=your_16_char_app_password

# CORS
ALLOWED_ORIGINS=http://localhost:4200
```

### Bước 3: Khởi chạy bằng Docker Compose
```bash
docker compose up -d --build
```

Sau khi các container chuyển sang trạng thái `healthy`, truy cập các địa chỉ sau:
- **Giao diện người dùng (Angular):** `http://localhost:4200`
- **Tài liệu API (Swagger UI):** `http://localhost:8080/swagger-ui.html`
- **Kiểm tra sức khỏe hệ thống (Actuator):** `http://localhost:8080/actuator/health`

---

## 🧪 Kiểm Thử

Chạy bộ kiểm thử đơn vị và kiểm thử đồng thời (Concurrency Test) cho Backend:
```bash
cd spa-booking-service
./gradlew test
```