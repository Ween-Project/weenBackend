package com.ween.service;

import com.ween.dto.response.CoinTransactionResponse;
import com.ween.entity.CoinTransaction;
import com.ween.entity.User;
import com.ween.enums.CoinReason;
import com.ween.exception.ResourceNotFoundException;
import com.ween.mapper.CoinTransactionMapper;
import com.ween.repository.CoinTransactionRepository;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
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
    private final CoinTransactionMapper coinTransactionMapper;

    @Transactional
    public CoinTransaction credit(String userId, Integer amount, CoinReason reason) {
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
                .build();

        CoinTransaction saved = coinTransactionRepository.save(transaction);
        log.info("Coins credited to user {}: {} coins for reason: {}", userId, amount, reason);
        return saved;
    }

    public Integer getUserCoinBalance(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return user.getWeenCoinBalance();
    }

    public Page<CoinTransactionResponse> getUserCoinTransactions(String userId, Pageable pageable) {
        return coinTransactionRepository.findByUserId(userId, pageable)
                .map(coinTransactionMapper::toCoinTransactionResponse);
    }

    @Transactional
    public void awardSignupBonus(String userId) {
        long signupCount = coinTransactionRepository.countByUserIdAndReason(userId, CoinReason.SIGNUP);

        // Ensure one-time bonus prevention: only award if user hasn't already received signup bonus
        if (signupCount == 0) {
            credit(userId, 50, CoinReason.SIGNUP);
            log.info("Signup bonus awarded to user: {}", userId);
        } else {
            log.info("Signup bonus already awarded to user: {}", userId);
        }
    }


    @Transactional
    public void awardAttendanceBonus(String userId, String eventId) {
        credit(userId, 50, CoinReason.ATTENDANCE);
        log.info("Attendance bonus awarded to user: {} for event: {}", userId, eventId);
    }


    @Transactional
    public void awardReferralBonus(String referrerId, String referredId) {
        credit(referrerId, 25, CoinReason.REFERRAL);
        log.info("Referral bonus awarded to referrer: {} for referring: {}", referrerId, referredId);
    }

    @Transactional
    public void awardReferredBonus(String referredId, String referrerId) {
        credit(referredId, 25, CoinReason.REFERRAL);
        log.info("Referred bonus awarded to user: {} referred by: {}", referredId, referrerId);
    }

    @Transactional
    public void awardProfileCompleteBonus(String userId) {
        long profileCount = coinTransactionRepository.countByUserIdAndReason(userId, CoinReason.PROFILE_COMPLETE);

        // One-time bonus prevention
        if (profileCount == 0) {
            credit(userId, 50, CoinReason.PROFILE_COMPLETE);
            log.info("Profile complete bonus awarded to user: {}", userId);
        } else {
            log.info("Profile complete bonus already awarded to user: {}", userId);
        }
    }

}