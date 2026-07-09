package com.ween.repository;

import com.ween.entity.QrToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QrTokenRepository extends JpaRepository<QrToken, String> {
    Optional<QrToken> findByUserIdAndIsRevokedFalse(String userId);

    Optional<QrToken> findByTokenHashAndIsRevokedFalse(String tokenHash);

    @Modifying
    @Query("UPDATE QrToken q SET q.isRevoked = true WHERE q.userId = :userId AND q.isRevoked = false")
    void revokeAllByUserId(@Param("userId") String userId);

    void deleteAllByExpiresAtBefore(java.time.LocalDateTime time);
}