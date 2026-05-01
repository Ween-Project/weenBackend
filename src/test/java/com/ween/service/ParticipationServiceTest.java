package com.ween.service;

import com.ween.entity.*;
import com.ween.enums.EventCategory;
import com.ween.enums.ParticipationStatus;
import com.ween.exception.EventNotRegisteredException;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ParticipationServiceTest {

    @Mock private ParticipationRepository participationRepository;
    @Mock private UserRepository userRepository;
    @Mock private EventRepository eventRepository;
    @Mock private CertificateTriggerService certificateTriggerService;
    @Mock private EventRegistrationRepository eventRegistrationRepository;
    @InjectMocks private ParticipationService participationService;

    private User testUser;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        testUser = User.builder().username("u").email("u@e.com").passwordHash("p").fullName("U").build();
        testUser.setId("uid");
        testEvent = Event.builder().title("Ev").organizationId("org1").category(EventCategory.EDUCATION).build();
        testEvent.setId("eid");
    }

    // ── joinEvent ───────────────────────────────────────────────────────

    @Test @DisplayName("Join event – success")
    void joinEvent_success() {
        EventRegistration reg = EventRegistration.builder().eventId("eid").userId("uid").build();
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.of(reg));
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById("eid")).thenReturn(Optional.of(testEvent));
        when(participationRepository.save(any(Participation.class))).thenAnswer(i -> i.getArgument(0));

        participationService.joinEvent("uid", "eid");

        ArgumentCaptor<Participation> captor = ArgumentCaptor.forClass(Participation.class);
        verify(participationRepository).save(captor.capture());

        Participation saved = captor.getValue();
        assertThat(saved.getUser()).isEqualTo(testUser);
        assertThat(saved.getEvent()).isEqualTo(testEvent);
        assertThat(saved.getStatus()).isEqualTo(ParticipationStatus.JOINED);
        assertThat(saved.getJoinedAt()).isNotNull();
    }

    @Test @DisplayName("Join event – not registered throws EventNotRegisteredException")
    void joinEvent_notRegistered() {
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.joinEvent("uid", "eid"))
                .isInstanceOf(EventNotRegisteredException.class);
        verifyNoInteractions(participationRepository);
    }

    @Test @DisplayName("Join event – user not found throws ResourceNotFoundException")
    void joinEvent_userNotFound() {
        EventRegistration reg = EventRegistration.builder().eventId("eid").userId("uid").build();
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.of(reg));
        when(userRepository.findById("uid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.joinEvent("uid", "eid"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test @DisplayName("Join event – event not found throws ResourceNotFoundException")
    void joinEvent_eventNotFound() {
        EventRegistration reg = EventRegistration.builder().eventId("eid").userId("uid").build();
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.of(reg));
        when(userRepository.findById("uid")).thenReturn(Optional.of(testUser));
        when(eventRepository.findById("eid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.joinEvent("uid", "eid"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ── completeParticipation ───────────────────────────────────────────

    @Test @DisplayName("Complete participation – success and triggers certificate")
    void completeParticipation_success() {
        EventRegistration reg = EventRegistration.builder().eventId("eid").userId("uid").build();
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.of(reg));

        Participation participation = Participation.builder()
                .user(testUser)
                .event(testEvent)
                .status(ParticipationStatus.JOINED)
                .joinedAt(LocalDateTime.now())
                .build();
        participation.setId("pid");
        when(participationRepository.findByUserIdAndEventId("uid", "eid")).thenReturn(Optional.of(participation));
        when(participationRepository.save(any(Participation.class))).thenAnswer(i -> i.getArgument(0));

        participationService.completeParticipation("uid", "eid");

        assertThat(participation.getStatus()).isEqualTo(ParticipationStatus.FINISHED);
        verify(participationRepository).save(participation);
        verify(certificateTriggerService).autoGenerateCertificateRecord("uid", "eid", EventCategory.EDUCATION);
    }

    @Test @DisplayName("Complete participation – not registered throws EventNotRegisteredException")
    void completeParticipation_notRegistered() {
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.completeParticipation("uid", "eid"))
                .isInstanceOf(EventNotRegisteredException.class);
        verifyNoInteractions(certificateTriggerService);
    }

    @Test @DisplayName("Complete participation – participation record not found throws")
    void completeParticipation_participationNotFound() {
        EventRegistration reg = EventRegistration.builder().eventId("eid").userId("uid").build();
        when(eventRegistrationRepository.findByEventIdAndUserId("eid", "uid")).thenReturn(Optional.of(reg));
        when(participationRepository.findByUserIdAndEventId("uid", "eid")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> participationService.completeParticipation("uid", "eid"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
