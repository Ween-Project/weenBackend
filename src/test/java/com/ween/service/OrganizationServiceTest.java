package com.ween.service;

import com.ween.dto.request.UpdateOrganizationRequest;
import com.ween.dto.request.UpdateProfilePhotoRequest;
import com.ween.entity.Organization;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.OrganizationMapper;
import com.ween.repository.EventRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrganizationServiceTest {

    @Mock private OrganizationRepository organizationRepository;
    @Mock private OrganizationMapper organizationMapper;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @InjectMocks private OrganizationService organizationService;

    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testOrg = Organization.builder().username("org").email("o@e.com")
                .passwordHash("p").organizationName("TestOrg").description("Desc").build();
        testOrg.setId("oid");
    }

    @Test @DisplayName("Get organization by id – found")
    void getById_found() {
        when(organizationRepository.findById("oid")).thenReturn(Optional.of(testOrg));
        assertThat(organizationService.getOrganizationById("oid").getOrganizationName()).isEqualTo("TestOrg");
    }

    @Test @DisplayName("Get organization by id – not found throws")
    void getById_notFound() {
        when(organizationRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> organizationService.getOrganizationById("x"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Update organization – all fields")
    void updateOrganization_allFields() {
        UpdateOrganizationRequest req = new UpdateOrganizationRequest();
        req.setName("NewName"); req.setDescription("NewDesc");
        req.setContactEmail("new@e.com"); req.setLogoUrl("http://logo"); req.setWebsite("http://web");

        when(organizationRepository.findById("oid")).thenReturn(Optional.of(testOrg));
        when(organizationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Organization result = organizationService.updateOrganization("oid", req);
        assertThat(result.getOrganizationName()).isEqualTo("NewName");
        assertThat(result.getDescription()).isEqualTo("NewDesc");
        assertThat(result.getEmail()).isEqualTo("new@e.com");
        assertThat(result.getLogoUrl()).isEqualTo("http://logo");
        assertThat(result.getWebsite()).isEqualTo("http://web");
    }

    @Test @DisplayName("Update organization – partial fields (nulls ignored)")
    void updateOrganization_partialFields() {
        UpdateOrganizationRequest req = new UpdateOrganizationRequest();
        req.setName("NewName"); // only name

        when(organizationRepository.findById("oid")).thenReturn(Optional.of(testOrg));
        when(organizationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Organization result = organizationService.updateOrganization("oid", req);
        assertThat(result.getOrganizationName()).isEqualTo("NewName");
        assertThat(result.getDescription()).isEqualTo("Desc"); // unchanged
    }

    @Test @DisplayName("Update organization photo")
    void updateOrganizationPhoto() {
        UpdateProfilePhotoRequest req = new UpdateProfilePhotoRequest();
        req.setImageUrl("http://new-logo");

        when(organizationRepository.findById("oid")).thenReturn(Optional.of(testOrg));
        when(organizationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Organization result = organizationService.updateOrganizationPhoto("oid", req);
        assertThat(result.getLogoUrl()).isEqualTo("http://new-logo");
    }

    @Test @DisplayName("Delete organization")
    void deleteOrganization() {
        when(organizationRepository.findById("oid")).thenReturn(Optional.of(testOrg));
        organizationService.deleteOrganization("oid");
        verify(organizationRepository).delete(testOrg);
    }
}
