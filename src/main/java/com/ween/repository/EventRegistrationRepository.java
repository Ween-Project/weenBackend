package com.ween.repository;

import com.ween.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, String> {
    Optional<EventRegistration> findByEventIdAndUserId(String eventId, String userId);
    List<EventRegistration> findByEventId(String eventId);
    org.springframework.data.domain.Page<EventRegistration> findByEventId(String eventId, org.springframework.data.domain.Pageable pageable);
    List<EventRegistration> findByEventIdAndIsJoinedTrue(String eventId);
    long countByEventId(String eventId);
    long countByEventIdAndIsJoinedTrue(String eventId);
    long countByIsJoinedTrue();
    long countByUserIdAndIsJoinedTrue(String userId);
    @Query("""
            SELECT COUNT(r) FROM EventRegistration r
            WHERE r.userId = :userId AND r.isJoined = true
              AND r.eventId IN (SELECT e.id FROM Event e WHERE e.category = :category)
            """)
    long countJoinedByUserIdAndEventCategory(
            @Param("userId") String userId,
            @Param("category") com.ween.enums.EventCategory category);
    List<EventRegistration> findByUserId(String userId);

    @Query("SELECT r.eventId, COUNT(r) FROM EventRegistration r WHERE r.eventId IN :eventIds GROUP BY r.eventId")
    List<Object[]> countByEventIds(@Param("eventIds") List<String> eventIds);

    default Map<String, Long> countsByEventIds(List<String> eventIds) {
        return countByEventIds(eventIds).stream()
                .collect(Collectors.toMap(
                        row -> (String) row[0],
                        row -> (Long) row[1]
                ));
    }
}
