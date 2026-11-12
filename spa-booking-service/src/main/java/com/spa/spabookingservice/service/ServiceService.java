package com.spa.spabookingservice.service;

import com.spa.spabookingservice.dto.service.ServiceRequest;
import com.spa.spabookingservice.dto.service.ServiceResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ServiceService {
    ServiceResponse createService(ServiceRequest request);
    ServiceResponse updateService(Long id, ServiceRequest request);
    ServiceResponse getServiceById(Long id);
    void deleteService(Long id);
    Page<ServiceResponse> searchServices(String keyword, String category, Pageable pageable);
    void toggleServiceStatus(Long id);
    String uploadServiceImage(MultipartFile file);
}