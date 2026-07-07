package com.ween.repository;

import com.ween.entity.Badge;
import com.ween.enums.BadgeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface BadgeRepository extends JpaRepository<Badge, String> {
    List<Badge> findByTypeAndIsActiveTrue(BadgeType type);
    Optional<Badge> findFirstByTypeAndIsActiveTrue(BadgeType type);
    List<Badge> findByIsActiveTrue();
    Page<Badge> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
