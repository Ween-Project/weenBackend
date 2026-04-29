package com.ween.service;

import com.ween.entity.Referral;
import com.ween.entity.User;
import com.ween.exception.AlreadyExistsException;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.ReferralRepository;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ReferralService {

    private final ReferralRepository referralRepository;
    private final UserRepository userRepository;
    private final CoinService coinService;

    @Transactional
    public Referral createReferral(String referrerCode, String referredUserId) {
        // Find referrer by referral code
        User referrer = userRepository.findByReferralCode(referrerCode)
                .orElseThrow(() -> new ResourceNotFoundException("Invalid referral code: " + referrerCode));

        User referredUser = userRepository.findById(referredUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Referred user not found: " + referredUserId));

        // Check if referral already exists
        if (referralRepository.findByReferrerIdAndReferredId(referrer.getId(), referredUserId).isPresent()) {
            throw new AlreadyExistsException("Referral already exists between these users");
        }

        Referral referral = Referral.builder()

                .referrerId(referrer.getId())
                .referredId(referredUserId)
                .coinAwarded(false)
                .build();

        Referral saved = referralRepository.save(referral);
        log.info("Referral created: referrer={}, referred={}", referrer.getId(), referredUserId);
        return saved;
    }

    @Transactional
    public void awardReferralCoins(String referralId) {
        Referral referral = referralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral not found: " + referralId));

        if (referral.getCoinAwarded()) {
            log.warn("Referral coins already awarded for referral: {}", referralId);
            return;
        }

        // Award referrer
        coinService.awardReferralBonus(referral.getReferrerId(), referral.getReferredId());

        // Award referred user
        coinService.credit(referral.getReferredId(), 100, com.ween.enums.CoinReason.REFERRAL, referral.getReferrerId());

        // Mark coins as awarded
        referral.setCoinAwarded(true);
        referralRepository.save(referral);
        log.info("Referral coins awarded for referral: {}", referralId);
    }

    public Referral getReferralById(String referralId) {
        return referralRepository.findById(referralId)
                .orElseThrow(() -> new ResourceNotFoundException("Referral not found: " + referralId));
    }

    public Integer getReferrerCount(String userId) {
        return (int) referralRepository.findAll().stream()
                .filter(r -> r.getReferrerId().equals(userId))
                .count();
    }

    public Integer getSuccessfulReferralCount(String userId) {
        return (int) referralRepository.findAll().stream()
                .filter(r -> r.getReferrerId().equals(userId) && r.getCoinAwarded())
                .count();
    }

    public Integer getTotalReferralCoinsEarned(String userId) {
        return getSuccessfulReferralCount(userId) * 150; // 150 coins per referral
    }
}