**# 🌸 Lotus Spa — Hệ Thống Quản Lý & Đặt Lịch Chăm Sóc Sức Khỏe

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
                                       │ /api/v1/**
                                       ▼
┌────────────────────────────────────────────────────────────────────────────┐
│                   Spring Boot Backend Service (:8080)                      │
│                                                                            │
│  [TraceIdFilter] ─► [RateLimitFilter] ─► [JwtAuthFilter] ─► [Controllers]  │
│                                                                   │        │
│                                      (@Async Email / @Scheduled)  │        │
└───────────────────────────────────────────────────┼────────────────┼────────┘
                                                    │                │
                         ┌──────────────────────────┘                ▼
                         ▼                                ┌─────────────────────┐
          ┌─────────────────────────────┐                 │   PostgreSQL 16     │
          │          Redis 7            │                 │ (Primary Database & │
          │ • Redisson Distributed Lock │                 │  Flyway Migrations) │
          │ • JWT Access Token Blacklist│                 └─────────────────────┘
          │ • Brute-force Login Counter │
          └─────────────────────────────┘
```

---

## 🛠️ Công Nghệ Sử Dụng

| Thành phần | Công nghệ / Thư viện | Vai trò trong hệ thống |
| :--- | :--- | :--- |
| **Backend** | Java 21+, Spring Boot, Spring Security | Xây dựng RESTful API (`/api/v1/**`), phân quyền RBAC (`ADMIN`, `STAFF`, `CUSTOMER`) |
| **Database** | PostgreSQL 16, Spring Data JPA, Flyway | Lưu trữ dữ liệu quan hệ & quản lý phiên bản schema tự động |
| **Cache & Lock** | Redis 7, Redisson, StringRedisTemplate | Distributed Lock chống trùng lịch, JWT Blacklist, khóa đăng nhập sai 5 lần |
| **Rate Limiting** | Bucket4j (`RateLimitFilter`) | Giới hạn tần suất request theo IP chống spam & tấn công Brute-force |
| **Async & Jobs** | Spring `@Async`, `@Scheduled`, JavaMailSender | Gửi email HTML bất đồng bộ, Cron Job nhắc lịch trước 2h & tự động hủy lịch quá hạn |
| **API Docs** | Springdoc OpenAPI / Swagger UI | Tài liệu hóa REST API tương tác trực tiếp kèm xác thực Bearer JWT |
| **Frontend** | Angular (Standalone Architecture), Tailwind CSS / SCSS, RxJS | Giao diện đặt lịch, dashboard quản trị, interceptor tự động refresh token |
| **DevOps & CI/CD** | Docker, Docker Compose, GitHub Actions, Nginx | Tự động chạy Unit Test, build/push Docker Image lên Docker Hub & chạy 1 lệnh |

---

## ✨ Điểm Nhấn Kỹ Thuật & Nghiệp Vụ

1. **Chống trùng lịch tuyệt đối (Race Condition Prevention):**
   - Áp dụng **Redis Distributed Lock (`RedissonClient`)** theo khóa `lock:booking:staff:{staffId}:date:{date}` trên cả luồng khách tự đặt lịch (`createAppointment`) và Admin phân công (`assignStaff`).
   - Sử dụng `TransactionTemplate` nằm **bên trong vùng khóa** thay vì `@Transactional` bọc ngoài nhằm đảm bảo PostgreSQL đã `COMMIT` dữ liệu hoàn tất trước khi giải phóng khóa Redis.
2. **Ràng buộc nghiệp vụ đặt lịch & Quản lý nghỉ phép KTV:**
   - Chặn đặt khung giờ trong quá khứ hoặc quá sát thời điểm hiện tại (yêu cầu đặt trước tối thiểu **30 phút** ở cả giao diện Angular và Backend).
   - Hệ thống quản lý đơn xin nghỉ phép của kỹ thuật viên (`PENDING` / `APPROVED` / `REJECTED`): tự động gộp khung giờ nghỉ đã duyệt vào danh sách giờ bận của KTV và khóa trực tiếp trên giao diện đặt lịch.
3. **Cơ chế xác thực & thu hồi Token tức thì:**
   - Kết hợp **Access Token (JWT - 15 phút)** và **Refresh Token (PostgreSQL - 7 ngày)**.
   - Khi người dùng đăng xuất (`/api/v1/auth/logout`), hệ thống đồng thời đưa Access Token vào **Redis Blacklist** (với TTL bằng đúng thời gian sống còn lại của token) và đánh dấu `revoked = true` cho Refresh Token trong Database.
   - Tích hợp khóa tài khoản tạm thời trên Redis nếu nhập sai mật khẩu 5 lần liên tiếp.
4. **Bảo vệ API với Rate Limiting (`Bucket4j`) & Truy vết (`TraceIdFilter`):**
   - Bộ lọc `RateLimitFilter` đứng ở tầng ưu tiên cao nhất (`HIGHEST_PRECEDENCE`), đọc đúng IP khách hàng qua header `X-Forwarded-For` và giới hạn tối đa **10 requests/phút** cho các endpoint nhạy cảm (`/login`, `/register`, `POST /appointments`).
   - `TraceIdFilter` gắn mã định danh duy nhất cho mỗi HTTP Request để dễ dàng truy vết log hệ thống.
5. **Xử lý bất đồng bộ & Tác vụ định kỳ (`@Scheduled`):**
   - Gửi email HTML xác nhận đặt lịch và thông báo thay đổi trạng thái qua luồng `@Async` tách biệt, giúp API phản hồi tức thì.
   - `AppointmentReminderScheduler` chạy định kỳ mỗi 15 phút để nhắc lịch khách hàng trước 2 tiếng (`JOIN FETCH` tránh `LazyInitializationException`), đồng thời tự động dọn dẹp và hủy các lịch hẹn quá giờ vào cuối ngày (`23:30`).

---

## 📂 Cấu Trúc Dự Án

```text
spa-booking/
├── .github/workflows/              # Pipeline CI/CD (Chạy Unit Test & Push Docker Hub)
├── docker-compose.yml              # Khởi chạy toàn bộ stack chỉ với 1 lệnh
├── .env.example                    # File biến môi trường mẫu (tùy chọn cấu hình Mail/Cloudinary)
├── .gitignore                      # Quy tắc ignore chung cho toàn bộ dự án
│
├── spa-booking-service/            # Backend (Spring Boot)
│   └── src/
│       ├── main/
│       │   ├── java/com/spa/spabookingservice/
│       │   │   ├── config/         # SecurityConfig, OpenApiConfig (Swagger), RedissonConfig
│       │   │   ├── controller/     # REST API Endpoints (/api/v1/**)
│       │   │   ├── dto/            # Request & Response DTOs
│       │   │   ├── entity/         # JPA Entities
│       │   │   ├── exception/      # GlobalExceptionHandler chuẩn hóa lỗi
│       │   │   ├── repository/     # Spring Data JPA Repositories
│       │   │   ├── scheduler/      # Background Cron Jobs (Nhắc lịch & hủy lịch quá hạn)
│       │   │   ├── security/       # RateLimitFilter, JwtAuthenticationFilter, TraceIdFilter
│       │   │   └── service/        # Business Logic & EmailNotificationService (@Async)
│       │   └── resources/
│       │       ├── db/migration/   # Flyway SQL Migration Scripts
│       │       └── application.yml # Cấu hình ứng dụng
│       └── test/                   # Unit Tests (Mockito) & Concurrency Integration Tests
│
└── spa-booking-web/                # Frontend (Angular Standalone)
    └── src/app/
        ├── core/                   # Singleton services, guards, interceptors & models
        │   ├── guards/             # Bảo vệ route theo trạng thái đăng nhập & vai trò
        │   ├── interceptors/       # Tự động đính kèm Bearer Token & Refresh Token khi gặp 401
        │   ├── models/             # TypeScript Interfaces & DTO Models
        │   └── services/           # API Services
        ├── environments/           # Cấu hình môi trường (environment.ts, environment.prod.ts)
        ├── features/               # Các module chức năng độc lập theo nghiệp vụ
        │   ├── admin/              # Dashboard quản trị, duyệt lịch, gán KTV, quản lý lịch làm việc
        │   ├── auth/               # Đăng nhập, đăng ký tài khoản
        │   ├── booking/            # Luồng đặt lịch trực tuyến (chọn chi nhánh, dịch vụ, KTV, khung giờ)
        │   ├── branches/           # Danh sách & thông tin chi tiết các chi nhánh Spa
        │   ├── home/               # Trang chủ giới thiệu dịch vụ nổi bật
        │   ├── my-bookings/        # Lịch sử đặt hẹn & hủy lịch dành cho khách hàng
        │   └── services/           # Danh mục dịch vụ chăm sóc & bảng giá
        └── shared/                 # Các thành phần giao diện dùng chung (Navbar, Sidebar...)
```

---

## 🚀 Khởi Chạy Nhanh Với 1 Lệnh (Docker)

Hệ thống đã được đóng gói sẵn lên **Docker Hub** (`dat170604/lotus-spa-backend` & `dat170604/lotus-spa-frontend`) kèm đầy đủ giá trị cấu hình mặc định. Bạn không cần cài đặt Java, Node.js hay tạo file `.env` thủ công.

### Yêu cầu
- Đã cài đặt **Docker** & **Docker Compose**

### Chạy ứng dụng
```bash
git clone [https://github.com/dat170604/spa-booking.git](https://github.com/dat170604/spa-booking.git)
cd spa-booking
docker compose up -d
```

Sau khoảng 30 giây để PostgreSQL, Redis và Backend khởi tạo hoàn tất, truy cập trực tiếp:
- **🌐 Giao diện Web (Angular):** `http://localhost:4200`
- **📘 Tài liệu API (Swagger UI):** `http://localhost:8080/swagger-ui.html`
- **💓 Trạng thái hệ thống (Actuator):** `http://localhost:8080/actuator/health`

### 🔑 Tài khoản trải nghiệm mặc định
| Vai trò | Email | Mật khẩu |
| :--- | :--- | :--- |
| **Quản trị viên (Admin)** | `admin@spa.com` | `admin@123` |

*(Tùy chọn: Nếu muốn kích hoạt tính năng gửi Email nhắc lịch hoặc upload ảnh lên Cloudinary, hãy sao chép file `.env.example` thành `.env`, điền thông tin SMTP/Cloudinary của bạn và chạy lại `docker compose up -d`).*

---

## 🧪 Kiểm Thử

Chạy bộ kiểm thử đơn vị (Unit Tests) cho Backend:
```bash
cd spa-booking-service
./gradlew test
```

Chạy kiểm thử tích hợp & xử lý đồng thời (Concurrency Tests — yêu cầu bật sẵn PostgreSQL & Redis):
```bash
cd spa-booking-service
./gradlew integrationTest
```**