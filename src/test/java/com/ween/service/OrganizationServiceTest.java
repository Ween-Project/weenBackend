package com.ween.service;

import com.ween.entity.Organization;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.OrganizationMapper;
import com.ween.repository.EventRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock OrganizationRepository organizationRepository;
    @Mock OrganizationMapper organizationMapper;
    @Mock UserRepository userRepository;
    @Mock EventRepository eventRepository;
    @InjectMocks OrganizationService organizationService;

    @Test
    void getOrganizationByIdReturnsExistingOrganization() {
        Organization organization = Organization.builder().username("org").build();
        organization.setId("org-1");
        when(organizationRepository.findById("org-1")).thenReturn(Optional.of(organization));

        assertThat(organizationService.getOrganizationById("org-1")).isSameAs(organization);
    }

    @Test
    void getOrganizationByIdFailsWhenMissing() {
        when(organizationRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> organizationService.getOrganizationById("missing"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
