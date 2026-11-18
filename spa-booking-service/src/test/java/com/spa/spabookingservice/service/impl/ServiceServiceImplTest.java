package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.service.ServiceRequest;
import com.spa.spabookingservice.dto.service.ServiceResponse;
import com.spa.spabookingservice.entity.ServiceEntity;
import com.spa.spabookingservice.exception.AppException;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.repository.ServiceRepository;
import com.spa.spabookingservice.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceServiceImplTest {

    @Mock private ServiceRepository serviceRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private ServiceServiceImpl serviceService;

    @Test
    @DisplayName("1. Tạo dịch vụ mới thành công với trạng thái mặc định ACTIVE")
    void createService_DefaultActiveStatus() {
        ServiceRequest req = new ServiceRequest();
        req.setName("Gội đầu dưỡng sinh");
        req.setCategory("HAIR");
        req.setPrice(new BigDecimal("250000"));
        req.setDurationMinutes(45);

        when(serviceRepository.save(any(ServiceEntity.class))).thenAnswer(inv -> {
            ServiceEntity s = inv.getArgument(0);
            s.setId(5L);
            return s;
        });

        ServiceResponse res = serviceService.createService(req);

        assertEquals(5L, res.getId());
        assertEquals("ACTIVE", res.getStatus());
    }

    @Test
    @DisplayName("2. Đổi trạng thái dịch vụ từ ACTIVE sang INACTIVE")
    void toggleServiceStatus_Success() {
        ServiceEntity entity = ServiceEntity.builder().id(5L).status("ACTIVE").build();
        when(serviceRepository.findById(5L)).thenReturn(Optional.of(entity));

        serviceService.toggleServiceStatus(5L);

        assertEquals("INACTIVE", entity.getStatus());
        verify(serviceRepository).save(entity);
    }

    @Test
    @DisplayName("3. Lấy chi tiết dịch vụ thất bại khi ID không tồn tại")
    void getServiceById_NotFound() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> serviceService.getServiceById(99L));
        assertEquals(ErrorCode.NOT_FOUND, ex.getErrorCode());
    }
}