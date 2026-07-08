package com.ween.service;

import com.ween.config.ThymeleafConfig;
import com.ween.entity.Certificate;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.CertificateRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock CertificateRepository certificateRepository;
    @Mock ThymeleafConfig thymeleafConfig;
    @Mock UserRepository userRepository;
    @Mock EventRepository eventRepository;
    @Mock OrganizationRepository organizationRepository;
    @InjectMocks CertificateService certificateService;

    @Test
    void getCertificateByIdReturnsExistingCertificate() {
        Certificate certificate = new Certificate();
        certificate.setId("cert-1");
        when(certificateRepository.findById("cert-1")).thenReturn(Optional.of(certificate));

        assertThat(certificateService.getCertificateById("cert-1")).isSameAs(certificate);
    }

    @Test
    void getCertificateByIdFailsWhenMissing() {
        when(certificateRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> certificateService.getCertificateById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
