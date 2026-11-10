package com.spa.spabookingservice.service;

import com.spa.spabookingservice.dto.user.StaffCreateRequest;
import com.spa.spabookingservice.dto.user.UpdateUserRolesRequest;
import com.spa.spabookingservice.dto.user.UserResponse;
import com.spa.spabookingservice.dto.user.UserUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface UserService {

    UserResponse createStaff(StaffCreateRequest request);

    UserResponse updateUser(Long id, UserUpdateRequest request);

    UserResponse updateUserRoles(Long id, UpdateUserRolesRequest request);

    UserResponse updateUserBranch(Long userId, Long branchId);

    UserResponse getUserById(Long id);

    Page<UserResponse> searchUsers(String keyword, String roleName, Pageable pageable);

    List<UserResponse> getStaffByBranch(Long branchId);

    void toggleUserStatus(Long id);

    void deleteUser(Long id);
}