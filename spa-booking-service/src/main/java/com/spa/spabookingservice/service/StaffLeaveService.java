package com.spa.spabookingservice.service;

import com.spa.spabookingservice.dto.staff.StaffLeaveRequest;
import com.spa.spabookingservice.dto.staff.StaffLeaveResponse;

import java.util.List;

public interface StaffLeaveService {


    StaffLeaveResponse createLeave(String callerEmail, boolean isAdmin, StaffLeaveRequest req);
    StaffLeaveResponse updateStatus(Long leaveId, String status);

    List<StaffLeaveResponse> getMyLeaves(String email);

    List<StaffLeaveResponse> getAllLeaves(Long branchId);

    void deleteLeave(Long leaveId);
}