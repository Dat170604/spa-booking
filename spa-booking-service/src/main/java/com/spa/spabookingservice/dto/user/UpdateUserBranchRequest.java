package com.spa.spabookingservice.dto.user;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateUserBranchRequest {
    @NotNull(message = "Chi nhánh không được để trống")
    private Long branchId;
}