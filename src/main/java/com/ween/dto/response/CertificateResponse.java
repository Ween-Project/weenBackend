package com.ween.dto.response;

import com.ween.enums.CertificateTemplate;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificateResponse {
    private String id;
    private String certificateNumber;
    private String userId;
    private String eventId;
    private String eventTitle;
    private String pdfUrl;
    private CertificateTemplate templateType;
    private LocalDateTime issuedAt;
}
