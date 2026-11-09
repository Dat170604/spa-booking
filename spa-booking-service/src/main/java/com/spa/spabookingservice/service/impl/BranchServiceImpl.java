package com.spa.spabookingservice.service.impl;

import com.spa.spabookingservice.dto.branch.BranchRequest;
import com.spa.spabookingservice.dto.branch.BranchResponse;
import com.spa.spabookingservice.entity.BranchEntity;
import com.spa.spabookingservice.exception.AppException;
import com.spa.spabookingservice.exception.ErrorCode;
import com.spa.spabookingservice.repository.BranchRepository;
import com.spa.spabookingservice.service.BranchService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BranchServiceImpl implements BranchService {

    private final BranchRepository branchRepository;

    @Override
    @Transactional
    public BranchResponse createBranch(BranchRequest request) {
        if (branchRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.CONFLICT, "Tên chi nhánh đã tồn tại trên hệ thống");
        }

        BranchEntity branch = BranchEntity.builder()
                .name(request.getName())
                .address(request.getAddress())
                .phone(request.getPhone())
                .email(request.getEmail())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .openTime(request.getOpenTime())
                .closeTime(request.getCloseTime())
                .active(request.isActive())
                .build();

        return mapToResponse(branchRepository.save(branch));
    }

    @Override
    @Transactional
    public BranchResponse updateBranch(Long id, BranchRequest request) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chi nhánh"));

        if (!branch.getName().equalsIgnoreCase(request.getName()) && branchRepository.existsByName(request.getName())) {
            throw new AppException(ErrorCode.CONFLICT, "Tên chi nhánh trùng với một chi nhánh khác");
        }

        branch.setName(request.getName());
        branch.setAddress(request.getAddress());
        branch.setPhone(request.getPhone());
        branch.setEmail(request.getEmail());
        branch.setDescription(request.getDescription());
        branch.setImageUrl(request.getImageUrl());
        branch.setOpenTime(request.getOpenTime());
        branch.setCloseTime(request.getCloseTime());
        branch.setActive(request.isActive());

        return mapToResponse(branchRepository.save(branch));
    }

    @Override
    @Transactional(readOnly = true)
    public BranchResponse getBranchById(Long id) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chi nhánh"));
        return mapToResponse(branch);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BranchResponse> getAllActiveBranches() {
        return branchRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BranchResponse> searchBranches(String keyword, Pageable pageable) {
        return branchRepository.searchBranches(keyword, pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    public void toggleBranchStatus(Long id) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chi nhánh"));
        branch.setActive(!branch.isActive());
        branchRepository.save(branch);
    }

    @Override
    @Transactional
    public void deleteBranch(Long id) {
        BranchEntity branch = branchRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND, "Không tìm thấy chi nhánh"));
        branchRepository.delete(branch);
    }

    private BranchResponse mapToResponse(BranchEntity entity) {
        return BranchResponse.builder()
                .id(entity.getId())
                .name(entity.getName())
                .address(entity.getAddress())
                .phone(entity.getPhone())
                .email(entity.getEmail())
                .description(entity.getDescription())
                .imageUrl(entity.getImageUrl())
                .openTime(entity.getOpenTime())
                .closeTime(entity.getCloseTime())
                .active(entity.isActive())
                .build();
    }
}