package com.spa.spabookingservice.controller;

import com.spa.spabookingservice.dto.service.ServiceRequest;
import com.spa.spabookingservice.dto.service.ServiceResponse;
import com.spa.spabookingservice.service.ServiceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/services")
@RequiredArgsConstructor
public class ServicesController {

    private final ServiceService serviceService;

    // 1. Lấy danh sách dịch vụ có tìm kiếm & lọc danh mục
    @GetMapping
    public ResponseEntity<Page<ServiceResponse>> getAllServices(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        return ResponseEntity.ok(
                serviceService.searchServices(keyword, category, PageRequest.of(page, size, Sort.by("id").descending()))
        );
    }

    // 2. Xem chi tiết dịch vụ theo ID
    @GetMapping("/{id}")
    public ResponseEntity<ServiceResponse> getServiceById(@PathVariable Long id) {
        return ResponseEntity.ok(serviceService.getServiceById(id));
    }


    // 3. API Upload ảnh dịch vụ (Trả về URL ảnh)
    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadImage(@RequestParam("file") MultipartFile file) {
        String fileUrl = serviceService.uploadServiceImage(file);
        return ResponseEntity.ok(Map.of("imageUrl", fileUrl));
    }

    // 4. Tạo dịch vụ mới
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> createService(@Valid @RequestBody ServiceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(serviceService.createService(request));
    }

    // 5. Cập nhật dịch vụ
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ServiceResponse> updateService(
            @PathVariable Long id,
            @Valid @RequestBody ServiceRequest request
    ) {
        return ResponseEntity.ok(serviceService.updateService(id, request));
    }

    // 6. Bật / Tắt trạng thái dịch vụ (Khớp với nút Tạm ngưng / Mở bán)
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleServiceStatus(@PathVariable Long id) {
        serviceService.toggleServiceStatus(id);
        return ResponseEntity.noContent().build();
    }

    // 7. Xóa dịch vụ
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteService(@PathVariable Long id) {
        serviceService.deleteService(id);
        return ResponseEntity.noContent().build();
    }
}