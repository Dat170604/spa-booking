package com.spa.spabookingservice.service;

import com.spa.spabookingservice.dto.appointment.AppointmentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class AppointmentConcurrencyTest {

    @Autowired
    private AppointmentService appointmentService;

    @Test
    void shouldAllowOnlyOneBookingWhenConcurrentRequestsArrive() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);

        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        AppointmentRequest request = new AppointmentRequest();
        request.setBranchId(1L);
        request.setServiceId(1L);
        request.setStaffId(2L);
        request.setAppointmentDate(LocalDate.now().plusDays(2));
        request.setStartTime(LocalTime.of(10, 0));

        for (int i = 0; i < threadCount; i++) {
            final String userEmail = "customer" + i + "@spa.com"; // Đảm bảo DB test có sẵn các user này
            executor.submit(() -> {
                try {
                    appointmentService.createAppointment(userEmail, request);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        // Kỳ vọng: Đúng 1 đơn thành công, 9 đơn bị chặn bởi Redisson Lock / Conflict check
        assertEquals(1, successCount.get());
        assertEquals(9, failCount.get());
    }
}