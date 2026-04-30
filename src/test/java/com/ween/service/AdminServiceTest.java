package com.ween.service;

import com.ween.dto.response.AdminStatsResponse;
import com.ween.entity.Organization;
import com.ween.entity.User;
import com.ween.mapper.OrganizationMapper;
import com.ween.mapper.UserMapper;
import com.ween.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @Mock private CertificateRepository certificateRepository;
    @Mock private CoinTransactionRepository coinTransactionRepository;
    @Mock private UserMapper userMapper;
    @Mock private OrganizationMapper organizationMapper;
    @InjectMocks private AdminService adminService;

    private User testUser;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testUser = User.builder().username("u").email("u@e.com").passwordHash("p")
                .fullName("U").banned(false).build();
        testUser.setId("uid");
        testOrg = Organization.builder().username("o").email("o@e.com")
                .passwordHash("p").organizationName("O").build();
        testOrg.setId("oid");
    }

    @Test @DisplayName("Get all users – no search")
    void getAllUsers_noSearch() {
        when(userRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
        adminService.getAllUsers(null, PageRequest.of(0, 10));
        verify(userRepository).findAll(any(Pageable.class));
    }

    @Test @DisplayName("Get all users – with search")
    void getAllUsers_withSearch() {
        when(userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                eq("test"), eq("test"), any())).thenReturn(Page.empty());
        adminService.getAllUsers("test", PageRequest.of(0, 10));
        verify(userRepository).findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(any(), any(), any());
    }

    @Test @DisplayName("Ban user – success")
    void banUser() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        adminService.banUser("uid", "Spam");
        assertThat(testUser.getBanned()).isTrue();
        assertThat(testUser.getBanReason()).isEqualTo("Spam");
        verify(userRepository).save(testUser);
    }

    @Test @DisplayName("Ban user – not found throws")
    void banUser_notFound() {
        when(userRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> adminService.banUser("x", "r")).isInstanceOf(RuntimeException.class);
    }

    @Test @DisplayName("Unban user – success")
    void unbanUser() {
        testUser.setBanned(true); testUser.setBanReason("Spam");
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        adminService.unbanUser("uid");
        assertThat(testUser.getBanned()).isFalse();
        assertThat(testUser.getBanReason()).isNull();
    }

    @Test @DisplayName("Get all organizations – no search")
    void getAllOrgs_noSearch() {
        when(organizationRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());
        adminService.getAllOrganizations(null, PageRequest.of(0, 10));
        verify(organizationRepository).findAll(any(Pageable.class));
    }

    @Test @DisplayName("Get all organizations – with search")
    void getAllOrgs_withSearch() {
        when(organizationRepository.findByOrganizationNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                any(), any(), any())).thenReturn(Page.empty());
        adminService.getAllOrganizations("test", PageRequest.of(0, 10));
        verify(organizationRepository).findByOrganizationNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(any(), any(), any());
    }

    @Test @DisplayName("Verify organization")
    void verifyOrganization() {
        when(organizationRepository.findById("oid")).thenReturn(Optional.of(testOrg));
        adminService.verifyOrganization("oid", true, "Approved");
        assertThat(testOrg.getIsVerified()).isTrue();
        assertThat(testOrg.getVerificationNote()).isEqualTo("Approved");
    }

    @Test @DisplayName("Reject organization")
    void rejectOrganization() {
        when(organizationRepository.findById("oid")).thenReturn(Optional.of(testOrg));
        adminService.rejectOrganization("oid", "Invalid docs");
        assertThat(testOrg.getIsVerified()).isFalse();
        assertThat(testOrg.getVerificationNote()).isEqualTo("Invalid docs");
    }

    @Test @DisplayName("Get admin stats")
    void getAdminStats() {
        when(userRepository.count()).thenReturn(100L);
        when(organizationRepository.count()).thenReturn(10L);
        when(eventRepository.count()).thenReturn(50L);
        when(eventRegistrationRepository.count()).thenReturn(500L);
        when(eventRegistrationRepository.countByIsJoinedTrue()).thenReturn(300L);
        when(certificateRepository.count()).thenReturn(200L);
        when(coinTransactionRepository.sumAllCoins()).thenReturn(50000L);

        AdminStatsResponse stats = adminService.getAdminStats();
        assertThat(stats.getTotalUsers()).isEqualTo(100);
        assertThat(stats.getTotalOrganizations()).isEqualTo(10);
        assertThat(stats.getTotalEvents()).isEqualTo(50);
        assertThat(stats.getTotalRegistrations()).isEqualTo(500);
        assertThat(stats.getTotalAttendees()).isEqualTo(300);
        assertThat(stats.getTotalCoinsDistributed()).isEqualTo(50000);
        assertThat(stats.getTotalCertificatesIssued()).isEqualTo(200);
    }

    @Test @DisplayName("Get admin stats – null coins returns 0")
    void getAdminStats_nullCoins() {
        when(userRepository.count()).thenReturn(0L);
        when(organizationRepository.count()).thenReturn(0L);
        when(eventRepository.count()).thenReturn(0L);
        when(eventRegistrationRepository.count()).thenReturn(0L);
        when(eventRegistrationRepository.countByIsJoinedTrue()).thenReturn(0L);
        when(certificateRepository.count()).thenReturn(0L);
        when(coinTransactionRepository.sumAllCoins()).thenReturn(null);

        AdminStatsResponse stats = adminService.getAdminStats();
        assertThat(stats.getTotalCoinsDistributed()).isEqualTo(0);
    }

    @Test @DisplayName("Delete user")
    void deleteUser() {
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        adminService.deleteUser("uid");
        verify(userRepository).delete(testUser);
    }

    @Test @DisplayName("Delete organization")
    void deleteOrganization() {
        when(organizationRepository.findById("oid")).thenReturn(Optional.of(testOrg));
        adminService.deleteOrganization("oid");
        verify(organizationRepository).delete(testOrg);
    }
}
