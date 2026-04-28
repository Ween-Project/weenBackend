package com.ween.repository;

import com.ween.entity.LeaderboardEntry;
import com.ween.enums.LeaderboardPeriod;
import com.ween.enums.LeaderboardScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, Long> {
    Page<LeaderboardEntry> findByPeriodAndScopeOrderByRankPositionAsc(
        LeaderboardPeriod period,
        LeaderboardScope scope,
        Pageable pageable
    );
    
    void deleteByPeriodAndScope(LeaderboardPeriod period, LeaderboardScope scope);
}
