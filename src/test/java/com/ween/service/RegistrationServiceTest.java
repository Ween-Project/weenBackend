package com.ween.service;

import com.ween.dto.response.EventResponse;
import com.ween.dto.response.ParticipantResponse;
import com.ween.entity.Event;
import com.ween.entity.EventRegistration;
import com.ween.entity.Participation;
import com.ween.entity.User;
import com.ween.enums.EventStatus;
import com.ween.enums.ParticipationStatus;
import com.ween.exception.*;
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.ParticipationRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RegistrationServiceTest {

    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @Mock private EventRepository eventRepository;
    @Mock private UserRepository userRepository;
    @Mock private NotificationService notificationService;
    @Mock private ChatService chatService;
    @Mock private CoinService coinService;
    @Mock private ParticipationRepository participationRepository;

    @InjectMocks
    private RegistrationService registrationService;

    @Test
    void registerThrowsExceptionWhenEventNotFound() {
        when(eventRepository.findById("event-123")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registrationService.registerForEvent("event-123", "user-1"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Event not found");
    }

    @Test
    void registerThrowsExceptionWhenAlreadyRegistered() {
        Event event = mock(Event.class);
        when(eventRepository.findById("event-123")).thenReturn(Optional.of(event));
        when(eventRegistrationRepository.findByEventIdAndUserId("event-123", "user-1"))
                .thenReturn(Optional.of(mock(EventRegistration.class)));

        assertThatThrownBy(() -> registrationService.registerForEvent("event-123", "user-1"))
                .isInstanceOf(AlreadyExistsException.class)
                .hasMessageContaining("User already registered");
    }

    @Test
    void registerThrowsExceptionWhenEventStarted() {
        Event event = mock(Event.class);
        when(event.getStartDate()).thenReturn(LocalDateTime.now().minusHours(1));
        when(eventRepository.findById("event-123")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> registrationService.registerForEvent("event-123", "user-1"))
                .isInstanceOf(RegistrationClosedException.class)
                .hasMessageContaining("Cannot register after the event has started");
    }

    @Test
    void registerThrowsExceptionWhenNotPublished() {
        Event event = mock(Event.class);
        when(event.getStartDate()).thenReturn(LocalDateTime.now().plusHours(1));
        when(event.getStatus()).thenReturn(EventStatus.DRAFT);
        when(eventRepository.findById("event-123")).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> registrationService.registerForEvent("event-123", "user-1"))
                .isInstanceOf(RegistrationClosedException.class)
                .hasMessageContaining("Cannot register for an event that is not published");
    }

    @Test
    void registerThrowsExceptionWhenCapacityExceeded() {
        Event event = mock(Event.class);
        when(event.getStartDate()).thenReturn(LocalDateTime.now().plusHours(1));
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getMaxParticipants()).thenReturn(10);
        when(eventRepository.findById("event-123")).thenReturn(Optional.of(event));

        when(eventRegistrationRepository.countByEventId("event-123")).thenReturn(10L);

        assertThatThrownBy(() -> registrationService.registerForEvent("event-123", "user-1"))
                .isInstanceOf(EventCapacityExceededException.class)
                .hasMessageContaining("Event is at maximum capacity");
    }

    @Test
    void registerCreatesRegistrationAndParticipationAndAddsToGroup() {
        Event event = mock(Event.class);
        when(event.getId()).thenReturn("event-123");
        when(event.getStartDate()).thenReturn(LocalDateTime.now().plusHours(1));
        when(event.getStatus()).thenReturn(EventStatus.PUBLISHED);
        when(event.getMaxParticipants()).thenReturn(10);
        when(eventRepository.findById("event-123")).thenReturn(Optional.of(event));

        when(eventRegistrationRepository.countByEventId("event-123")).thenReturn(5L);

        User user = mock(User.class);
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        EventRegistration registration = mock(EventRegistration.class);
        when(eventRegistrationRepository.save(any(EventRegistration.class))).thenReturn(registration);

        EventRegistration result = registrationService.registerForEvent("event-123", "user-1");

        verify(eventRegistrationRepository).save(any(EventRegistration.class));
        verify(participationRepository).save(any(Participation.class));
        verify(chatService).addUserToEventGroup("event-123", "user-1");
        verify(notificationService).createRegistrationNotification("user-1", "event-123");
        assertThat(result).isEqualTo(registration);
    }

    @Test
    void cancelRegistrationRemovesRegistrationAndParticipation() {
        EventRegistration registration = mock(EventRegistration.class);
        when(eventRegistrationRepository.findByEventIdAndUserId("event-123", "user-1"))
                .thenReturn(Optional.of(registration));

        Participation participation = mock(Participation.class);
        when(participationRepository.findByUserIdAndEventId("user-1", "event-123"))
                .thenReturn(Optional.of(participation));

        registrationService.cancelRegistration("event-123", "user-1");

        verify(eventRegistrationRepository).delete(registration);
        verify(participationRepository).delete(participation);
        verify(chatService).removeUserFromEventGroup("event-123", "user-1");
    }

    @Test
    void markUserAsJoinedAwardsBonus() {
        EventRegistration registration = mock(EventRegistration.class);
        when(eventRegistrationRepository.findByEventIdAndUserId("event-123", "user-1"))
                .thenReturn(Optional.of(registration));

        registrationService.markUserAsJoined("event-123", "user-1");

        verify(registration).setIsJoined(true);
        verify(eventRegistrationRepository).save(registration);
        verify(coinService).awardAttendanceBonus("user-1", "event-123");
    }
}
