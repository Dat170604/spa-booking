package com.spa.spabookingservice.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.spa.spabookingservice.dto.user.*;
import com.spa.spabookingservice.entity.BranchEntity;
import com.spa.spabookingservice.entity.RefreshTokenEntity;
import com.spa.spabookingservice.entity.RoleEntity;
import com.spa.spabookingservice.entity.UserEntity;
import com.spa.spabookingservice.exception.AppException;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.repository.BranchRepository;
import com.spa.spabookingservice.repository.RoleRepository;
import com.spa.spabookingservice.repository.UserRepository;
import com.spa.spabookingservice.security.JwtTokenProvider;
import com.spa.spabookingservice.service.RefreshTokenService;
import com.spa.spabookingservice.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final BranchRepository branchRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final Cloudinary cloudinary;
    private final RefreshTokenService refreshTokenService;

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

        return mapToResponse(updatedUser);
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

    // ================== THÔNG TIN CÁ NHÂN (MY PROFILE) ==================

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(String email) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy thông tin người dùng"));
        return mapToResponse(user);
    }

    @Override
    @Transactional
    public Map<String, Object> updateMyProfile(String currentEmail, UpdateProfileRequest request) {
        UserEntity user = userRepository.findByEmail(currentEmail)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy thông tin người dùng"));

        String newEmail = request.getEmail().trim().toLowerCase();
        boolean emailChanged = !user.getEmail().equalsIgnoreCase(newEmail);

        if (emailChanged && userRepository.existsByEmail(newEmail)) {
            throw new AppException(ErrorCode.CONFLICT, "Email này đã được sử dụng bởi tài khoản khác!");
        }

        String newPhone = (request.getPhone() != null && !request.getPhone().isBlank())
                ? request.getPhone().trim()
                : null;
        if (newPhone != null && !newPhone.equals(user.getPhone()) && userRepository.existsByPhone(newPhone)) {
            throw new AppException(ErrorCode.CONFLICT, "Số điện thoại này đã được sử dụng bởi tài khoản khác!");
        }

        user.setFullName(request.getFullName().trim());
        user.setEmail(newEmail);
        user.setPhone(newPhone);

        UserEntity saved = userRepository.save(user);

        Map<String, Object> result = new HashMap<>();
        result.put("profile", mapToResponse(saved));
        result.put("emailChanged", emailChanged);

        // Nếu đổi email thì cấp lại cặp token mới để phiên đăng nhập không bị gián đoạn
        if (emailChanged) {
            refreshTokenService.deleteByUserId(saved.getId());
            RefreshTokenEntity newRefreshToken = refreshTokenService.createRefreshToken(saved.getId());

            result.put("accessToken", jwtTokenProvider.generateAccessToken(saved.getEmail()));
            result.put("refreshToken", newRefreshToken.getToken());
        }

        return result;
    }

    @Override
    @Transactional
    public void changePassword(String email, ChangePasswordRequest request) {
        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy thông tin người dùng"));

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mật khẩu hiện tại không chính xác!");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Mật khẩu mới không được trùng với mật khẩu cũ!");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    @Override
    @Transactional
    public UserResponse uploadAvatar(String email, MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Vui lòng chọn ảnh để tải lên");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new AppException(ErrorCode.BAD_REQUEST, "Chỉ chấp nhận tệp định dạng hình ảnh (JPG, PNG, WEBP)");
        }

        UserEntity user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy thông tin người dùng"));

        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap(
                        "folder", "lotus-spa/avatars",
                        "public_id", "user_" + user.getId(),
                        "overwrite", true,
                        "resource_type", "image"
                )
        );

        String secureUrl = uploadResult.get("secure_url").toString();
        user.setAvatarUrl(secureUrl);

        return mapToResponse(userRepository.save(user));
    }

    private UserResponse mapToResponse(UserEntity user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .avatarUrl(user.getAvatarUrl())
                .branchId(user.getBranch() != null ? user.getBranch().getId() : null)
                .branchName(user.getBranch() != null ? user.getBranch().getName() : null)
                .branchAddress(user.getBranch() != null ? user.getBranch().getAddress() : null)
                .active("ACTIVE".equalsIgnoreCase(user.getStatus()))
                .roles(user.getRoles().stream().map(RoleEntity::getName).toList())
                .createdAt(user.getCreatedAt() != null ? user.getCreatedAt().toLocalDateTime() : null)
                .build();
    }
}