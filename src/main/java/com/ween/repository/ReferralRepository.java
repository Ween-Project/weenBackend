package com.ween.repository;

import com.ween.entity.Referral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReferralRepository extends JpaRepository<Referral, String> {
    Optional<Referral> findByReferrerIdAndReferredId(String referrerId, String referredId);
    long countByReferrerId(String referrerId);
}
