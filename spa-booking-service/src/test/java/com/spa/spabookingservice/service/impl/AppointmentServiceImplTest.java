package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.appointment.AppointmentRequest;
import com.spa.spabookingservice.dto.appointment.AppointmentResponse;
import com.spa.spabookingservice.entity.*;
import com.spa.spabookingservice.repository.*;
import com.spa.spabookingservice.service.EmailNotificationService;
import com.spa.spabookingservice.service.impl.AppointmentServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceImplTest {

    @Mock private AppointmentRepository appointmentRepository;
    @Mock private UserRepository userRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private ServiceRepository serviceRepository;
    @Mock private RedissonClient redissonClient;
    @Mock private TransactionTemplate transactionTemplate;
    @Mock private EmailNotificationService emailNotificationService;
    @Mock private RLock rLock;

    @InjectMocks
    private AppointmentServiceImpl appointmentService;

    private UserEntity customer;
    private UserEntity staff;
    private BranchEntity branch;
    private ServiceEntity service;

    @BeforeEach
    void setUp() {
        customer = UserEntity.builder().id(1L).fullName("Khách A").email("khach@spa.com").phone("0901111111").build();
        staff = UserEntity.builder().id(2L).fullName("KTV Lan").email("lan@spa.com").build();
        branch = BranchEntity.builder().id(10L).name("Chi nhánh Q1").openTime(LocalTime.of(8, 0)).closeTime(LocalTime.of(21, 0)).active(true).build();
        service = ServiceEntity.builder().id(100L).name("Massage đá nóng").price(new BigDecimal("500000")).durationMinutes(60).build();
    }

    private void mockLockAndTx() throws InterruptedException {
        when(redissonClient.getLock(anyString())).thenReturn(rLock);
        when(rLock.tryLock(5, 10, TimeUnit.SECONDS)).thenReturn(true);
        when(rLock.isHeldByCurrentThread()).thenReturn(true);
        when(transactionTemplate.execute(any())).thenAnswer(inv -> {
            TransactionCallback<?> cb = inv.getArgument(0);
            return cb.doInTransaction(null);
        });
    }

    @Test
    @DisplayName("1. Đặt lịch thành công: Tính giờ kết thúc, tổng tiền, gửi email & nhả khóa Redis")
    void createAppointment_Success() throws InterruptedException {
        mockLockAndTx();
        AppointmentRequest req = new AppointmentRequest();
        req.setBranchId(10L);
        req.setServiceId(100L);
        req.setStaffId(2L);
        req.setAppointmentDate(LocalDate.now().plusDays(3));
        req.setStartTime(LocalTime.of(10, 0));

        when(userRepository.findByEmail("khach@spa.com")).thenReturn(Optional.of(customer));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
        when(serviceRepository.findAllById(List.of(100L))).thenReturn(List.of(service));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(appointmentRepository.existsStaffConflict(eq(2L), any(), eq(LocalTime.of(10, 0)), eq(LocalTime.of(11, 0)))).thenReturn(false);
        when(appointmentRepository.save(any())).thenAnswer(inv -> {
            AppointmentEntity a = inv.getArgument(0);
            a.setId(88L);
            a.setCreatedAt(OffsetDateTime.now());
            return a;
        });

        AppointmentResponse res = appointmentService.createAppointment("khach@spa.com", req);

        assertEquals(88L, res.getId());
        assertEquals(LocalTime.of(11, 0), res.getEndTime());
        assertEquals(new BigDecimal("500000"), res.getTotalAmount());
        verify(emailNotificationService).sendBookingCreatedEmail(any());
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("2. Đặt lịch thất bại: Vượt quá giờ đóng cửa chi nhánh")
    void createAppointment_FailWhenOutsideCloseTime() throws InterruptedException {
        mockLockAndTx();
        AppointmentRequest req = new AppointmentRequest();
        req.setBranchId(10L);
        req.setServiceId(100L);
        req.setAppointmentDate(LocalDate.now().plusDays(3));
        req.setStartTime(LocalTime.of(20, 30)); // + 60p = 21:30 > 21:00

        when(userRepository.findByEmail("khach@spa.com")).thenReturn(Optional.of(customer));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
        when(serviceRepository.findAllById(List.of(100L))).thenReturn(List.of(service));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> appointmentService.createAppointment("khach@spa.com", req));

        assertTrue(ex.getMessage().contains("vượt quá giờ đóng cửa"));
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("3. Hủy lịch thất bại: Khách hủy sát giờ (dưới 24 tiếng)")
    void cancelAppointment_FailWithin24Hours() {
        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(50L)
                .customer(customer)
                .status("CONFIRMED")
                .appointmentDate(LocalDate.now())
                .startTime(LocalTime.now().plusHours(2))
                .build();

        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(appointment));

        assertThrows(IllegalStateException.class,
                () -> appointmentService.cancelAppointment("khach@spa.com", 50L, "Bận việc"));
    }

    @Test
    @DisplayName("4. Admin phân công KTV (assignStaff): Khóa đúng lockKey, gán KTV và nhả khóa")
    void assignStaff_SuccessWithDistributedLock() throws InterruptedException {
        mockLockAndTx();
        LocalDate date = LocalDate.now().plusDays(2);
        AppointmentDetailEntity detail = AppointmentDetailEntity.builder().id(1L).service(service).price(service.getPrice()).build();
        AppointmentEntity appointment = AppointmentEntity.builder()
                .id(77L)
                .customer(customer)
                .branch(branch)
                .appointmentDate(date)
                .startTime(LocalTime.of(14, 0))
                .endTime(LocalTime.of(15, 0))
                .status("PENDING")
                .details(List.of(detail))
                .createdAt(OffsetDateTime.now())
                .build();

        when(appointmentRepository.findById(77L)).thenReturn(Optional.of(appointment));
        when(userRepository.findById(2L)).thenReturn(Optional.of(staff));
        when(appointmentRepository.existsStaffConflict(2L, date, LocalTime.of(14, 0), LocalTime.of(15, 0))).thenReturn(false);
        when(appointmentRepository.save(any())).thenReturn(appointment);

        AppointmentResponse res = appointmentService.assignStaff(77L, 2L);

        assertEquals(2L, res.getStaffId());
        verify(redissonClient).getLock("lock:booking:staff:2:date:" + date);
        verify(rLock).unlock();
    }

    @Test
    @DisplayName("5. Đặt lịch thất bại: Thời gian đặt nằm trong quá khứ hoặc dưới 30 phút tới")
    void createAppointment_FailWhenTimeInPast() throws InterruptedException {
        mockLockAndTx();
        AppointmentRequest req = new AppointmentRequest();
        req.setBranchId(10L);
        req.setServiceId(100L);
        req.setAppointmentDate(LocalDate.now());
        req.setStartTime(LocalTime.now().minusMinutes(10)); // Giờ đã qua

        when(userRepository.findByEmail("khach@spa.com")).thenReturn(Optional.of(customer));
        when(branchRepository.findById(10L)).thenReturn(Optional.of(branch));
        when(serviceRepository.findAllById(List.of(100L))).thenReturn(List.of(service));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> appointmentService.createAppointment("khach@spa.com", req));

        assertTrue(ex.getMessage().contains("ít nhất 30 phút"));
        verify(rLock).unlock();
    }
}