package com.ween.service;

import com.ween.dto.request.CreateEventRequest;
import com.ween.dto.request.UpdateEventRequest;
import com.ween.dto.response.EventDetailResponse;
import com.ween.dto.response.EventResponse;
import com.ween.dto.response.EventStatsResponse;
import com.ween.entity.Event;
import com.ween.entity.Organization;
import com.ween.enums.EventCategory;
import com.ween.enums.EventStatus;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.EventMapper;
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.OrganizationRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private OrganizationRepository organizationRepository;
    @Mock private EventRegistrationRepository registrationRepository;
    @Mock private EventMapper eventMapper;
    @Mock private OrganizationService organizationService;
    @Mock private RegistrationService registrationService;
    @InjectMocks private EventService eventService;

    private Event testEvent;
    private Organization testOrg;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder().title("Test Event").description("Desc")
                .category(EventCategory.TECHNOLOGY).city("Baku").organizationId("org1")
                .status(EventStatus.DRAFT).maxParticipants(100)
                .startDate(LocalDateTime.now().plusDays(7))
                .endDate(LocalDateTime.now().plusDays(8)).build();
        testEvent.setId("eid");

        testOrg = Organization.builder().username("org").email("o@e.com")
                .passwordHash("p").organizationName("TestOrg").build();
        testOrg.setId("org1");
    }

    @Test @DisplayName("Create event – success")
    void createEvent_success() {
        CreateEventRequest req = new CreateEventRequest();
        req.setTitle("New"); req.setDescription("D"); req.setCategory(EventCategory.EDUCATION);

        when(organizationService.getOrganizationById("org1")).thenReturn(testOrg);
        when(eventRepository.save(any())).thenAnswer(i -> { Event e=i.getArgument(0); e.setId("nid"); return e; });

        Event result = eventService.createEvent(req, "org1");
        assertThat(result.getTitle()).isEqualTo("New");
        assertThat(result.getStatus()).isEqualTo(EventStatus.DRAFT);
    }

    @Test @DisplayName("Get event by id – found")
    void getEventById_found() {
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        assertThat(eventService.getEventById("eid").getTitle()).isEqualTo("Test Event");
    }

    @Test @DisplayName("Get event by id – not found throws")
    void getEventById_notFound() {
        when(eventRepository.findById("x")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> eventService.getEventById("x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Update event – partial fields")
    void updateEvent_partial() {
        UpdateEventRequest req = new UpdateEventRequest();
        req.setTitle("Updated"); req.setCity("Istanbul");

        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        Event result = eventService.updateEvent("eid", "org1", req);
        assertThat(result.getTitle()).isEqualTo("Updated");
        assertThat(result.getCity()).isEqualTo("Istanbul");
        assertThat(result.getDescription()).isEqualTo("Desc"); // unchanged
    }

    @Test @DisplayName("Publish event – sets status to PUBLISHED")
    void publishEvent() {
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any())).thenReturn(testEvent);
        eventService.publishEvent("eid");
        assertThat(testEvent.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    }

    @Test @DisplayName("Start event – sets status to ONGOING")
    void startEvent() {
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any())).thenReturn(testEvent);
        eventService.startEvent("eid");
        assertThat(testEvent.getStatus()).isEqualTo(EventStatus.ONGOING);
    }

    @Test @DisplayName("Complete event – sets status to COMPLETED")
    void completeEvent() {
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(eventRepository.save(any())).thenReturn(testEvent);
        eventService.completeEvent("eid");
        assertThat(testEvent.getStatus()).isEqualTo(EventStatus.COMPLETED);
    }

    @Test @DisplayName("Cancel event – by owner deletes event")
    void cancelEvent_byOwner() {
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        eventService.cancelEvent("eid", "org1");
        verify(registrationService).cancelAllRegistrationsForEvent("eid");
        verify(eventRepository).delete(testEvent);
    }

    @Test @DisplayName("Cancel event – by non-owner throws")
    void cancelEvent_nonOwner() {
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        assertThatThrownBy(() -> eventService.cancelEvent("eid", "other-user"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test @DisplayName("Delete event – success")
    void deleteEvent() {
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        eventService.deleteEvent("eid");
        verify(eventRepository).delete(testEvent);
    }

    @Test @DisplayName("isEventCapacityFull – no limit returns false")
    void isCapacityFull_noLimit() {
        testEvent.setMaxParticipants(null);
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        assertThat(eventService.isEventCapacityFull("eid")).isFalse();
    }

    @Test @DisplayName("isRegistrationDeadlinePassed – future deadline returns false")
    void isDeadlinePassed_future() {
        testEvent.setRegistrationDeadline(LocalDateTime.now().plusDays(1));
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        assertThat(eventService.isRegistrationDeadlinePassed("eid")).isFalse();
    }

    @Test @DisplayName("isRegistrationDeadlinePassed – past deadline returns true")
    void isDeadlinePassed_past() {
        testEvent.setRegistrationDeadline(LocalDateTime.now().minusDays(1));
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        assertThat(eventService.isRegistrationDeadlinePassed("eid")).isTrue();
    }

    @Test @DisplayName("isRegistrationDeadlinePassed – null deadline returns false")
    void isDeadlinePassed_null() {
        testEvent.setRegistrationDeadline(null);
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        assertThat(eventService.isRegistrationDeadlinePassed("eid")).isFalse();
    }

    @Test @DisplayName("Get event detail – includes registration counts")
    void getEventDetail() {
        EventDetailResponse detailResp = new EventDetailResponse();
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(eventMapper.toEventDetailResponse(testEvent)).thenReturn(detailResp);
        when(registrationService.getEventRegistrationCount("eid")).thenReturn(10L);
        when(registrationService.getEventJoinedCount("eid")).thenReturn(5L);
        when(organizationService.getOrganizationById("org1")).thenReturn(testOrg);

        EventDetailResponse result = eventService.getEventDetail("eid");
        assertThat(result.getCurrentRegistrations()).isEqualTo(10);
        assertThat(result.getAttendeeCount()).isEqualTo(5);
        assertThat(result.getOrganizationName()).isEqualTo("TestOrg");
    }

    @Test @DisplayName("Get event stats")
    void getEventStats() {
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(registrationService.getEventRegistrationCount("eid")).thenReturn(50L);
        when(registrationService.getEventJoinedCount("eid")).thenReturn(30L);

        EventStatsResponse stats = eventService.getEventStats("org1", "eid");
        assertThat(stats.getTotalRegistered()).isEqualTo(50);
        assertThat(stats.getTotalAttended()).isEqualTo(30);
        assertThat(stats.getRegistrationRate()).isEqualTo(50); // 50*100/100
        assertThat(stats.getAttendanceRate()).isEqualTo(60); // 30*100/50
    }

    @Test @DisplayName("Get organization events list")
    void getOrganizationEventsList() {
        when(organizationRepository.findById("org1")).thenReturn(Optional.of(testOrg));
        when(eventRepository.findByOrganizationId("org1")).thenReturn(List.of(testEvent));
        when(registrationService.getEventRegistrationCount("eid")).thenReturn(5L);

        List<EventResponse> result = eventService.getOrganizationEventsList("org1");
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getOrganizationName()).isEqualTo("TestOrg");
        assertThat(result.get(0).getCurrentRegistrations()).isEqualTo(5);
    }

    @Test @DisplayName("getRemainingCapacity – no limit returns MAX_VALUE")
    void remainingCapacity_noLimit() {
        testEvent.setMaxParticipants(null);
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        assertThat(eventService.getRemainingCapacity("eid")).isEqualTo(Integer.MAX_VALUE);
    }

    @Test @DisplayName("isEventInFuture – future event returns true")
    void isEventInFuture_true() {
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        assertThat(eventService.isEventInFuture("eid")).isTrue();
    }
}
