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
import com.ween.exception.ServiceUnavailableException;
import com.ween.mapper.EventMapper;
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.OrganizationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final OrganizationRepository organizationRepository;
    private final EventRegistrationRepository registrationRepository;
    private final EventMapper eventMapper;
    private final OrganizationService organizationService;
    private final RegistrationService registrationService;

    @Transactional
    public Event createEvent(CreateEventRequest request, String organizationId) {
        Organization organization = organizationService.getOrganizationById(organizationId);

        Event event = Event.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .category(request.getCategory())
                .city(request.getCity())
                .address(request.getAddress())
                .coverImageUrl(request.getCoverImageUrl())
                .customFields(request.getCustomFields())
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
    public Event updateEvent(String eventId, String id, UpdateEventRequest request) {
        Event event = getEventById(eventId);

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

        Event updated = eventRepository.save(event);
        log.info("Event updated: {}", eventId);
        return updated;
    }

    @Transactional
    public void publishEvent(String eventId) {
        Event event = getEventById(eventId);
        event.setStatus(EventStatus.PUBLISHED);
        eventRepository.save(event);
        log.info("Event published: {}", eventId);
    }

    @Transactional
    public void startEvent(String eventId) {
        Event event = getEventById(eventId);
        event.setStatus(EventStatus.ONGOING);
        eventRepository.save(event);
        log.info("Event started: {}", eventId);
    }

    @Transactional
    public void completeEvent(String eventId) {
        Event event = getEventById(eventId);
        event.setStatus(EventStatus.COMPLETED);
        eventRepository.save(event);
        log.info("Event completed: {}", eventId);
    }

    @Transactional
    public void cancelEvent(String eventId, String userId) {
        Event event = getEventById(eventId);

        if (!event.getOrganizationId().equals(userId)) {
            throw new AccessDeniedException("Only the event owner can delete this event");
        }

        registrationService.cancelAllRegistrationsForEvent(eventId);
        eventRepository.delete(event);
        log.info("Event deleted: {} by owner: {}", eventId, userId);
    }

    @Transactional
    public void deleteEvent(String eventId) {
        Event event = getEventById(eventId);
        eventRepository.delete(event);
        log.info("Event deleted: {}", eventId);
    }

    public Page<Event> getAllPublishedEvents(Pageable pageable) {
        return eventRepository.findAll(pageable);
    }

    public long getEventParticipantCount(String eventId) {
        return eventRepository.findById(eventId)
                .map(event -> {
                    // Will be calculated from EventRegistration count in RegistrationService
                    return 0L;
                })
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
    }

    public boolean isEventCapacityFull(String eventId) {
        Event event = getEventById(eventId);
        if (event.getMaxParticipants() == null) {
            return false;
        }

        // Get registrations count from repository
        long registrationCount = eventRepository.findById(eventId)
                .map(e -> 0L) // Will be replaced with actual count from RegistrationService
                .orElse(0L);

        return registrationCount >= event.getMaxParticipants();
    }

    public Integer getRemainingCapacity(String eventId) {
        Event event = getEventById(eventId);
        if (event.getMaxParticipants() == null) {
            return Integer.MAX_VALUE;
        }

        long registrationCount = 0; // Will be fetched from RegistrationService
        return Math.max(0, (int) (event.getMaxParticipants() - registrationCount));
    }

    public boolean isRegistrationDeadlinePassed(String eventId) {
        Event event = getEventById(eventId);
        if (event.getRegistrationDeadline() == null) {
            return false;
        }
        return java.time.LocalDateTime.now().isAfter(event.getRegistrationDeadline());
    }

    public boolean isEventInFuture(String eventId) {
        Event event = getEventById(eventId);
        if (event.getStartDate() == null) {
            return false;
        }
        return event.getStartDate().isAfter(java.time.LocalDateTime.now());
    }

    public boolean isEventOngoing(String eventId) {
        Event event = getEventById(eventId);
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        return now.isAfter(event.getStartDate()) && now.isBefore(event.getEndDate());
    }

    @Transactional
    public void setCustomFields(String eventId, String customFieldsJson) {
        Event event = getEventById(eventId);
        event.setCustomFields(customFieldsJson);
        eventRepository.save(event);
        log.info("Custom fields set for event: {}", eventId);
    }

    public List<EventResponse> getOrganizationEventsList(String orgId) {

        String orgName = organizationRepository.findById(orgId)
                .map(Organization::getOrganizationName)
                .orElse(null);

        List<Event> events = eventRepository.findByOrganizationId(orgId);

        List<EventResponse> responseList = new ArrayList<>();

        for (Event event : events) {

            long count = registrationService.getEventRegistrationCount(event.getId());

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
                    .customFields(event.getCustomFields())
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
}
