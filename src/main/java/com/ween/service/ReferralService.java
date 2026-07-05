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
        User referrer = userRepository.findByReferralCode(referrerCode).orElse(null);
        if (referrer == null) {
            log.warn("Ignoring invalid referral code during signup");
            return;
        }


        if (referralRepository.findByReferrerIdAndReferredId(referrer.getId(), referredUserId).isPresent()) {
            log.info("Referral already exists between referrer={} and referred={}",
                    referrer.getId(), referredUserId);
            return;
        }

        Referral referral = Referral.builder()
                .referrerId(referrer.getId())
                .referredId(referredUserId)
                .coinAwarded(true)
                .build();
        referralRepository.save(referral);

        coinService.awardReferralBonus(referrer.getId(), referredUserId);
        coinService.awardReferredBonus(referredUserId, referrer.getId());

        log.info("Referral successfully processed at signup: referrer={}, referred={}", referrer.getId(), referredUserId);
    }


}
