package com.ween.repository;

import com.ween.entity.Organizer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizerRepository extends JpaRepository<Organizer, String> {

    Optional<Organizer> findByUserId(String userId);
    
    Optional<Organizer> findByIdAndOrganizationId(String id, String organizationId);

}
