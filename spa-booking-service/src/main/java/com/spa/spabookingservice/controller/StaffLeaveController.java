package com.spa.spabookingservice.controller;

import com.spa.spabookingservice.dto.staff.StaffLeaveRequest;
import com.spa.spabookingservice.dto.staff.StaffLeaveResponse;
import com.spa.spabookingservice.service.StaffLeaveService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/staff-leaves")
@RequiredArgsConstructor
public class StaffLeaveController {

    private final StaffLeaveService staffLeaveService;

    // KTV tự đăng ký ngày nghỉ hoặc Admin xếp lịch nghỉ cho KTV
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<StaffLeaveResponse> createLeave(
            @Valid @RequestBody StaffLeaveRequest request,
            Authentication auth
    ) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(staffLeaveService.createLeave(auth.getName(), isAdmin, request));
    }

    // KTV xem lịch nghỉ của chính mình
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<List<StaffLeaveResponse>> getMyLeaves(Authentication auth) {
        return ResponseEntity.ok(staffLeaveService.getMyLeaves(auth.getName()));
    }

    // Admin xem toàn bộ danh sách lịch nghỉ theo chi nhánh
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<StaffLeaveResponse>> getAllLeaves(
            @RequestParam(required = false) Long branchId
    ) {
        return ResponseEntity.ok(staffLeaveService.getAllLeaves(branchId));
    }

    // Admin duyệt / từ chối đơn xin nghỉ (APPROVED / REJECTED)
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<StaffLeaveResponse> updateStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return ResponseEntity.ok(staffLeaveService.updateStatus(id, status));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
    public ResponseEntity<Void> deleteLeave(@PathVariable Long id) {
        staffLeaveService.deleteLeave(id);
        return ResponseEntity.noContent().build();
    }
}