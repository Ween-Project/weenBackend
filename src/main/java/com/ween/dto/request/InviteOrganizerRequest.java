package com.ween.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InviteOrganizerRequest {
    
    @NotBlank(message = "Username or email is required")
    private String emailOrUsername;
    
}
