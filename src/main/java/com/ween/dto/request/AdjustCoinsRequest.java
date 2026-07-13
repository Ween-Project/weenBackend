package com.ween.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AdjustCoinsRequest {
    @NotBlank(message = "User ID is required")
    private String userId;

    @NotNull(message = "Amount is required")
    private Integer amount;

    @NotBlank(message = "Reason is required")
    private String reason;
}
