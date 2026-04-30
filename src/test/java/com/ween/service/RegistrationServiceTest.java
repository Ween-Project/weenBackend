package com.ween.service;

import com.ween.entity.*;
import com.ween.enums.CoinReason;
import com.ween.exception.*;
import com.ween.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.*;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private CoinService coinService;
    @Mock private NotificationService notificationService;
    @InjectMocks private RegistrationService registrationService;

    private Event testEvent;

    @BeforeEach
    void setUp() {
        testEvent = Event.builder().title("Ev").organizationId("org1")
                .maxParticipants(100).startDate(LocalDateTime.now().plusDays(7))
                .endDate(LocalDateTime.now().plusDays(8)).build();
        testEvent.setId("eid");
    }

    @Test @DisplayName("Register for event – success")
    void registerForEvent_success() {
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.empty());
        when(eventRegistrationRepository.countByEventId("eid")).thenReturn(10L);
        when(eventRegistrationRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        EventRegistration reg = registrationService.registerForEvent("eid", "uid");
        assertThat(reg.getEventId()).isEqualTo("eid");
        assertThat(reg.getUserId()).isEqualTo("uid");
        verify(coinService).awardEventRegistrationBonus("uid", "eid");
    }

    @Test @DisplayName("Register – already registered throws")
    void registerForEvent_alreadyRegistered() {
        EventRegistration existing = EventRegistration.builder().eventId("eid").userId("uid").build();
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.of(existing));
        assertThatThrownBy(() -> registrationService.registerForEvent("eid", "uid"))
                .isInstanceOf(AlreadyExistsException.class);
    }

    @Test @DisplayName("Register – capacity full throws")
    void registerForEvent_capacityFull() {
        testEvent.setMaxParticipants(5);
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.empty());
        when(eventRegistrationRepository.countByEventId("eid")).thenReturn(5L);
        assertThatThrownBy(() -> registrationService.registerForEvent("eid", "uid"))
                .isInstanceOf(EventCapacityExceededException.class);
    }

    @Test @DisplayName("Register – deadline passed throws")
    void registerForEvent_deadlinePassed() {
        testEvent.setRegistrationDeadline(LocalDateTime.now().minusDays(1));
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.empty());
        when(eventRegistrationRepository.countByEventId("eid")).thenReturn(0L);
        assertThatThrownBy(() -> registrationService.registerForEvent("eid", "uid"))
                .isInstanceOf(RuntimeException.class).hasMessageContaining("deadline");
    }

    @Test @DisplayName("Cancel registration – success")
    void cancelRegistration_success() {
        EventRegistration reg = EventRegistration.builder().eventId("eid").userId("uid").build();
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.of(reg));
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));

        registrationService.cancelRegistration("eid", "uid");
        verify(eventRegistrationRepository).delete(reg);
    }

    @Test @DisplayName("Cancel registration – not registered throws")
    void cancelRegistration_notRegistered() {
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> registrationService.cancelRegistration("eid", "uid"))
                .isInstanceOf(EventNotRegisteredException.class);
    }

    @Test @DisplayName("Cancel registration – after event start debits coins")
    void cancelRegistration_afterStart_debits() {
        testEvent.setStartDate(LocalDateTime.now().minusDays(1));
        EventRegistration reg = EventRegistration.builder().eventId("eid").userId("uid").build();
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.of(reg));
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));

        registrationService.cancelRegistration("eid", "uid");
        verify(coinService).debit("uid", 25, CoinReason.REGISTRATION, "eid");
    }

    @Test @DisplayName("Mark user as joined – success")
    void markUserAsJoined_success() {
        EventRegistration reg = EventRegistration.builder().eventId("eid").userId("uid").isJoined(false).build();
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.of(reg));
        when(eventRegistrationRepository.save(any())).thenReturn(reg);

        registrationService.markUserAsJoined("eid", "uid");
        assertThat(reg.getIsJoined()).isTrue();
        verify(coinService).awardAttendanceBonus("uid", "eid");
    }

    @Test @DisplayName("Mark user as joined – not registered throws")
    void markUserAsJoined_notRegistered() {
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> registrationService.markUserAsJoined("eid", "uid"))
                .isInstanceOf(EventNotRegisteredException.class);
    }

    @Test @DisplayName("Cancel all registrations for event")
    void cancelAllRegistrations() {
        List<EventRegistration> regs = List.of(
                EventRegistration.builder().eventId("eid").userId("u1").build(),
                EventRegistration.builder().eventId("eid").userId("u2").build()
        );
        when(eventRegistrationRepository.findByEventId("eid")).thenReturn(regs);
        registrationService.cancelAllRegistrationsForEvent("eid");
        verify(eventRegistrationRepository).deleteAll(regs);
    }

    @Test @DisplayName("Get event registration count")
    void getEventRegistrationCount() {
        when(eventRegistrationRepository.countByEventId("eid")).thenReturn(42L);
        assertThat(registrationService.getEventRegistrationCount("eid")).isEqualTo(42L);
    }

    @Test @DisplayName("Get event joined count")
    void getEventJoinedCount() {
        when(eventRegistrationRepository.countByEventIdAndIsJoinedTrue("eid")).thenReturn(10L);
        assertThat(registrationService.getEventJoinedCount("eid")).isEqualTo(10L);
    }

    @Test @DisplayName("Get user events – maps registrations to event responses")
    void getUserEvents() {
        EventRegistration reg = EventRegistration.builder().eventId("eid").userId("uid").build();
        when(eventRegistrationRepository.findByUserId("uid")).thenReturn(List.of(reg));
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));

        Page<com.ween.dto.response.EventResponse> result =
                registrationService.getUserEvents("uid", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
    }

    @Test @DisplayName("Get event participants")
    void getEventParticipants() {
        EventRegistration reg = EventRegistration.builder().eventId("eid").userId("uid")
                .registeredAt(LocalDateTime.now()).isJoined(false).build();
        User u = User.builder().username("u").fullName("F").weenCoinBalance(0).build();
        u.setId("uid");

        when(eventRegistrationRepository.findByEventId("eid")).thenReturn(List.of(reg));
        when(userRepository.findById("uid")).thenReturn(Optional.of(u));

        var result = registrationService.getEventParticipants("org", "eid", PageRequest.of(0, 10));
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getUsername()).isEqualTo("u");
    }
}
