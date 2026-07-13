package com.ween.service;

import com.ween.entity.Certificate;
import com.ween.enums.EventCategory;
import com.ween.repository.CertificateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateTriggerServiceTest {

    @Mock
    private CertificateRepository certificateRepository;

    @InjectMocks
    private CertificateTriggerService certificateTriggerService;

    @Test
    void autoGenerateCertificateRecordSavesAndReturnsCertificate() {
        String userId = "user-123";
        String eventId = "event-456";
        EventCategory category = EventCategory.EDUCATION;

        Certificate mockSavedCertificate = new Certificate();
        mockSavedCertificate.setId("cert-999");
        mockSavedCertificate.setUserId(userId);
        mockSavedCertificate.setEventId(eventId);
        mockSavedCertificate.setTemplateType(category);
        mockSavedCertificate.setCertificateNumber("CERT-2026-ABCD");

        when(certificateRepository.save(any(Certificate.class))).thenReturn(mockSavedCertificate);

        Certificate result = certificateTriggerService.autoGenerateCertificateRecord(userId, eventId, category);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("cert-999");
        assertThat(result.getUserId()).isEqualTo(userId);
        assertThat(result.getEventId()).isEqualTo(eventId);

        ArgumentCaptor<Certificate> certificateCaptor = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateRepository).save(certificateCaptor.capture());

        Certificate saved = certificateCaptor.getValue();
        assertThat(saved.getUserId()).isEqualTo(userId);
        assertThat(saved.getEventId()).isEqualTo(eventId);
        assertThat(saved.getTemplateType()).isEqualTo(category);
        assertThat(saved.getCertificateNumber()).startsWith("CERT-");
        assertThat(saved.getIssuedAt()).isNotNull();
    }
}
