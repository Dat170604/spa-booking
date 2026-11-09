package com.spa.spabookingservice.service;

import com.spa.spabookingservice.dto.service.ServiceRequest;
import com.spa.spabookingservice.dto.service.ServiceResponse;

import java.util.List;

public interface ServiceService {
    ServiceResponse createService(ServiceRequest request);
    ServiceResponse updateService(Long id, ServiceRequest request);
    List<ServiceResponse> getAllServices();
    ServiceResponse getServiceById(Long id);
    void deleteService(Long id);
}