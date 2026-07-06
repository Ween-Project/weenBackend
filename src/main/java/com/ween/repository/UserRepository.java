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
               OR LOWER(COALESCE(u.skills, '')) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(COALESCE(u.interests, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            """)
    Page<User> searchPublicProfiles(@Param("query") String query, Pageable pageable);

        @Query("""
                        SELECT DISTINCT candidate FROM User candidate
                        JOIN Follow f2 ON f2.following = candidate
                        JOIN Follow f1 ON f1.following = f2.follower
                        WHERE f1.follower.id = :currentUserId
                          AND candidate.id <> :currentUserId
                          AND candidate.banned = false
                          AND candidate.id NOT IN (
                                  SELECT followed.id FROM Follow direct
                                  JOIN direct.following followed
                                  WHERE direct.follower.id = :currentUserId
                          )
                        """)
        Page<User> findNetworkDiscoveryCandidates(@Param("currentUserId") String currentUserId, Pageable pageable);

        @Query("""
                        SELECT DISTINCT candidate FROM User candidate
                        JOIN Follow f2 ON f2.following = candidate
                        JOIN Follow f1 ON f1.following = f2.follower
                        WHERE f1.follower.id = :currentUserId
                          AND candidate.id <> :currentUserId
                          AND candidate.banned = false
                          AND candidate.id NOT IN (
                                  SELECT followed.id FROM Follow direct
                                  JOIN direct.following followed
                                  WHERE direct.follower.id = :currentUserId
                          )
                          AND (
                                  LOWER(candidate.username) LIKE LOWER(CONCAT('%', :query, '%'))
                                  OR LOWER(candidate.fullName) LIKE LOWER(CONCAT('%', :query, '%'))
                                  OR LOWER(COALESCE(candidate.university, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                                  OR LOWER(COALESCE(candidate.major, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                                  OR LOWER(COALESCE(candidate.skills, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                                  OR LOWER(COALESCE(candidate.interests, '')) LIKE LOWER(CONCAT('%', :query, '%'))
                          )
                        """)
        Page<User> findNetworkDiscoveryCandidatesByQuery(@Param("currentUserId") String currentUserId,
                                                                                                         @Param("query") String query,
                                                                                                         Pageable pageable);
    
    // For ALL_TIME leaderboard
    Page<User> findAllByOrderByWeenCoinBalanceDesc(Pageable pageable);
}
