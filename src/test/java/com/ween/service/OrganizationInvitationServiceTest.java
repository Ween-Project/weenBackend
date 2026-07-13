package com.ween.service;

import com.ween.dto.request.InviteOrganizerRequest;
import com.ween.entity.Organization;
import com.ween.entity.OrganizationInvitation;
import com.ween.entity.Organizer;
import com.ween.entity.User;
import com.ween.enums.InvitationStatus;
import com.ween.enums.UserRole;
import com.ween.repository.OrganizationInvitationRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.OrganizerRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class OrganizationInvitationServiceTest {

    private OrganizationInvitationRepository invitationRepository;
    private OrganizationRepository organizationRepository;
    private UserRepository userRepository;
    private OrganizerRepository organizerRepository;
    private EmailService emailService;
    private OrganizationInvitationService invitationService;

    @BeforeEach
    void setUp() {
        invitationRepository = mock(OrganizationInvitationRepository.class);
        organizationRepository = mock(OrganizationRepository.class);
        userRepository = mock(UserRepository.class);
        organizerRepository = mock(OrganizerRepository.class);
        emailService = mock(EmailService.class);

        invitationService = new OrganizationInvitationService(
                invitationRepository,
                organizationRepository,
                userRepository,
                organizerRepository,
                emailService
        );
        ReflectionTestUtils.setField(invitationService, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void inviteOrganizerSendsEmailOnSuccess() {
        String orgId = "org-123";
        InviteOrganizerRequest request = new InviteOrganizerRequest("testuser");

        Organization organization = mock(Organization.class);
        when(organization.getId()).thenReturn(orgId);
        when(organization.getOrganizationName()).thenReturn("Ween Org");
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        User user = mock(User.class);
        when(user.getId()).thenReturn("user-123");
        when(user.getRole()).thenReturn(UserRole.VOLUNTEER);
        when(user.getEmail()).thenReturn("user@example.com");
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.empty());
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));

        invitationService.inviteOrganizer(orgId, request);

        verify(invitationRepository).save(any(OrganizationInvitation.class));
        verify(emailService).sendOrganizerInvitationEmail(
                eq("user@example.com"),
                eq("Ween Org"),
                anyString(),
                anyString()
        );
    }

    @Test
    void inviteOrganizerThrowsExceptionWhenUserIsAlreadyOrganizer() {
        String orgId = "org-123";
        InviteOrganizerRequest request = new InviteOrganizerRequest("testuser");

        Organization organization = mock(Organization.class);
        when(organizationRepository.findById(orgId)).thenReturn(Optional.of(organization));

        User user = mock(User.class);
        when(user.getRole()).thenReturn(UserRole.ORGANIZER);
        when(userRepository.findByEmail("testuser")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> invitationService.inviteOrganizer(orgId, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("User is already an organizer");
    }

    @Test
    void approveInvitationSavesOrganizerAndUpdatesUserRole() {
        String token = "valid-token";
        Organization organization = mock(Organization.class);
        User user = mock(User.class);
        when(user.getRole()).thenReturn(UserRole.VOLUNTEER);

        OrganizationInvitation invitation = mock(OrganizationInvitation.class);
        when(invitation.getStatus()).thenReturn(InvitationStatus.PENDING);
        when(invitation.getExpirationDate()).thenReturn(LocalDateTime.now().plusHours(1));
        when(invitation.getUser()).thenReturn(user);
        when(invitation.getOrganization()).thenReturn(organization);

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        invitationService.approveInvitation(token);

        verify(invitation).setStatus(InvitationStatus.APPROVED);
        verify(invitationRepository).save(invitation);
        verify(organizerRepository).save(any(Organizer.class));
        verify(user).setRole(UserRole.ORGANIZER);
        verify(userRepository).save(user);
    }

    @Test
    void approveInvitationThrowsExceptionIfExpired() {
        String token = "expired-token";
        OrganizationInvitation invitation = mock(OrganizationInvitation.class);
        when(invitation.getStatus()).thenReturn(InvitationStatus.PENDING);
        when(invitation.getExpirationDate()).thenReturn(LocalDateTime.now().minusHours(1));

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        assertThatThrownBy(() -> invitationService.approveInvitation(token))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invitation has expired");
    }

    @Test
    void rejectInvitationUpdatesStatusToRejected() {
        String token = "reject-token";
        OrganizationInvitation invitation = mock(OrganizationInvitation.class);
        when(invitation.getStatus()).thenReturn(InvitationStatus.PENDING);

        when(invitationRepository.findByToken(token)).thenReturn(Optional.of(invitation));

        invitationService.rejectInvitation(token);

        verify(invitation).setStatus(InvitationStatus.REJECTED);
        verify(invitationRepository).save(invitation);
    }

    @Test
    void removeOrganizerDeletesOrganizerAndSetsUserRoleToVolunteer() {
        String orgId = "org-123";
        String organizerId = "organizer-123";

        User user = mock(User.class);
        Organizer organizer = mock(Organizer.class);
        when(organizer.getUser()).thenReturn(user);

        when(organizerRepository.findByIdAndOrganizationId(organizerId, orgId)).thenReturn(Optional.of(organizer));

        invitationService.removeOrganizer(orgId, organizerId);

        verify(organizerRepository).delete(organizer);
        verify(user).setRole(UserRole.VOLUNTEER);
        verify(userRepository).save(user);
    }
}
