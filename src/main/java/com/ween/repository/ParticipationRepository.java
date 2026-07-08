package com.ween.repository;

import com.ween.entity.Participation;
import com.ween.enums.ParticipationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, String> {
    Optional<Participation> findByUserIdAndEventId(String userId, String eventId);
    void deleteByEventId(String eventId);

    @Modifying
    @Query("UPDATE Participation p SET p.status = :status WHERE p.event.id = :eventId")
    void updateStatusByEventId(@Param("eventId") String eventId, @Param("status") ParticipationStatus status);
}