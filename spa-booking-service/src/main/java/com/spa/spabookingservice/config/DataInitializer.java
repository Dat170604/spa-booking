package com.spa.spabookingservice.config;

import com.spa.spabookingservice.entity.RoleEntity;
import com.spa.spabookingservice.entity.UserEntity;
import com.spa.spabookingservice.repository.RoleRepository;
import com.spa.spabookingservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.admin.email}")
    private String adminEmail;

    @Value("${app.admin.password}")
    private String adminPassword;

    @Value("${app.admin.full-name}")
    private String adminFullName;

    @Value("${app.admin.phone}")
    private String adminPhone;

    @Override
    @Transactional
    public void run(@NonNull ApplicationArguments args) throws Exception {
        // 1. Tạo các Role mặc định nếu chưa tồn tại
        RoleEntity adminRole = roleRepository.findByName("ROLE_ADMIN")
                .orElseGet(() -> roleRepository.save(
                        RoleEntity.builder().name("ROLE_ADMIN").build()
                ));

        RoleEntity customerRole = roleRepository.findByName("ROLE_CUSTOMER")
                .orElseGet(() -> roleRepository.save(
                        RoleEntity.builder().name("ROLE_CUSTOMER").build()
                ));

        // 2. Tạo tài khoản Admin từ cấu hình trong YML
        if (!userRepository.existsByEmail(adminEmail)) {
            UserEntity adminUser = UserEntity.builder()
                    .fullName(adminFullName)
                    .email(adminEmail)
                    .phone(adminPhone)
                    .password(passwordEncoder.encode(adminPassword))
                    .status("ACTIVE")
                    .roles(Set.of(adminRole, customerRole))
                    .build();

            userRepository.save(adminUser);
            log.info(">>> [ApplicationRunner] Đã khởi tạo thành công tài khoản Admin: {}", adminEmail);
        } else {
            log.info(">>> [ApplicationRunner] Tài khoản Admin ({}) đã tồn tại.", adminEmail);
        }
    }
}