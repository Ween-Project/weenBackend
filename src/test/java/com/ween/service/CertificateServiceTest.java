package com.ween.service;

import com.ween.entity.Certificate;
import com.ween.entity.Event;
import com.ween.entity.User;
import com.ween.enums.CertificateTemplate;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.CertificateMapper;
import com.ween.repository.CertificateRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock private CertificateRepository certificateRepository;
    @Mock private CertificateMapper certificateMapper;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private CoinService coinService;
    @Mock private NotificationService notificationService;
    @InjectMocks private CertificateService certificateService;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        testUser = User.builder().username("u").email("u@e.com").passwordHash("p").fullName("U").build();
        testUser.setId("uid");
        testEvent = Event.builder().title("Ev").organizationId("org1").build();
        testEvent.setId("eid");
    }

    @Test @DisplayName("Generate certificate – success")
    void createCertificate_Pdf_success() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(certificateRepository.existsByUserIdAndEventId("uid", "eid")).thenReturn(false);
        when(certificateRepository.save(any())).thenAnswer(i -> { Certificate c = i.getArgument(0); c.setId("cid"); return c; });

        Certificate cert = certificateService.createCertificatePdf("uid", "eid", CertificateTemplate.GENERAL);
        assertThat(cert.getUserId()).isEqualTo("uid");
        assertThat(cert.getEventId()).isEqualTo("eid");
        assertThat(cert.getCertificateNumber()).startsWith("CERT-");
        verify(coinService).awardCertificateBonus(eq("uid"), anyString());
    }

    @Test @DisplayName("Generate certificate – user not found throws")
    void createCertificate_Pdf_userNotFound() {
        when(userRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> certificateService.createCertificatePdf("x", "eid", CertificateTemplate.GENERAL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Generate certificate – event not found throws")
    void createCertificate_Pdf_eventNotFound() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> certificateService.createCertificatePdf("uid", "x", CertificateTemplate.GENERAL))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Generate certificate – already exists throws")
    void createCertificate_Pdf_alreadyExists() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(certificateRepository.existsByUserIdAndEventId("uid", "eid")).thenReturn(true);
        assertThatThrownBy(() -> certificateService.createCertificatePdf("uid", "eid", CertificateTemplate.GENERAL))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test @DisplayName("Generate certificate with default template")
    void createCertificate_Pdf_defaultTemplate() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(certificateRepository.existsByUserIdAndEventId("uid", "eid")).thenReturn(false);
        when(certificateRepository.save(any())).thenAnswer(i -> { Certificate c = i.getArgument(0); c.setId("cid"); return c; });

        Certificate cert = certificateService.createCertificatePdf("uid", "eid");
        assertThat(cert.getTemplateType()).isEqualTo(CertificateTemplate.GENERAL);
    }

    @Test @DisplayName("Get certificate by id – found")
    void getCertificateById_found() {
        Certificate cert = Certificate.builder().userId("uid").eventId("eid").certificateNumber("CERT-1").build();
        cert.setId("cid");
        when(certificateRepository.findById("cid")).thenReturn(Optional.of(cert));
        assertThat(certificateService.getCertificateById("cid").getCertificateNumber()).isEqualTo("CERT-1");
    }

    @Test @DisplayName("Get certificate by id – not found throws")
    void getCertificateById_notFound() {
        when(certificateRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> certificateService.getCertificateById("x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Get certificate by number – found")
    void getCertificateByNumber_found() {
        Certificate cert = Certificate.builder().certificateNumber("CERT-1").build();
        when(certificateRepository.findByCertificateNumber("CERT-1")).thenReturn(Optional.of(cert));
        assertThat(certificateService.getCertificateByNumber("CERT-1")).isNotNull();
    }

    @Test @DisplayName("Get certificate by number – not found throws")
    void getCertificateByNumber_notFound() {
        when(certificateRepository.findByCertificateNumber("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> certificateService.getCertificateByNumber("x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Get user certificates")
    void getUserCertificates() {
        when(certificateRepository.findByUserId("uid")).thenReturn(List.of(
                Certificate.builder().userId("uid").build()
        ));
        assertThat(certificateService.getUserCertificates("uid")).hasSize(1);
    }

    @Test @DisplayName("Delete certificate")
    void deleteCertificate() {
        Certificate cert = Certificate.builder().build(); cert.setId("cid");
        when(certificateRepository.findById("cid")).thenReturn(Optional.of(cert));
        certificateService.deleteCertificate("cid");
        verify(certificateRepository).delete(cert);
    }

    @Test @DisplayName("Verify certificate – exists returns true")
    void verifyCertificate_true() {
        when(certificateRepository.findByCertificateNumber("CERT-1")).thenReturn(Optional.of(Certificate.builder().build()));
        assertThat(certificateService.verifyCertificate("CERT-1")).isTrue();
    }

    @Test @DisplayName("Verify certificate – not exists returns false")
    void verifyCertificate_false() {
        when(certificateRepository.findByCertificateNumber("x")).thenReturn(Optional.empty());
        assertThat(certificateService.verifyCertificate("x")).isFalse();
    }

    @Test @DisplayName("Get user certificate count")
    void getUserCertificateCount() {
        when(certificateRepository.findByUserId("uid")).thenReturn(List.of(
                Certificate.builder().build(), Certificate.builder().build()
        ));
        assertThat(certificateService.getUserCertificateCount("uid")).isEqualTo(2);
    }
}
