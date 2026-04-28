package com.ween.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckinRequest {

    @NotBlank(message = "Event ID is required")
    private String eventId;

    @NotBlank(message = "QR token is required")
    private String qrToken;
}
