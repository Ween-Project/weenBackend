package com.ween.service;

import com.ween.entity.Certificate;
import com.ween.enums.EventCategory;
import com.ween.repository.CertificateRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateTriggerServiceTest {

    @Mock private CertificateRepository certificateRepository;
    @InjectMocks private CertificateTriggerService certificateTriggerService;

    @Test @DisplayName("Auto-generate certificate record – success")
    void autoGenerateCertificateRecord_success() {
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(i -> {
            Certificate c = i.getArgument(0);
            c.setId("generated-id");
            return c;
        });

        certificateTriggerService.autoGenerateCertificateRecord("uid", "eid", EventCategory.EDUCATION);

        ArgumentCaptor<Certificate> captor = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateRepository).save(captor.capture());

        Certificate saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo("uid");
        assertThat(saved.getEventId()).isEqualTo("eid");
        assertThat(saved.getTemplateType()).isEqualTo(EventCategory.EDUCATION);
        assertThat(saved.getCertificateNumber()).startsWith("CERT-");
        assertThat(saved.getIssuedAt()).isNotNull();
    }

    @Test @DisplayName("Auto-generate certificate record – different category")
    void autoGenerateCertificateRecord_differentCategory() {
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(i -> i.getArgument(0));

        certificateTriggerService.autoGenerateCertificateRecord("uid", "eid", EventCategory.ENVIRONMENT);

        ArgumentCaptor<Certificate> captor = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateRepository).save(captor.capture());

        assertThat(captor.getValue().getTemplateType()).isEqualTo(EventCategory.ENVIRONMENT);
    }

    @Test @DisplayName("Auto-generate certificate – unique certificate numbers")
    void autoGenerateCertificateRecord_uniqueNumbers() {
        when(certificateRepository.save(any(Certificate.class))).thenAnswer(i -> i.getArgument(0));

        certificateTriggerService.autoGenerateCertificateRecord("u1", "e1", EventCategory.HEALTH);
        certificateTriggerService.autoGenerateCertificateRecord("u2", "e2", EventCategory.HEALTH);

        ArgumentCaptor<Certificate> captor = ArgumentCaptor.forClass(Certificate.class);
        verify(certificateRepository, times(2)).save(captor.capture());

        String certNum1 = captor.getAllValues().get(0).getCertificateNumber();
        String certNum2 = captor.getAllValues().get(1).getCertificateNumber();
        assertThat(certNum1).isNotEqualTo(certNum2);
    }
}
