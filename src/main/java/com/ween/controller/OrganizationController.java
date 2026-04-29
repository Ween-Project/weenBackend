package com.ween.controller;


import com.ween.dto.request.CreateOrganizationRequest;
import com.ween.dto.request.UpdateOrganizationProfileRequest;
import com.ween.dto.request.UpdateOrganizationRequest;
import com.ween.dto.request.UpdateProfilePhotoRequest;
import com.ween.dto.response.*;
import com.ween.entity.Organization;
import com.ween.security.SecurityUtil;
import com.ween.service.EventService;
import com.ween.service.OrganizationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/organizations")
@RequiredArgsConstructor
@Tag(name = "Organizations", description = "Organization management endpoints")
public class OrganizationController {

    private final SecurityUtil securityUtil;
    private final EventService eventService;
    private final OrganizationService organizationService;


    @GetMapping("/{id}")
    @Operation(summary = "Get current organization profile", description = "Retrieve detailed information about an organization")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<ApiResponse<Organization>> getOrganization() {
        String orgId = null;
        try {
            orgId = securityUtil.getCurrentUserId();
            Organization response = organizationService.getOrganizationById(orgId);
            return ResponseEntity.ok(ApiResponse.ok(response, "Organization retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve organization: {}", orgId, e);
            throw e;
        }
    }

    @PutMapping("/{id}")
    @Transactional
    @Operation(summary = "Update current organization profile", description = "Update organization details (owner only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Only owner can update"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<ApiResponse<Organization>> updateOrganization(
            @Parameter(description = "Organization ID", required = true)
            @Valid @RequestBody UpdateOrganizationRequest request) {
        String orgId = null;
        try {
            orgId = securityUtil.getCurrentUserId();
            Organization response = organizationService.updateOrganization(orgId, request);
            return ResponseEntity.ok(ApiResponse.ok(response, "Organization updated successfully"));
        } catch (Exception e) {
            log.error("Failed to update organization: {}", orgId, e);
            throw e;
        }
    }


    @PutMapping("/{id}/logo")
    @Transactional
    @Operation(summary = "Update current organization logo", description = "Update organization logo (owner only)")
    @SecurityRequirement(name = "Bearer")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Organization updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Only owner can update"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    })
    public ResponseEntity<ApiResponse<Organization>> updateOrganizationLogo(
            @Parameter(description = "Organization ID", required = true)
            @Valid @RequestBody UpdateProfilePhotoRequest request) {
        String organizationId=null;
        try {
            organizationId = securityUtil.getCurrentUserId();
            Organization response = organizationService.updateOrganizationPhoto(organizationId,request);
            return ResponseEntity.ok(ApiResponse.ok(response, "Organization logo updated successfully"));
        } catch (Exception e) {
            log.error("Failed to update organization: {}", organizationId, e);
            throw e;
        }
    }


    @GetMapping("/current-organization-events")
    @Operation(
            summary = "Get current organization events",
            description = "Retrieve all events for the authenticated organization using JWT token"
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Events retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized - Token is missing or invalid"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Organization not found")
    })
    @SecurityRequirement(name = "Bearer")
    public ResponseEntity<ApiResponse<List<EventResponse>>> getOrganizationEvents() {
        String orgId = null;
        try {
            orgId = securityUtil.getCurrentUserId();

            List<EventResponse> response = eventService.getOrganizationEventsList(orgId);

            return ResponseEntity.ok(ApiResponse.ok(response, "Events retrieved successfully"));
        } catch (Exception e) {
            log.error("Failed to retrieve events for organization: {}", orgId, e);
            throw e;
        }
    }

}
