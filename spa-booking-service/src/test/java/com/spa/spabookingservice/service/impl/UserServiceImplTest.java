package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.user.StaffCreateRequest;
import com.spa.spabookingservice.dto.user.UserResponse;
import com.spa.spabookingservice.entity.BranchEntity;
import com.spa.spabookingservice.entity.RoleEntity;
import com.spa.spabookingservice.entity.UserEntity;
import com.spa.spabookingservice.exception.AppException;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.repository.BranchRepository;
import com.spa.spabookingservice.repository.RoleRepository;
import com.spa.spabookingservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock private UserRepository userRepository;
    @Mock private RoleRepository roleRepository;
    @Mock private BranchRepository branchRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    @DisplayName("1. Tạo tài khoản Kỹ thuật viên thành công: Gán chi nhánh và cả 2 role STAFF + CUSTOMER")
    void createStaff_Success() {
        StaffCreateRequest req = new StaffCreateRequest();
        req.setFullName("KTV Mai");
        req.setEmail("mai@spa.com");
        req.setPhone("0988888888");
        req.setPassword("123456");
        req.setBranchId(1L);

        BranchEntity branch = BranchEntity.builder().id(1L).name("Chi nhánh Q1").build();
        RoleEntity staffRole = RoleEntity.builder().name("ROLE_STAFF").build();
        RoleEntity customerRole = RoleEntity.builder().name("ROLE_CUSTOMER").build();

        when(userRepository.existsByEmail("mai@spa.com")).thenReturn(false);
        when(userRepository.existsByPhone("0988888888")).thenReturn(false);
        when(branchRepository.findById(1L)).thenReturn(Optional.of(branch));
        when(roleRepository.findByName("ROLE_STAFF")).thenReturn(Optional.of(staffRole));
        when(roleRepository.findByName("ROLE_CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(userRepository.save(any(UserEntity.class))).thenAnswer(inv -> {
            UserEntity u = inv.getArgument(0);
            u.setId(20L);
            return u;
        });

        UserResponse res = userService.createStaff(req);

        assertEquals(20L, res.getId());
        assertEquals(1L, res.getBranchId());
        assertEquals(2, res.getRoles().size());
        assertTrue(res.getRoles().contains("ROLE_STAFF"));
    }

    @Test
    @DisplayName("2. Tạo KTV thất bại: Trùng số điện thoại")
    void createStaff_FailWhenDuplicatePhone() {
        StaffCreateRequest req = new StaffCreateRequest();
        req.setEmail("mai@spa.com");
        req.setPhone("0988888888");

        when(userRepository.existsByEmail("mai@spa.com")).thenReturn(false);
        when(userRepository.existsByPhone("0988888888")).thenReturn(true);

        AppException ex = assertThrows(AppException.class, () -> userService.createStaff(req));
        assertEquals(ErrorCode.CONFLICT, ex.getErrorCode());
    }
}