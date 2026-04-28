package com.ween.repository;

import com.ween.entity.Event;
import com.ween.enums.EventCategory;
import com.ween.enums.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, String>, JpaSpecificationExecutor<Event> {
    List<Event> findByOrganizationId(String orgId);
    
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
}
