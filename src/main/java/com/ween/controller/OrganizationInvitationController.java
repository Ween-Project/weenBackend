package com.ween.controller;

import com.ween.dto.request.InviteOrganizerRequest;
import com.ween.service.OrganizationInvitationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import com.ween.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Organization Invitations", description = "Endpoints for managing organization organizer invitations")
@SecurityRequirement(name = "Bearer")
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class OrganizationInvitationController {

    private final OrganizationInvitationService invitationService;

    @PostMapping("/organizations/{orgId}/invitations")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN', 'ORGANIZER')")
    @Operation(summary = "Invite an organizer", description = "Send an invitation to a user to become an organizer for this organization")
    public ResponseEntity<ApiResponse<Void>> inviteOrganizer(
            @PathVariable String orgId,
            @Valid @RequestBody InviteOrganizerRequest request) {
        invitationService.inviteOrganizer(orgId, request);
        return ResponseEntity.ok(ApiResponse.ok(null, "Invitation sent successfully"));
    }

    @GetMapping("/organizations/{orgId}/organizers")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN', 'ORGANIZER')")
    @Operation(summary = "Get organizers", description = "Get list of organizers for this organization")
    public ResponseEntity<ApiResponse<java.util.List<com.ween.dto.response.OrganizerResponse>>> getOrganizers(@PathVariable String orgId) {
        return ResponseEntity.ok(ApiResponse.ok(invitationService.getOrganizers(orgId), "Organizers retrieved successfully"));
    }

    @GetMapping("/invitations/approve")
    @Operation(summary = "Approve an invitation", description = "Approve an organization invitation using the provided token")
    public ResponseEntity<ApiResponse<Void>> approveInvitation(@RequestParam String token) {
        invitationService.approveInvitation(token);
        return ResponseEntity.ok(ApiResponse.ok(null, "Invitation approved successfully. You are now an organizer."));
    }

    @GetMapping("/invitations/reject")
    @Operation(summary = "Reject an invitation", description = "Reject an organization invitation using the provided token")
    public ResponseEntity<ApiResponse<Void>> rejectInvitation(@RequestParam String token) {
        invitationService.rejectInvitation(token);
        return ResponseEntity.ok(ApiResponse.ok(null, "Invitation rejected successfully."));
    }

    @DeleteMapping("/organizations/{orgId}/organizers/{organizerId}")
    @PreAuthorize("hasAnyRole('ORGANIZATION_ADMIN', 'ORGANIZER')")
    @Operation(summary = "Remove an organizer", description = "Remove an existing organizer from the organization")
    public ResponseEntity<ApiResponse<Void>> removeOrganizer(
            @PathVariable String orgId,
            @PathVariable String organizerId) {
        invitationService.removeOrganizer(orgId, organizerId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Organizer removed successfully"));
    }
}
