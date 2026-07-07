package com.ween.repository;

import com.ween.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserBadgeRepository extends JpaRepository<UserBadge, String> {
    List<UserBadge> findByUserId(String userId);
    boolean existsByUserIdAndBadgeIdAndSpecialKey(String userId, String badgeId, String specialKey);
    boolean existsByUserIdAndBadgeId(String userId, String badgeId);
}
