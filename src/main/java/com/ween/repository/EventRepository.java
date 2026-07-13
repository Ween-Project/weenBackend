package com.ween.repository;

import com.ween.entity.Event;
import com.ween.enums.EventCategory;
import com.ween.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, String>, JpaSpecificationExecutor<Event> {
    List<Event> findByOrganizationId(String orgId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Event e WHERE e.id = :id")
    Optional<Event> findByIdWithLock(@Param("id") String id);

    @Query("SELECT e FROM Event e JOIN EventRegistration er ON e.id = er.eventId WHERE er.userId = :userId")
    Page<Event> findEventsByRegisteredUserId(@Param("userId") String userId, Pageable pageable);

    
    @Query(value = "SELECT * FROM events WHERE MATCH(title, description) AGAINST(:query IN BOOLEAN MODE)",
           nativeQuery = true)
    Page<Event> searchFullText(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT e FROM Event e WHERE " +
           "(:category IS NULL OR e.category = :category) AND " +
           "(:city IS NULL OR e.city = :city) AND " +
           "(:startDate IS NULL OR e.startDate >= :startDate) AND " +
           "(:endDate IS NULL OR e.endDate <= :endDate) AND " +
           "(:organizationId IS NULL OR e.organizationId = :organizationId)")
    Page<Event> findWithFilters(
        @Param("category") EventCategory category,
        @Param("city") String city,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate,
        @Param("organizationId") String organizationId,
        Pageable pageable
    );
    
    List<Event> findByStatusOrderByStartDateAsc(EventStatus status);

    long countByStatus(EventStatus status);

    @Query("SELECT e FROM Event e WHERE LOWER(e.title) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(e.description) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Event> searchEvents(@Param("search") String search, Pageable pageable);

    @Query("SELECT e FROM Event e WHERE e.startDate BETWEEN :now AND :end AND e.status = :status")
    List<Event> findEventsStartingBetween(@Param("now") LocalDateTime now, @Param("end") LocalDateTime end, @Param("status") EventStatus status);

    List<Event> findByStatusAndRegistrationDeadlineBefore(EventStatus status, LocalDateTime registrationDeadline);
}
