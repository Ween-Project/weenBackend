package com.ween.service;

import com.ween.dto.response.EventResponse;
import com.ween.dto.response.ParticipantResponse;
import com.ween.entity.Event;
import com.ween.entity.EventRegistration;
import com.ween.entity.Participation;
import com.ween.entity.User;
import com.ween.enums.ParticipationStatus;
import com.ween.exception.*;
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.ParticipationRepository;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class RegistrationService {

    private final EventRegistrationRepository eventRegistrationRepository;
    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;
    private final ChatService chatService;
    private final CoinService coinService;
    private final ParticipationRepository participationRepository;

@Transactional
public EventRegistration registerForEvent(String eventId, String userId) {
    Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + eventId));

    // Check if already registered
    if (eventRegistrationRepository.findByEventIdAndUserId(eventId, userId).isPresent()) {
        throw new AlreadyExistsException("User already registered for this event");
    }

    if (event.getStartDate() != null && LocalDateTime.now().isAfter(event.getStartDate())) {
        throw new RegistrationClosedException("Cannot register after the event has started");
    }

    if (event.getStatus() != com.ween.enums.EventStatus.PUBLISHED) {
        throw new RegistrationClosedException("Cannot register for an event that is not published");
    }

    // Check capacity
    long registrationCount = eventRegistrationRepository.countByEventId(eventId);
    if (event.getMaxParticipants() != null && registrationCount >= event.getMaxParticipants()) {
        throw new EventCapacityExceededException("Event is at maximum capacity");
    }

    if (event.getRegistrationDeadline() != null && LocalDateTime.now().isAfter(event.getRegistrationDeadline())) {
        throw new RegistrationClosedException("Registration deadline has passed");
    }

    EventRegistration registration = EventRegistration.builder()
            .eventId(eventId)
            .userId(userId)
            .registeredAt(LocalDateTime.now())
            .isJoined(false)
            .build();

    EventRegistration saved = eventRegistrationRepository.save(registration);
    log.info("User {} registered for event: {}", userId, eventId);

    User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    Participation participation = Participation.builder()
            .user(user)
            .event(event)
            .status(ParticipationStatus.JOINED)
            .joinedAt(LocalDateTime.now())
            .build();
    participationRepository.save(participation);

    // Add user to Event Group Chat
    try {
        chatService.addUserToEventGroup(eventId, userId);
    } catch (Exception e) {
        log.warn("Failed to add user to event group chat", e);
    }

    // Send notification
    try {
        notificationService.createRegistrationNotification(userId, event.getId());
    } catch (Exception e) {
        log.warn("Failed to create registration notification", e);
    }

    return saved;
}

    @Transactional
    public void cancelRegistration(String eventId, String userId) {
        EventRegistration registration = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new EventNotRegisteredException("User not registered for this event"));

        eventRegistrationRepository.delete(registration);
        log.info("User {} cancelled registration for event: {}", userId, eventId);

        // Debit coins removed
    }

    @Transactional
    public void markUserAsJoined(String eventId, String userId) {
        EventRegistration registration = eventRegistrationRepository.findByEventIdAndUserId(eventId, userId)
                .orElseThrow(() -> new EventNotRegisteredException("User not registered for this event"));

        registration.setIsJoined(true);
        registration.setJoinedAt(LocalDateTime.now());
        eventRegistrationRepository.save(registration);
        log.info("User marked as joined for event: {}", eventId);
        coinService.awardAttendanceBonus(userId, eventId);
    }

    public List<EventRegistration> getEventRegistrations(String eventId) {
        return eventRegistrationRepository.findByEventId(eventId);
    }

    public List<EventRegistration> getUserRegistrations(String userId) {
        return eventRegistrationRepository.findByUserId(userId);
    }

    public long getEventRegistrationCount(String eventId) {
        return eventRegistrationRepository.countByEventId(eventId);
    }

    public long getEventJoinedCount(String eventId) {
        return eventRegistrationRepository.countByEventIdAndIsJoinedTrue(eventId);
    }

    @Transactional
    public void cancelAllRegistrationsForEvent(String eventId) {
        List<EventRegistration> registrations = eventRegistrationRepository.findByEventId(eventId);
        eventRegistrationRepository.deleteAll(registrations);

        log.info("All registrations cancelled for event: {}", eventId);
    }

    public Page<EventResponse> getUserEvents(String userId, Pageable pageable) {
        List<EventRegistration> registrations = getUserRegistrations(userId);
        List<EventResponse> eventResponses = registrations.stream()
                .map(reg -> eventRepository.findById(reg.getEventId())
                        .map(event -> EventResponse.builder()
                                .id(event.getId())
                                .title(event.getTitle())
                                .description(event.getDescription())
                                .category(event.getCategory())
                                .city(event.getCity())
                                .address(event.getAddress())
                                .isOnline(event.getIsOnline())
                                .startDate(event.getStartDate())
                                .endDate(event.getEndDate())
                                .maxParticipants(event.getMaxParticipants())
                                .build())
                        .orElse(null))
                .filter(r -> r != null)
                .collect(java.util.stream.Collectors.toList());
        return new PageImpl<>(eventResponses, pageable, eventResponses.size());
    }

    public Page<ParticipantResponse> getEventParticipants(String userId, String eventId, Pageable pageable) {
        List<EventRegistration> registrations = getEventRegistrations(eventId);
        List<ParticipantResponse> participants = registrations.stream()
                .map(reg -> userRepository.findById(reg.getUserId())
                        .map(user -> ParticipantResponse.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .fullName(user.getFullName())
                                .profilePhotoUrl(user.getProfilePhotoUrl())
                                .weenCoinBalance(user.getWeenCoinBalance())
                                .registeredAt(reg.getRegisteredAt())
                                .joinedAt(reg.getJoinedAt())
                                .isJoined(reg.getIsJoined())
                                .build())
                        .orElse(null))
                .filter(p -> p != null)
                .collect(java.util.stream.Collectors.toList());
        return new PageImpl<>(participants, pageable, participants.size());
    }
}