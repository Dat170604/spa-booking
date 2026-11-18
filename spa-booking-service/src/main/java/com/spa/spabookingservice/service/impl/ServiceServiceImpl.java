package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.service.ServiceRequest;
import com.spa.spabookingservice.dto.service.ServiceResponse;
import com.spa.spabookingservice.entity.ServiceEntity;
import com.spa.spabookingservice.exception.AppException;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.repository.ServiceRepository;
import com.spa.spabookingservice.service.FileStorageService;
import com.spa.spabookingservice.service.ServiceService;
import com.spa.spabookingservice.util.RestPage;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
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
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    @CacheEvict(value = "service_search", allEntries = true)
    public ServiceResponse createService(ServiceRequest request) {
        ServiceEntity entity = ServiceEntity.builder()
                .name(request.getName())
                .category(request.getCategory())
                .imageUrl(request.getImageUrl())
                .description(request.getDescription())
                .price(request.getPrice())
                .durationMinutes(request.getDurationMinutes())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .build();

        return mapToResponse(serviceRepository.save(entity));
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "service_search", allEntries = true),
            @CacheEvict(value = "service_detail", key = "#id")
    })
    public ServiceResponse updateService(Long id, ServiceRequest request) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        entity.setName(request.getName());
        entity.setCategory(request.getCategory());
        entity.setImageUrl(request.getImageUrl());
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
    @Cacheable(value = "service_detail", key = "#id")
    public ServiceResponse getServiceById(Long id) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        return mapToResponse(entity);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "service_search", allEntries = true),
            @CacheEvict(value = "service_detail", key = "#id")
    })
    public void deleteService(Long id) {
        ServiceEntity entity = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        serviceRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(
            value = "service_search",
            key = "(#keyword ?: 'all') + ':' + (#category ?: 'all') + ':' + #pageable.pageNumber + ':' + #pageable.pageSize"
    )
    public Page<ServiceResponse> searchServices(String keyword, String category, Pageable pageable) {
        Page<ServiceResponse> page = serviceRepository.searchServices(keyword, category, pageable)
                .map(this::mapToResponse);
        return new RestPage<>(page);
    }

    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "service_search", allEntries = true),
            @CacheEvict(value = "service_detail", key = "#id")
    })
    public void toggleServiceStatus(Long id) {
        ServiceEntity service = serviceRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy dịch vụ"));
        service.setStatus("ACTIVE".equals(service.getStatus()) ? "INACTIVE" : "ACTIVE");
        serviceRepository.save(service);
    }

    @Override
    public String uploadServiceImage(MultipartFile file) {
        return fileStorageService.uploadImage(file, "services");
    }

    private ServiceResponse mapToResponse(ServiceEntity entity) {
        return ServiceResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .category(entity.getCategory())
                .imageUrl(entity.getImageUrl())
                .description(entity.getDescription())
                .price(entity.getPrice())
                .durationMinutes(entity.getDurationMinutes())
                .status(entity.getStatus())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}