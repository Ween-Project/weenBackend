package com.ween.repository;

import com.ween.entity.Follow;
import com.ween.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FollowRepository extends JpaRepository<Follow, String> {

    boolean existsByFollowerAndFollowing(User follower, User following);

    void deleteByFollowerAndFollowing(User follower, User following);

    Page<Follow> findByFollowing(User following, Pageable pageable);

    Page<Follow> findByFollower(User follower, Pageable pageable);

    long countByFollower(User follower);

    long countByFollowing(User following);
}