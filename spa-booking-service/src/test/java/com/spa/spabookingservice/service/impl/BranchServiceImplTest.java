package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.branch.BranchRequest;
import com.spa.spabookingservice.dto.branch.BranchResponse;
import com.spa.spabookingservice.entity.BranchEntity;
import com.spa.spabookingservice.exception.AppException;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.repository.BranchRepository;
import com.spa.spabookingservice.service.FileStorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BranchServiceImplTest {

    @Mock private BranchRepository branchRepository;
    @Mock private FileStorageService fileStorageService;

    @InjectMocks
    private BranchServiceImpl branchService;

    @Test
    @DisplayName("1. Tạo chi nhánh thành công")
    void createBranch_Success() {
        BranchRequest req = new BranchRequest();
        req.setName("Lotus Spa Hà Nội");
        req.setAddress("123 Cầu Giấy");
        req.setOpenTime(LocalTime.of(8, 0));
        req.setCloseTime(LocalTime.of(22, 0));
        req.setActive(true);

        when(branchRepository.existsByName("Lotus Spa Hà Nội")).thenReturn(false);
        when(branchRepository.save(any(BranchEntity.class))).thenAnswer(inv -> {
            BranchEntity b = inv.getArgument(0);
            b.setId(1L);
            return b;
        });

        BranchResponse res = branchService.createBranch(req);

        assertEquals(1L, res.getId());
        assertEquals("Lotus Spa Hà Nội", res.getName());
    }

    @Test
    @DisplayName("2. Tạo chi nhánh thất bại: Trùng tên chi nhánh")
    void createBranch_FailWhenDuplicateName() {
        BranchRequest req = new BranchRequest();
        req.setName("Lotus Spa Hà Nội");
        when(branchRepository.existsByName("Lotus Spa Hà Nội")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> branchService.createBranch(req));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }

    @Test
    @DisplayName("3. Đổi trạng thái hoạt động chi nhánh (Active <-> Inactive)")
    void toggleBranchStatus_Success() {
        BranchEntity branch = BranchEntity.builder().id(1L).active(true).build();
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));

        branchService.toggleBranchStatus(1L);

        assertFalse(branch.isActive());
        verify(branchRepository).save(branch);
    }
}