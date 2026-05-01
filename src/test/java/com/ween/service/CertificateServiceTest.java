package com.ween.service;

import com.ween.config.ThymeleafConfig;
import com.ween.entity.Certificate;
import com.ween.entity.Event;
import com.ween.entity.Organization;
import com.ween.entity.User;
import com.ween.enums.EventCategory;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.CertificateRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CertificateServiceTest {

    @Mock private CertificateRepository certificateRepository;
    @Mock private ThymeleafConfig thymeleafConfig;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private OrganizationRepository organizationRepository;
    @InjectMocks private CertificateService certificateService;

    private User testUser;
    private Event testEvent;
    private Certificate testCertificate;

    @BeforeEach
    void setUp() {
        testUser = User.builder().username("u").email("u@e.com").passwordHash("p").fullName("U").build();
        testUser.setId("uid");
        testEvent = Event.builder().title("Ev").organizationId("org1").category(EventCategory.EDUCATION).build();
        testEvent.setId("eid");

        testCertificate = Certificate.builder()
                .userId("uid")
                .eventId("eid")
                .certificateNumber("CERT-2026-ABCD")
                .templateType(EventCategory.EDUCATION)
                .issuedAt(LocalDateTime.now())
                .build();
        testCertificate.setId("cid");
    }

    // ── getCertificateById ──────────────────────────────────────────────

    @Test @DisplayName("Get certificate by id – found")
    void getCertificateById_found() {
        when(certificateRepository.findById("cid")).thenReturn(Optional.of(testCertificate));
        Certificate result = certificateService.getCertificateById("cid");
        assertThat(result.getCertificateNumber()).isEqualTo("CERT-2026-ABCD");
    }

    @Test @DisplayName("Get certificate by id – not found throws")
    void getCertificateById_notFound() {
        when(certificateRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> certificateService.getCertificateById("x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getCertificateByNumber ──────────────────────────────────────────

    @Test @DisplayName("Get certificate by number – found")
    void getCertificateByNumber_found() {
        when(certificateRepository.findByCertificateNumber("CERT-1")).thenReturn(Optional.of(testCertificate));
        assertThat(certificateService.getCertificateByNumber("CERT-1")).isNotNull();
    }

    @Test @DisplayName("Get certificate by number – not found throws")
    void getCertificateByNumber_notFound() {
        when(certificateRepository.findByCertificateNumber("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> certificateService.getCertificateByNumber("x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── getUserCertificates ─────────────────────────────────────────────

    @Test @DisplayName("Get user certificates")
    void getUserCertificates() {
        when(certificateRepository.findByUserId("uid")).thenReturn(List.of(testCertificate));
        assertThat(certificateService.getUserCertificates("uid")).hasSize(1);
    }

    @Test @DisplayName("Get user certificates – empty list")
    void getUserCertificates_empty() {
        when(certificateRepository.findByUserId("uid")).thenReturn(List.of());
        assertThat(certificateService.getUserCertificates("uid")).isEmpty();
    }

    // ── deleteCertificate ───────────────────────────────────────────────

    @Test @DisplayName("Delete certificate")
    void deleteCertificate() {
        when(certificateRepository.findById("cid")).thenReturn(Optional.of(testCertificate));
        certificateService.deleteCertificate("cid");
        verify(certificateRepository).delete(testCertificate);
    }

    @Test @DisplayName("Delete certificate – not found throws")
    void deleteCertificate_notFound() {
        when(certificateRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> certificateService.deleteCertificate("x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── verifyCertificate ───────────────────────────────────────────────

    @Test @DisplayName("Verify certificate – exists returns true")
    void verifyCertificate_true() {
        when(certificateRepository.findByCertificateNumber("CERT-1")).thenReturn(Optional.of(testCertificate));
        assertThat(certificateService.verifyCertificate("CERT-1")).isTrue();
    }

    @Test @DisplayName("Verify certificate – not exists returns false")
    void verifyCertificate_false() {
        when(certificateRepository.findByCertificateNumber("x")).thenReturn(Optional.empty());
        assertThat(certificateService.verifyCertificate("x")).isFalse();
    }

    // ── getUserCertificateCount ─────────────────────────────────────────

    @Test @DisplayName("Get user certificate count")
    void getUserCertificateCount() {
        when(certificateRepository.findByUserId("uid")).thenReturn(List.of(
                Certificate.builder().build(), Certificate.builder().build()
        ));
        assertThat(certificateService.getUserCertificateCount("uid")).isEqualTo(2);
    }

    @Test @DisplayName("Get user certificate count – zero")
    void getUserCertificateCount_zero() {
        when(certificateRepository.findByUserId("uid")).thenReturn(List.of());
        assertThat(certificateService.getUserCertificateCount("uid")).isEqualTo(0);
    }

    // ── downloadCertificatePdf (stub) ───────────────────────────────────

    @Test @DisplayName("Download certificate PDF – returns empty byte array (stub)")
    void downloadCertificatePdf_stub() {
        byte[] result = certificateService.downloadCertificatePdf("uid", "cid");
        assertThat(result).isEmpty();
    }

    // ── generateCertificatesAsync (stub) ────────────────────────────────

    @Test @DisplayName("Generate certificates async – returns userId (stub)")
    void generateCertificatesAsync_stub() {
        String result = certificateService.generateCertificatesAsync("uid", "eid");
        assertThat(result).isEqualTo("uid");
    }
}
