package com.spa.spabookingservice.service;

import com.spa.spabookingservice.dto.user.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;

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

    UserResponse getMyProfile(String email);

    Map<String, Object> updateMyProfile(String currentEmail, UpdateProfileRequest request);

    void changePassword(String email, ChangePasswordRequest request);

    UserResponse uploadAvatar(String email, MultipartFile file) throws IOException;
}