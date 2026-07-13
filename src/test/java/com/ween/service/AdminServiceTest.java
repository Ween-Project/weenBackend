package com.ween.service;

import com.ween.dto.response.AdminStatsResponse;
import com.ween.dto.response.OrganizationResponse;
import com.ween.dto.response.UserResponse;
import com.ween.entity.AuditLog;
import com.ween.entity.Organization;
import com.ween.entity.User;
import com.ween.enums.UserRole;
import com.ween.mapper.OrganizationMapper;
import com.ween.mapper.UserMapper;
import com.ween.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
    @Mock private PostRepository postRepository;
    @Mock private AuditLogRepository auditLogRepository;

    @InjectMocks
    private AdminService adminService;

    @Test
    void getAllUsersReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        User user = mock(User.class);
        UserResponse response = mock(UserResponse.class);
        when(userRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user)));
        when(userMapper.toUserResponse(user)).thenReturn(response);

        Page<UserResponse> result = adminService.getAllUsers(null, pageable);

        assertThat(result.getContent()).containsExactly(response);
    }

    @Test
    void banUserSetsBannedStateAndLogsAction() {
        User user = mock(User.class);
        User admin = mock(User.class);
        when(user.getUsername()).thenReturn("target_user");
        when(admin.getUsername()).thenReturn("admin_user");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        adminService.banUser("user-1", "spam", "admin-1");

        verify(user).setBanned(true);
        verify(user).setBanReason("spam");
        verify(userRepository).save(user);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void unbanUserClearsBannedStateAndLogsAction() {
        User user = mock(User.class);
        User admin = mock(User.class);
        when(user.getUsername()).thenReturn("target_user");
        when(admin.getUsername()).thenReturn("admin_user");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        adminService.unbanUser("user-1", "admin-1");

        verify(user).setBanned(false);
        verify(user).setBanReason(null);
        verify(userRepository).save(user);
        verify(auditLogRepository).save(any(AuditLog.class));
    }

    @Test
    void verifyOrganizationUpdatesVerifiedStatusAndLogs() {
        Organization organization = mock(Organization.class);
        when(organization.getOrganizationName()).thenReturn("weenOrg");
        OrganizationResponse response = mock(OrganizationResponse.class);

        User admin = mock(User.class);
        when(admin.getUsername()).thenReturn("admin_user");

        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));
        when(organizationMapper.toOrganizationResponse(organization)).thenReturn(response);
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));

        OrganizationResponse result = adminService.verifyOrganization("org-1", true, "verified ok", "admin-1");

        verify(organization).setVerified(true);
        verify(organization).setVerificationNote("verified ok");
        verify(organizationRepository).save(organization);
        verify(auditLogRepository).save(any(AuditLog.class));
        assertThat(result).isEqualTo(response);
    }

    @Test
    void getPlatformStatisticsComputesCoreMetrics() {
        when(userRepository.count()).thenReturn(10L);
        when(organizationRepository.count()).thenReturn(3L);
        when(eventRepository.count()).thenReturn(5L);
        when(eventRegistrationRepository.count()).thenReturn(20L);
        when(eventRegistrationRepository.countByIsJoinedTrue()).thenReturn(12L);
        when(certificateRepository.count()).thenReturn(8L);
        when(coinTransactionRepository.sumAllCoins()).thenReturn(500L);
        when(postRepository.count()).thenReturn(15L);
        when(organizationRepository.countByIsVerified(true)).thenReturn(2L);
        when(organizationRepository.countByIsVerified(false)).thenReturn(1L);
        when(userRepository.countByBanned(true)).thenReturn(1L);
        when(eventRepository.countByStatus(any())).thenReturn(4L);

        AdminStatsResponse stats = adminService.getPlatformStatistics();

        assertThat(stats.getTotalUsers()).isEqualTo(10L);
        assertThat(stats.getTotalOrganizations()).isEqualTo(3L);
        assertThat(stats.getTotalEvents()).isEqualTo(5L);
        assertThat(stats.getTotalCoinsDistributed()).isEqualTo(500L);
    }

    @Test
    void changeUserRoleUpdatesRoleAndLogs() {
        User user = mock(User.class);
        User admin = mock(User.class);
        when(user.getUsername()).thenReturn("username");
        when(user.getRole()).thenReturn(UserRole.VOLUNTEER);
        UserResponse userResponse = mock(UserResponse.class);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(userRepository.findById("admin-1")).thenReturn(Optional.of(admin));
        when(userMapper.toUserResponse(user)).thenReturn(userResponse);

        UserResponse result = adminService.changeUserRole("user-1", UserRole.ORGANIZER, "admin-1");

        verify(user).setRole(UserRole.ORGANIZER);
        verify(userRepository).save(user);
        verify(auditLogRepository).save(any(AuditLog.class));
        assertThat(result).isEqualTo(userResponse);
    }
}
