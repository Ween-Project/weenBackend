package com.ween.dto.request;

import com.ween.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangeRoleRequest {
    @NotNull(message = "Role is required")
    private UserRole role;
}
