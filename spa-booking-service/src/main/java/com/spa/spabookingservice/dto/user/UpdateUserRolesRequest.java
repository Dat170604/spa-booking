package com.spa.spabookingservice.dto.user;

import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
public class UpdateUserRolesRequest {

    @NotEmpty(message = "Vui lòng chọn ít nhất 1 role")
    private Set<String> roles; // VD: ["ROLE_ADMIN", "ROLE_STAFF", "ROLE_CUSTOMER"]
}