package com.ween.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.ween.entity.Organization;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, String> {
    Page<Organization> findByOrganizationNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(String name, String description, Pageable pageable);
    
    Optional<Organization> findByEmail(String email);
    
    Optional<Organization> findByUsername(String username);
    
    boolean existsByEmail(String email);
    
    boolean existsByUsername(String username);
}
