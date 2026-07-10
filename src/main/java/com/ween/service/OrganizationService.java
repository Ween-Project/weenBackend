package com.ween.service;

import com.ween.dto.request.CreateOrganizationRequest;
import com.ween.dto.request.UpdateOrganizationRequest;
import com.ween.dto.request.UpdateProfilePhotoRequest;
import com.ween.entity.Event;
import com.ween.entity.Organization;
import com.ween.entity.User;
import com.ween.enums.EventStatus;
import com.ween.exception.AlreadyExistsException;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.OrganizationMapper;
import com.ween.repository.EventRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class OrganizationService {

    private final OrganizationRepository organizationRepository;
    private final OrganizationMapper organizationMapper;
    private final UserRepository userRepository;
    private final EventRepository eventRepository;
    private final CloudinaryService cloudinaryService;

    public Organization getOrganizationById(String organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));
    }

    @Transactional
    public Organization updateOrganization(String organizationId, UpdateOrganizationRequest request, org.springframework.web.multipart.MultipartFile logo) {
        Organization organization = getOrganizationById(organizationId);

        if (!organization.getId().equals(organizationId)) {
            throw new RuntimeException("Only organization owner can update");
        }


        if (request.getOrganizationName() != null) {
            organization.setOrganizationName(request.getOrganizationName());
        }
        if (request.getDescription() != null) {
            organization.setDescription(request.getDescription());
        }
        if (request.getWebsite() != null) {
            organization.setWebsite(request.getWebsite());
        }
        if (request.getEmail() != null) {
            organization.setEmail(request.getEmail());
        }

        if (logo != null && !logo.isEmpty()) {
            try {
                String logoUrl = cloudinaryService.uploadFile(logo, "organizations/logos");
                organization.setLogoUrl(logoUrl);
            } catch (java.io.IOException e) {
                log.error("Failed to upload organization logo to Cloudinary", e);
                throw new RuntimeException("Logo upload failed", e);
            }
        }

        if (request.getWebsite() != null) {
            organization.setWebsite(request.getWebsite());
        }

        Organization updated = organizationRepository.save(organization);
        log.info("Organization updated: {}", organizationId);
        return updated;
    }



    @Transactional
    public void deleteOrganization(String organizationId) {
        Organization organization = getOrganizationById(organizationId);
        organizationRepository.delete(organization);
        log.info("Organization deleted: {}", organizationId);
    }

}
