package com.spa.spabookingservice.service;

import com.spa.spabookingservice.dto.branch.BranchRequest;
import com.spa.spabookingservice.dto.branch.BranchResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BranchService {

    BranchResponse createBranch(BranchRequest request);

    BranchResponse updateBranch(Long id, BranchRequest request);

    BranchResponse getBranchById(Long id);

    List<BranchResponse> getAllActiveBranches();

    Page<BranchResponse> searchBranches(String keyword, Pageable pageable);

    void toggleBranchStatus(Long id);

    void deleteBranch(Long id);

    String uploadBranchImage(MultipartFile file);
}