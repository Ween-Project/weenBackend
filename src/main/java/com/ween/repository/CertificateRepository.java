package com.ween.repository;

import com.ween.entity.Certificate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

@Repository
public interface CertificateRepository extends JpaRepository<Certificate, String> {
    List<Certificate> findByUserId(String userId);
    Page<Certificate> findByUserId(String userId, Pageable pageable);
    Optional<Certificate> findByCertificateNumber(String number);
    void deleteById(String id);
    boolean existsByUserIdAndEventId(String userId, String eventId);
    void deleteByEventId(String eventId);

    @Query("SELECT c FROM Certificate c, User u WHERE c.userId = u.id AND (LOWER(c.certificateNumber) LIKE LOWER(CONCAT('%', :search, '%')) OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')))")
    Page<Certificate> searchCertificates(@Param("search") String search, Pageable pageable);
}
