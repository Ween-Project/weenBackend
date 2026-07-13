package com.ween.service;

import com.ween.dto.request.CreateEventRequest;

import com.ween.dto.request.CreateEventRequest;
import com.ween.dto.request.UpdateEventRequest;
import com.ween.dto.response.EventDetailResponse;
import com.ween.dto.response.EventResponse;
import com.ween.dto.response.EventStatsResponse;
import com.ween.entity.Event;
import com.ween.entity.User;
import com.ween.entity.Organization;
import com.ween.enums.EventCategory;
import com.ween.enums.EventStatus;
import com.ween.enums.UserRole;
import com.ween.enums.ParticipationStatus;
import com.ween.entity.Participation;
import com.ween.exception.ResourceNotFoundException;
import com.ween.exception.ServiceUnavailableException;
import com.ween.mapper.EventMapper;
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.UserRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.OrganizerRepository;
import com.ween.repository.ParticipationRepository;
import com.ween.repository.CertificateRepository;
import com.ween.repository.ChatRoomRepository;
import com.ween.repository.ChatRoomMemberRepository;
import com.ween.repository.GroupChatMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventMapper eventMapper;
    private final OrganizationService organizationService;
    private final OrganizerRepository organizerRepository;
    private final RegistrationService registrationService;
    private final ParticipationRepository participationRepository;
    private final CertificateRepository certificateRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final GroupChatMessageRepository groupChatMessageRepository;
    private final CertificateService certificateService;
    private final ChatService chatService;
    private final CloudinaryService cloudinaryService;

    @Transactional
    public Event createEvent(CreateEventRequest request, String organizationId) {
        return createEvent(request, organizationId, null);
    }

    @Transactional
    public Event createEvent(CreateEventRequest request, String organizationId, MultipartFile coverImage) {
        Organization organization = organizationService.getOrganizationById(organizationId);
        if (!Boolean.TRUE.equals(organization.getIsVerified())) {
            throw new AccessDeniedException("Your organization must be approved by a super admin before publishing events");
        }

        String coverImageUrl = null;
        if (coverImage != null && !coverImage.isEmpty()) {
            coverImageUrl = uploadEventCover(coverImage);
        }

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .city(request.getCity())
                .address(request.getAddress())
                .coverImageUrl(coverImageUrl)
                .isOnline(request.getIsOnline())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .registrationDeadline(request.getRegistrationDeadline())
                .maxParticipants(request.getMaxParticipants())
                .organizationId(organizationId)
                .status(EventStatus.DRAFT)
                .build();

        Event saved = eventRepository.save(event);
        log.info("Event created: {} by organization: {}", saved.getTitle(), organizationId);
        return saved;
    }

    public Event getEventById(String eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found with id: " + eventId));
    }

    @Transactional
    public Event updateEvent(String eventId, String userId, UpdateEventRequest request) {
        return updateEvent(eventId, userId, request, null);
    }

    @Transactional
    public Event updateEvent(String eventId, String userId, UpdateEventRequest request, MultipartFile coverImage) {
        Event event = getEventById(eventId);
        validateEventAccess(event, userId);

        if (request.getTitle() != null) {
            event.setTitle(request.getTitle());
        }

        if (request.getDescription() != null) {
            event.setDescription(request.getDescription());
        }

        if (request.getCategory() != null) {
            event.setCategory(request.getCategory());
        }

        if (request.getCity() != null) {
            event.setCity(request.getCity());
        }

        if (request.getAddress() != null) {
            event.setAddress(request.getAddress());
        }

        if (request.getIsOnline() != null) {
            event.setIsOnline(request.getIsOnline());
        }

        if (request.getStartDate() != null) {
            event.setStartDate(request.getStartDate());
        }

        if (request.getEndDate() != null) {
            event.setEndDate(request.getEndDate());
        }

        if (request.getRegistrationDeadline() != null) {
            event.setRegistrationDeadline(request.getRegistrationDeadline());
        }

        if (request.getMaxParticipants() != null) {
            event.setMaxParticipants(request.getMaxParticipants());
        }

        if (request.getStatus() != null) {
            event.setStatus(request.getStatus());
        }

        if (coverImage != null && !coverImage.isEmpty()) {
            event.setCoverImageUrl(uploadEventCover(coverImage));
        }

        Event updated = eventRepository.save(event);
        log.info("Event updated: {}", eventId);
        return updated;
    }

    private void validateEventAccess(Event event, String userId) {
        if (event.getOrganizationId().equals(userId)) {
            return;
        }

        boolean isAdmin = userRepository.findById(userId)
                .map(user -> user.getRole() == UserRole.ADMIN)
                .orElseGet(() -> organizationRepository.findById(userId)
                        .map(org -> org.getRole() == UserRole.ADMIN)
                        .orElse(false));

        if (isAdmin) {
            return;
        }

        boolean isOrganizer = organizerRepository.findByUserId(userId)
                .map(org -> org.getOrganization().getId().equals(event.getOrganizationId()))
                .orElse(false);

        if (!isOrganizer) {
            throw new AccessDeniedException("Only the event owner, organizer or admin can perform this action");
        }
    }

    @Transactional
    public void publishEvent(String eventId, String userId) {
        Event event = getEventById(eventId);
        validateEventAccess(event, userId);
        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event);
        log.info("Event published: {} by user: {}", eventId, userId);

        try {
            chatService.createEventGroup(eventId, event.getOrganizationId());
        } catch (Exception e) {
            log.warn("Failed to create event group chat on publish", e);
        }
    }

    @Transactional
    public void startEvent(String eventId, String userId) {
        Event event = getEventById(eventId);
        validateEventAccess(event, userId);
        event.setStatus(EventStatus.ONGOING);
        eventRepository.save(event);
        log.info("Event started: {} by user: {}", eventId, userId);
    }

    @Transactional
    public void completeEvent(String eventId, String userId) {
        Event event = getEventById(eventId);
        validateEventAccess(event, userId);

        if (event.getStatus() == EventStatus.COMPLETED) {
            throw new IllegalArgumentException("Event is already completed");
        }
        if (event.getStatus() == EventStatus.CANCELLED) {
            throw new IllegalArgumentException("Cannot complete a cancelled event");
        }

        event.setStatus(EventStatus.COMPLETED);
        eventRepository.save(event);

        certificateService.generateCertificatesForEventAsync(eventId);

        log.info("Event completed: {} by user: {}", eventId, userId);
    }

    @Transactional
    public void cancelEvent(String eventId, String userId) {
        Event event = getEventById(eventId);
        validateEventAccess(event, userId);
        event.setStatus(EventStatus.CANCELLED);
        eventRepository.save(event);

        participationRepository.updateStatusByEventId(eventId, ParticipationStatus.CANCELLED);

        log.info("Event cancelled: {} by user: {}", eventId, userId);
    }

    @Transactional
    public void deleteEventData(String eventId, String userId) {
        Event event = getEventById(eventId);
        validateEventAccess(event, userId);

        registrationService.cancelAllRegistrationsForEvent(eventId);
        participationRepository.deleteByEventId(eventId);
        certificateRepository.deleteByEventId(eventId);
        chatRoomRepository.findByEventId(eventId).ifPresent(room -> {
            groupChatMessageRepository.deleteByChatRoomId(room.getId());
            chatRoomMemberRepository.deleteByChatRoomId(room.getId());
            chatRoomRepository.delete(room);
        });
        eventRepository.delete(event);
        log.info("Event data fully deleted: {} by owner: {}", eventId, userId);
    }

    public List<EventResponse> getOrganizationEventsList(String orgId) {
        String orgName = organizationRepository.findById(orgId)
                .map(Organization::getOrganizationName)
                .orElse(null);

        List<Event> events = eventRepository.findByOrganizationId(orgId);
        List<String> eventIds = events.stream().map(Event::getId).toList();
        
        Map<String, Long> registrationCounts = eventIds.isEmpty() ? new HashMap<>() : registrationRepository.countsByEventIds(eventIds);

        List<EventResponse> responseList = new ArrayList<>();

        for (Event event : events) {
            long count = registrationCounts.getOrDefault(event.getId(), 0L);

            EventResponse eventDto = EventResponse.builder()
                    .id(event.getId())
                    .title(event.getTitle())
                    .description(event.getDescription())
                    .category(event.getCategory())
                    .city(event.getCity())
                    .address(event.getAddress())
                    .isOnline(event.getIsOnline())
                    .startDate(event.getStartDate())
                    .endDate(event.getEndDate())
                    .registrationDeadline(event.getRegistrationDeadline())
                    .maxParticipants(event.getMaxParticipants())
                    .organizationId(event.getOrganizationId())
                    .status(event.getStatus())
                    .coverImageUrl(event.getCoverImageUrl())
                    .createdAt(event.getCreatedAt())
                    .updatedAt(event.getUpdatedAt())
                    .organizationName(orgName)
                    .currentRegistrations((int) count)
                    .build();

            responseList.add(eventDto);
        }

        return responseList;
    }

    public Page<EventResponse> listEvents(EventCategory category, String city, LocalDateTime dateFrom, LocalDateTime dateTo, String search, String organizationId, String sort, Pageable pageable) {
        try {
            Pageable safePageable = buildSafePageable(pageable, sort);

            // filter everything in db query instead of filtering them in memory
            Specification<Event> spec = Specification.where(hasCategory(category))
                    .and(hasCity(city))
                    .and(startDateAfter(dateFrom))
                    .and(endDateBefore(dateTo))
                    .and(hasSearch(search))
                    .and(hasOrganization(organizationId));

            Page<Event> events = eventRepository.findAll(spec, safePageable);

            List<String> eventIds = events.getContent().stream()
                    .map(Event::getId)
                    .toList();
            Map<String, Long> registrationCounts = registrationRepository.countsByEventIds(eventIds);

            List<String> orgIds = events.getContent().stream()
                    .map(Event::getOrganizationId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
            Map<String, String> orgNames = organizationRepository.findAllById(orgIds)
                    .stream()
                    .collect(Collectors.toMap(Organization::getId, Organization::getOrganizationName));

            return events.map(event -> {
                EventResponse response = eventMapper.toEventResponse(event);
                response.setCurrentRegistrations(
                        registrationCounts.getOrDefault(event.getId(), 0L).intValue()
                );
                response.setOrganizationName(orgNames.get(event.getOrganizationId()));
                return response;
            });
        } catch (DataAccessException e) {
            log.error("Database error whilst listing events");
            throw new ServiceUnavailableException("Our services are currently unavailable, please try again later");
        } catch (Exception e) {
            log.error("Unexpected error whilst listin events", e);
            throw new ServiceUnavailableException("Our services are currently unavailable, please try again later");
        }
    }

    // Specifications
    private Specification<Event> hasCategory(EventCategory category) {
        return (root, query, cb) ->
                category == null ? null : cb.equal(root.get("category"), category);
    }

    private Specification<Event> hasCity(String city) {
        return (root, query, cb) ->
                (city == null || city.isEmpty()) ? null : cb.equal(cb.lower(root.get("city")), city.toLowerCase());
    }

    private Specification<Event> startDateAfter(LocalDateTime dateFrom) {
        return (root, query, cb) ->
                dateFrom == null ? null : cb.greaterThan(root.get("startDate"), dateFrom);
    }

    private Specification<Event> endDateBefore(LocalDateTime dateTo) {
        return (root, query, cb) ->
                dateTo == null ? null : cb.lessThan(root.get("endDate"), dateTo);
    }

    private Specification<Event> hasSearch(String search) {
        return (root, query, cb) -> {
            if (search == null || search.isEmpty()) return null;
            String pattern = "%" + search.toLowerCase() + "%";
            return cb.or(
                    cb.like(cb.lower(root.get("title")), pattern),
                    cb.like(cb.lower(root.get("description")), pattern)
            );
        };
    }

    private Specification<Event> hasOrganization(String organizationId) {
        return (root, query, cb) ->
                (organizationId == null || organizationId.isEmpty()) ? null : cb.equal(root.get("organizationId"), organizationId);
    }

    private Pageable buildSafePageable(Pageable pageable, String sortField) {
        String normalizedSort = (sortField == null || sortField.isBlank()) ? "createdAt" : sortField.trim();

        Set<String> allowedSortFields = Set.of(
                "createdAt",
                "updatedAt",
                "startDate",
                "endDate",
                "registrationDeadline",
                "title",
                "city",
                "status",
                "category"
        );

        if (!allowedSortFields.contains(normalizedSort)) {
            normalizedSort = "createdAt";
        }

        int page = pageable == null ? 0 : Math.max(pageable.getPageNumber(), 0);
        int size = pageable == null ? 20 : Math.max(pageable.getPageSize(), 1);

        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, normalizedSort));
    }

    private String uploadEventCover(MultipartFile coverImage) {
        try {
            return cloudinaryService.uploadFile(coverImage, "events/covers");
        } catch (IOException e) {
            log.error("Failed to upload event cover to Cloudinary", e);
            throw new RuntimeException("Event cover upload failed", e);
        }
    }

    public EventDetailResponse getEventDetail(String id) {
        Event event = getEventById(id);
        EventDetailResponse response = eventMapper.toEventDetailResponse(event);
        response.setCurrentRegistrations((int) registrationService.getEventRegistrationCount(id));
        response.setAttendeeCount((int) registrationService.getEventJoinedCount(id));

        try {
            Organization org = organizationService.getOrganizationById(event.getOrganizationId());
            response.setOrganizationName(org.getOrganizationName());
        } catch (Exception e) {
            log.warn("Organization not found for event: {}", event.getId());
        }

        return response;
    }

    public EventStatsResponse getEventStats(String userId, String id) {
        Event event = getEventById(id);
        validateEventAccess(event, userId);
        long totalRegistered = registrationService.getEventRegistrationCount(id);
        long totalAttended = registrationService.getEventJoinedCount(id);

        long registrationRate = event.getMaxParticipants() != null && event.getMaxParticipants() > 0
                ? (totalRegistered * 100) / event.getMaxParticipants()
                : 0;

        long attendanceRate = totalRegistered > 0
                ? (totalAttended * 100) / totalRegistered
                : 0;

        return EventStatsResponse.builder()
                .eventId(event.getId())
                .eventTitle(event.getTitle())
                .totalRegistered(totalRegistered)
                .totalAttended(totalAttended)
                .registrationRate(registrationRate)
                .attendanceRate(attendanceRate)
                .build();
    }

    @Scheduled(cron = "0 * * * * *")
    public void checkAndCloseRegistrations() {
        List<Event> eventsToClose = eventRepository.findByStatusAndRegistrationDeadlineBefore(
                EventStatus.PUBLISHED, LocalDateTime.now());

        if (!eventsToClose.isEmpty()) {
            for (Event event : eventsToClose) {
                event.setStatus(EventStatus.REGISTRATION_CLOSED);
                log.info("Registration closed automatically for event: {}", event.getId());
            }
            eventRepository.saveAll(eventsToClose);
        }
    }
}
