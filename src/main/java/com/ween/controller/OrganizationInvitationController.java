package com.ween.controller;

import com.ween.dto.request.InviteOrganizerRequest;
import com.ween.service.OrganizationInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrganizationInvitationController {

    private final OrganizationInvitationService invitationService;

    @PostMapping("/organizations/{orgId}/invitations")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN', 'ORGANIZER')")
    public ResponseEntity<Void> inviteOrganizer(
            @PathVariable Long orgId,
            @Valid @RequestBody InviteOrganizerRequest request) {
        invitationService.inviteOrganizer(orgId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/invitations/approve")
    public ResponseEntity<String> approveInvitation(@RequestParam String token) {
        invitationService.approveInvitation(token);
        return ResponseEntity.ok("Invitation approved successfully. You are now an organizer.");
    }

    @GetMapping("/invitations/reject")
    public ResponseEntity<String> rejectInvitation(@RequestParam String token) {
        invitationService.rejectInvitation(token);
        return ResponseEntity.ok("Invitation rejected successfully.");
    }

    @DeleteMapping("/organizations/{orgId}/organizers/{organizerId}")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN', 'ORGANIZER')")
    public ResponseEntity<Void> removeOrganizer(
            @PathVariable Long orgId,
            @PathVariable Long organizerId) {
        invitationService.removeOrganizer(orgId, organizerId);
        return ResponseEntity.noContent().build();
    }
}
