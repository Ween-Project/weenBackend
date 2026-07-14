package com.ween.repository;

import com.ween.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {
    Optional<User> findByEmail(String email);
    Optional<User> findByUsername(String username);
    Optional<User> findByReferralCode(String code);
    boolean existsByEmail(String email);
    boolean existsByUsername(String username);
    long countByBanned(Boolean banned);
    Page<User> findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(String username, String email, Pageable pageable);

    @Query("""
            SELECT u FROM User u
            WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(u.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(u.university, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(u.major, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(CAST(u.skills AS string), '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(CAST(u.interests AS string), '')) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<User> searchPublicProfiles(@Param("query") String query, Pageable pageable);

    // For ALL_TIME leaderboard
    Page<User> findAllByOrderByWeenCoinBalanceDesc(Pageable pageable);
}
