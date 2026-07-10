package com.ween.service;

import com.ween.dto.request.InviteOrganizerRequest;
import com.ween.entity.Organization;
import com.ween.entity.OrganizationInvitation;
import com.ween.entity.Organizer;
import com.ween.entity.User;
import com.ween.enums.InvitationStatus;
import com.ween.enums.UserRole;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.OrganizationInvitationRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.OrganizerRepository;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizationInvitationService {

    private final OrganizationInvitationRepository invitationRepository;
    private final OrganizationRepository organizationRepository;
    private final UserRepository userRepository;
    private final OrganizerRepository organizerRepository;
    private final EmailService emailService;

    @Value("${ween.app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public void inviteOrganizer(String organizationId, InviteOrganizerRequest request) {
        Organization org = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found"));

        String identifier = request.getEmailOrUsername();
        User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByUsername(identifier)
                        .orElseThrow(() -> new ResourceNotFoundException("User not found")));

        if (user.getRole() == UserRole.ORGANIZER) {
            throw new IllegalArgumentException("User is already an organizer for an organization");
        }

        String token = UUID.randomUUID().toString();
        OrganizationInvitation invitation = OrganizationInvitation.builder()
                .token(token)
                .organization(org)
                .user(user)
                .status(InvitationStatus.PENDING)
                .expirationDate(LocalDateTime.now().plusDays(1))
                .build();
        
        invitationRepository.save(invitation);

        String approveLink = frontendUrl + "/api/v1/invitations/approve?token=" + token;
        String rejectLink = frontendUrl + "/api/v1/invitations/reject?token=" + token;

        emailService.sendOrganizerInvitationEmail(user.getEmail(), org.getOrganizationName(), approveLink, rejectLink);
        log.info("Invitation sent to user {} for organization {}", user.getId(), org.getId());
    }

    @Transactional
    public void approveInvitation(String token) {
        OrganizationInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is not pending");
        }
        if (invitation.getExpirationDate().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invitation has expired");
        }

        User user = invitation.getUser();
        if (user.getRole() == UserRole.ORGANIZER) {
            throw new IllegalArgumentException("User is already an organizer in another organization");
        }

        invitation.setStatus(InvitationStatus.APPROVED);
        invitationRepository.save(invitation);

        Organizer organizer = Organizer.builder()
                .user(user)
                .organization(invitation.getOrganization())
                .build();
        organizerRepository.save(organizer);

        user.setRole(UserRole.ORGANIZER);
        userRepository.save(user);
    }

    @Transactional
    public void rejectInvitation(String token) {
        OrganizationInvitation invitation = invitationRepository.findByToken(token)
                .orElseThrow(() -> new ResourceNotFoundException("Invitation not found"));

        if (invitation.getStatus() != InvitationStatus.PENDING) {
            throw new IllegalArgumentException("Invitation is not pending");
        }

        invitation.setStatus(InvitationStatus.REJECTED);
        invitationRepository.save(invitation);
    }

    @Transactional
    public void removeOrganizer(String organizationId, String organizerId) {
        Organizer organizer = organizerRepository.findByIdAndOrganizationId(organizerId, organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organizer not found for this organization"));

        User user = organizer.getUser();
        organizerRepository.delete(organizer);

        user.setRole(UserRole.VOLUNTEER);
        userRepository.save(user);
        log.info("Organizer {} removed from organization {}", organizerId, organizationId);
    }
}
