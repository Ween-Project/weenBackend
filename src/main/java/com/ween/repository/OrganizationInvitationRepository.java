package com.ween.repository;

import com.ween.entity.OrganizationInvitation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OrganizationInvitationRepository extends JpaRepository<OrganizationInvitation, String> {

    Optional<OrganizationInvitation> findByToken(String token);

}
