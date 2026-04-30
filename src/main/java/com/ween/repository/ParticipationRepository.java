package com.ween.repository;

import com.ween.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, String> {
    Optional<Participation> findByUserIdAndEventId(String userId, String eventId);
}