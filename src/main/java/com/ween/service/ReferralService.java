package com.ween.service;

import com.ween.entity.Referral;
import com.ween.entity.User;
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
    public void processReferralAtSignup(String referrerCode, String referredUserId) {
        // Referral codes are optional. An invalid code must not mark the surrounding
        // registration transaction rollback-only after AuthService catches it.
        User referrer = userRepository.findByReferralCode(referrerCode).orElse(null);
        if (referrer == null) {
            log.warn("Ignoring invalid referral code during signup");
            return;
        }


        // 2. Check if referral already exists
        if (referralRepository.findByReferrerIdAndReferredId(referrer.getId(), referredUserId).isPresent()) {
            log.info("Referral already exists between referrer={} and referred={}",
                    referrer.getId(), referredUserId);
            return;
        }

        // 3. Create the referral record, immediately awarded
        Referral referral = Referral.builder()
                .referrerId(referrer.getId())
                .referredId(referredUserId)
                .coinAwarded(true)
                .build();
        referralRepository.save(referral);

        // 4. Award the coins via CoinService
        coinService.awardReferralBonus(referrer.getId(), referredUserId);
        coinService.awardReferredBonus(referredUserId, referrer.getId());

        log.info("Referral successfully processed at signup: referrer={}, referred={}", referrer.getId(), referredUserId);
    }


}
