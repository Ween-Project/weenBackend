package com.ween.service;

import com.ween.repository.CertificateRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class CertificateTriggerServiceTest {

    @Mock CertificateRepository certificateRepository;
    @InjectMocks CertificateTriggerService certificateTriggerService;

    @Test
    void createsWithMockitoDependencies() {
        assertThat(certificateTriggerService).isNotNull();
    }
}
