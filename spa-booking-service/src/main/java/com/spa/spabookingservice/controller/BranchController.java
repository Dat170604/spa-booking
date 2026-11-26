package com.spa.spabookingservice.controller;

import com.spa.spabookingservice.audit.LogActivity;
import com.spa.spabookingservice.dto.branch.BranchRequest;
import com.spa.spabookingservice.dto.branch.BranchResponse;
import com.spa.spabookingservice.service.BranchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
public class BranchController {

    private final BranchService branchService;

    // Public API: Khách hàng xem danh sách chi nhánh đang hoạt động
    @GetMapping("/active")
    public ResponseEntity<List<BranchResponse>> getActiveBranches() {
        return ResponseEntity.ok(branchService.getAllActiveBranches());
    }

    // Public API: Xem chi tiết 1 chi nhánh
    @GetMapping("/{id}")
    public ResponseEntity<BranchResponse> getBranchById(@PathVariable Long id) {
        return ResponseEntity.ok(branchService.getBranchById(id));
    }

    // Phân trang & Tìm kiếm chi nhánh (Chỉ Admin)
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<BranchResponse>> searchBranches(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Sort sort = sortDir.equalsIgnoreCase("desc")
                ? Sort.by(sortBy).descending()
                : Sort.by(sortBy).ascending();
        return ResponseEntity.ok(branchService.searchBranches(keyword, PageRequest.of(page, size, sort)));
    }

    // Tạo chi nhánh mới (Chỉ Admin)
    @LogActivity(module = "BRANCH", action = "CREATE_BRANCH", description = "Admin thêm chi nhánh mới")
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BranchResponse> createBranch(@Valid @RequestBody BranchRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(branchService.createBranch(request));
    }

    // Cập nhật chi nhánh (Chỉ Admin)
    @LogActivity(module = "BRANCH", action = "UPDATE_BRANCH", description = "Admin cập nhật thông tin chi nhánh")
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BranchResponse> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody BranchRequest request
    ) {
        return ResponseEntity.ok(branchService.updateBranch(id, request));
    }

    // Bật / Tắt trạng thái chi nhánh (Chỉ Admin)
    @LogActivity(module = "BRANCH", action = "TOGGLE_BRANCH_STATUS", description = "Admin bật/tắt trạng thái hoạt động chi nhánh")
    @PatchMapping("/{id}/toggle-status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> toggleBranchStatus(@PathVariable Long id) {
        branchService.toggleBranchStatus(id);
        return ResponseEntity.noContent().build();
    }

    // Xóa chi nhánh (Chỉ Admin)
    @LogActivity(module = "BRANCH", action = "DELETE_BRANCH", description = "Admin xóa chi nhánh")
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteBranch(@PathVariable Long id) {
        branchService.deleteBranch(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/upload-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> uploadBranchImage(@RequestParam("file") MultipartFile file) {
        String fileUrl = branchService.uploadBranchImage(file);
        return ResponseEntity.ok(Map.of("imageUrl", fileUrl));
    }
}