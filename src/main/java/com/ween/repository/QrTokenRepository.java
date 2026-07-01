package com.ween.repository;

import com.ween.entity.QrToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QrTokenRepository extends JpaRepository<QrToken, String> {
    Optional<QrToken> findByUserIdAndIsRevokedFalse(String userId);

    void deleteAllByExpiresAtBefore(java.time.LocalDateTime time);
}