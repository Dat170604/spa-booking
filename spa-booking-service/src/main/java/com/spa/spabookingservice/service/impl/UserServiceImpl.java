package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.user.StaffCreateRequest;
import com.spa.spabookingservice.dto.user.UpdateUserRolesRequest;
import com.spa.spabookingservice.dto.user.UserResponse;
import com.spa.spabookingservice.dto.user.UserUpdateRequest;
import com.spa.spabookingservice.entity.BranchEntity;
import com.spa.spabookingservice.entity.RoleEntity;
import com.spa.spabookingservice.entity.UserEntity;
import com.spa.spabookingservice.exception.AppException;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.repository.BranchRepository;
import com.spa.spabookingservice.repository.RoleRepository;
import com.spa.spabookingservice.repository.UserRepository;
import com.spa.spabookingservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public UserResponse createStaff(StaffCreateRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.CONFLICT, "Email đã được sử dụng");
        }
        if (userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.CONFLICT, "Số điện thoại đã được sử dụng");
        }

        BranchEntity branch = branchRepository.findById(request.getBranchId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chi nhánh"));

        RoleEntity staffRole = roleRepository.findByName("ROLE_STAFF")
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Role ROLE_STAFF không tồn tại"));
        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Role ROLE_CUSTOMER không tồn tại"));

        UserEntity user = UserEntity.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .status("ACTIVE")
                .branch(branch)
                .roles(Set.of(staffRole, customerRole))
                .build();

        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateUser(Long id, UserUpdateRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy người dùng"));

        if (!user.getEmail().equalsIgnoreCase(request.getEmail()) && userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.CONFLICT, "Email đã được sử dụng bởi tài khoản khác");
        }

        if (!user.getPhone().equals(request.getPhone()) && userRepository.existsByPhone(request.getPhone())) {
            throw new AppException(ErrorCode.CONFLICT, "Số điện thoại đã được sử dụng bởi tài khoản khác");
        }

        user.setFullName(request.getFullName());
        user.setEmail(request.getEmail());
        user.setPhone(request.getPhone());

        if (request.getPassword() != null && !request.getPassword().trim().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateUserRoles(Long id, UpdateUserRolesRequest request) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy người dùng"));

        Set<RoleEntity> roles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            RoleEntity role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy role: " + roleName));
            roles.add(role);
        }

        user.setRoles(roles);
        return mapToResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public UserResponse updateUserBranch(Long userId, Long branchId) {
        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND, "Không tìm thấy người dùng"));

        BranchEntity branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chi nhánh"));

        user.setBranch(branch);
        UserEntity updatedUser = userRepository.save(user);

        return mapToResponse(updatedUser); // Phương thức map Entity -> DTO của bạn
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy người dùng"));
        return mapToResponse(user);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserResponse> searchUsers(String keyword, String roleName, Pageable pageable) {
        return userRepository.searchUsers(keyword, roleName, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponse> getStaffByBranch(Long branchId) {
        return userRepository.findActiveStaffByBranchId(branchId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy người dùng"));

        user.setStatus("ACTIVE".equalsIgnoreCase(user.getStatus()) ? "INACTIVE" : "ACTIVE");
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        UserEntity user = userRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy người dùng"));
        userRepository.delete(user);
    }

    private UserResponse mapToResponse(UserEntity entity) {
        Set<String> roles = entity.getRoles().stream()
                .map(RoleEntity::getName)
                .collect(Collectors.toSet());

        return UserResponse.builder()
                .id(entity.getId())
                .fullName(entity.getFullName())
                .email(entity.getEmail())
                .phone(entity.getPhone())
                .status(entity.getStatus())
                .branchId(entity.getBranch() != null ? entity.getBranch().getId() : null)
                .branchName(entity.getBranch() != null ? entity.getBranch().getName() : null)
                .roles(roles)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}