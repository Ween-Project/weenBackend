package com.ween.repository;

import com.ween.entity.Certificate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, String> {
    List<Certificate> findByUserId(String userId);
    Optional<Certificate> findByCertificateNumber(String number);
    boolean existsByUserIdAndEventId(String userId, String eventId);
}
