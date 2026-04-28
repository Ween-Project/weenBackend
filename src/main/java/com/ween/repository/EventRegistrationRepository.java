package com.ween.repository;

import com.ween.entity.EventRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRegistrationRepository extends JpaRepository<EventRegistration, String> {
    Optional<EventRegistration> findByEventIdAndUserId(String eventId, String userId);
    List<EventRegistration> findByEventId(String eventId);
    List<EventRegistration> findByEventIdAndIsJoinedTrue(String eventId);
    long countByEventId(String eventId);
    long countByEventIdAndIsJoinedTrue(String eventId);
    long countByIsJoinedTrue();
    List<EventRegistration> findByUserId(String userId);
}
