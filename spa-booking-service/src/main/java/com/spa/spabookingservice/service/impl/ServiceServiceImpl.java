package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.service.ServiceRequest;
import com.spa.spabookingservice.dto.service.ServiceResponse;
import com.spa.spabookingservice.entity.ServiceEntity;
import com.spa.spabookingservice.exception.AppException;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.repository.ServiceRepository;
import com.spa.spabookingservice.service.ServiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ServiceServiceImpl implements ServiceService {

    private final ServiceRepository serviceRepository;

    @Override
    @Transactional
    public ServiceResponse createService(ServiceRequest request) {
        ServiceEntity entity = ServiceEntity.builder()
                .name(request.getName())
                .category(request.getCategory()) // Bổ sung category
                .imageUrl(request.getImageUrl()) // Bổ sung imageUrl
                .description(request.getDescription())
                .price(request.getPrice())
                .durationMinutes(request.getDurationMinutes())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        return mapToResponse(serviceRepository.save(entity));
    }

    @Override
    @Transactional
    public ServiceResponse updateService(Long id, ServiceRequest request) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        entity.setName(request.getName());
        entity.setCategory(request.getCategory()); // Bổ sung category
        entity.setImageUrl(request.getImageUrl()); // Bổ sung imageUrl
        entity.setDescription(request.getDescription());
        entity.setPrice(request.getPrice());
        entity.setDurationMinutes(request.getDurationMinutes());
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        return mapToResponse(serviceRepository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponse getServiceById(Long id) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    public void deleteService(Long id) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        serviceRepository.delete(entity);
    }

    @Override
    public Page<ServiceResponse> searchServices(String keyword, String category, Pageable pageable) {
        return serviceRepository.searchServices(keyword, category, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void toggleServiceStatus(Long id) {
        ServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy dịch vụ"));
        service.setStatus("ACTIVE".equals(service.getStatus()) ? "INACTIVE" : "ACTIVE");
        serviceRepository.save(service);
    }

    @Override
    public String uploadServiceImage(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File ảnh không được để trống!");
        }
        try {
            Path uploadPath = Paths.get("uploads/services");
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            String originalFilename = file.getOriginalFilename();
            String extension = originalFilename != null && originalFilename.contains(".")
                    ? originalFilename.substring(originalFilename.lastIndexOf("."))
                    : ".jpg";
            String fileName = UUID.randomUUID() + extension;

            Path filePath = uploadPath.resolve(fileName);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            return "http://localhost:8080/uploads/services/" + fileName;
        } catch (IOException e) {
            throw new RuntimeException("Lỗi lưu file ảnh: " + e.getMessage(), e);
        }
    }

    private ServiceResponse mapToResponse(ServiceEntity entity) {
        return ServiceResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .category(entity.getCategory()) // Bổ sung category
                .imageUrl(entity.getImageUrl()) // Bổ sung imageUrl
                .description(entity.getDescription())
                .price(entity.getPrice())
                .durationMinutes(entity.getDurationMinutes())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}