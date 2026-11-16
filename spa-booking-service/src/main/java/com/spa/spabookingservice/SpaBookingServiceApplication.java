package com.spa.spabookingservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync       // Cho phép gửi email chạy ngầm, không bắt user chờ
@EnableScheduling  // Cho phép chạy tác vụ quét nhắc lịch định kỳ
public class SpaBookingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpaBookingServiceApplication.class, args);
    }

}
