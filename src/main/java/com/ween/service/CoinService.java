package com.ween.service;

import com.ween.entity.CoinTransaction;
import com.ween.entity.User;
import com.ween.enums.CoinReason;
import com.ween.exception.ResourceNotFoundException;
import com.ween.repository.CoinTransactionRepository;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CoinService {

    private final CoinTransactionRepository coinTransactionRepository;
    private final UserRepository userRepository;

    @Transactional
    public CoinTransaction credit(String userId, Integer amount, CoinReason reason, String relatedEntityId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Update user balance
        user.setWeenCoinBalance(user.getWeenCoinBalance() + amount);
        userRepository.save(user);

        // Create coin transaction (atomic with balance update)
        CoinTransaction transaction = CoinTransaction.builder()
                .userId(userId)
                .amount(amount)
                .reason(reason)
                .relatedEntityId(relatedEntityId)
                .build();

        CoinTransaction saved = coinTransactionRepository.save(transaction);
        log.info("Coins credited to user {}: {} coins for reason: {}", userId, amount, reason);
        return saved;
    }

    @Transactional
    public CoinTransaction debit(String userId, Integer amount, CoinReason reason, String relatedEntityId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (user.getWeenCoinBalance() < amount) {
            throw new IllegalArgumentException("Insufficient coin balance");
        }

        // Update user balance
        user.setWeenCoinBalance(user.getWeenCoinBalance() - amount);
        userRepository.save(user);

        // Create coin transaction (atomic with balance update)
        CoinTransaction transaction = CoinTransaction.builder()
                .userId(userId)
                .amount(-amount)
                .reason(reason)
                .relatedEntityId(relatedEntityId)
                .build();

        CoinTransaction saved = coinTransactionRepository.save(transaction);
        log.info("Coins debited from user {}: {} coins for reason: {}", userId, amount, reason);
        return saved;
    }

    public Integer getUserCoinBalance(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return user.getWeenCoinBalance();
    }

    public List<CoinTransaction> getUserCoinTransactions(String userId, Pageable pageable) {
        return coinTransactionRepository.findByUserId(userId);
    }

    @Transactional
    public void awardSignupBonus(String userId) {
        long signupCount = coinTransactionRepository.countByUserIdAndReason(userId, CoinReason.SIGNUP);

        // Ensure one-time bonus prevention: only award if user hasn't already received signup bonus
        if (signupCount == 0) {
            credit(userId, 100, CoinReason.SIGNUP, null);
            log.info("Signup bonus awarded to user: {}", userId);
        } else {
            log.info("Signup bonus already awarded to user: {}", userId);
        }
    }

    @Transactional
    public void awardEventRegistrationBonus(String userId, String eventId) {
        credit(userId, 25, CoinReason.REGISTRATION, eventId);
        log.info("Event registration bonus awarded to user: {} for event: {}", userId, eventId);
    }

    @Transactional
    public void awardAttendanceBonus(String userId, String eventId) {
        credit(userId, 50, CoinReason.ATTENDANCE, eventId);
        log.info("Attendance bonus awarded to user: {} for event: {}", userId, eventId);
    }

    @Transactional
    public void awardCertificateBonus(String userId, String certificateId) {
        credit(userId, 75, CoinReason.CERTIFICATE, certificateId);
        log.info("Certificate bonus awarded to user: {} for certificate: {}", userId, certificateId);
    }

    @Transactional
    public void awardProfileCompleteBonus(String userId) {
        long profileCount = coinTransactionRepository.countByUserIdAndReason(userId, CoinReason.PROFILE_COMPLETE);

        // One-time bonus prevention
        if (profileCount == 0) {
            credit(userId, 50, CoinReason.PROFILE_COMPLETE, null);
            log.info("Profile complete bonus awarded to user: {}", userId);
        } else {
            log.info("Profile complete bonus already awarded to user: {}", userId);
        }
    }

    @Transactional
    public void awardReferralBonus(String referrerId, String referredId) {
        credit(referrerId, 150, CoinReason.REFERRAL, referredId);
        log.info("Referral bonus awarded to user: {} for referral: {}", referrerId, referredId);
    }

    @Transactional
    public void awardLeaderboardBonus(String userId, Integer rank) {
        Integer bonusCoins = calculateLeaderboardBonus(rank);
        if (bonusCoins > 0) {
            credit(userId, bonusCoins, CoinReason.LEADERBOARD_BONUS, null);
            log.info("Leaderboard bonus awarded to user: {} for rank: {}", userId, rank);
        }
    }

    private Integer calculateLeaderboardBonus(Integer rank) {
        if (rank == null || rank <= 0) {
            return 0;
        }

        if (rank == 1) return 500;
        if (rank == 2) return 300;
        if (rank == 3) return 150;
        if (rank <= 10) return 100;
        if (rank <= 50) return 50;

        return 0;
    }
}