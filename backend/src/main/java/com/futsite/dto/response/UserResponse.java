package com.futsite.dto.response;

import com.futsite.model.enums.UserRole;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private UserRole role;
}
