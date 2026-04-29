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

    public Organization getOrganizationById(String organizationId) {
        return organizationRepository.findById(organizationId)
                .orElseThrow(() -> new ResourceNotFoundException("Organization not found with id: " + organizationId));
    }

    @Transactional
    public Organization updateOrganization(String organizationId, UpdateOrganizationRequest request) {
        Organization organization = getOrganizationById(organizationId);

        if (!organization.getId().equals(organizationId)) {
            throw new RuntimeException("Only organization owner can update");
        }


        if (request.getName() != null) {
            organization.setOrganizationName(request.getName());
        }

        if (request.getDescription() != null) {
            organization.setDescription(request.getDescription());
        }

        if (request.getContactEmail() != null) {
            organization.setEmail(request.getContactEmail());
        }

        if (request.getLogoUrl() != null) {
            organization.setLogoUrl(request.getLogoUrl());
        }

        if (request.getWebsite() != null) {
            organization.setWebsite(request.getWebsite());
        }

        Organization updated = organizationRepository.save(organization);
        log.info("Organization updated: {}", organizationId);
        return updated;
    }

    @Transactional
    public Organization updateOrganizationPhoto(String organizationId, UpdateProfilePhotoRequest request){

        Organization organization = getOrganizationById(organizationId);

        if(request.getImageUrl() !=null){
            organization.setLogoUrl(request.getImageUrl());
        }
        Organization updated = organizationRepository.save(organization);

        log.info("User profile photo updated: {}", organizationId);
        return updated;
    }

    @Transactional
    public void deleteOrganization(String organizationId) {
        Organization organization = getOrganizationById(organizationId);
        organizationRepository.delete(organization);
        log.info("Organization deleted: {}", organizationId);
    }

}
