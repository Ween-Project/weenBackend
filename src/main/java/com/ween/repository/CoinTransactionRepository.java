package com.ween.repository;

import com.ween.entity.CoinTransaction;
import com.ween.enums.CoinReason;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoinTransactionRepository extends JpaRepository<CoinTransaction, String> {
    Page<CoinTransaction> findByUserId(String userId, Pageable pageable);
    List<CoinTransaction> findAllByUserId(String userId);

    @Query("SELECT COUNT(ct) FROM CoinTransaction ct WHERE ct.userId = :userId AND ct.reason = :reason")
    long countByUserIdAndReason(@Param("userId") String userId, @Param("reason") CoinReason reason);
    boolean existsByUserIdAndReasonAndRelatedEntityId(String userId, CoinReason reason, String relatedEntityId);


    @Query("SELECT COALESCE(SUM(ct.amount), 0) FROM CoinTransaction ct WHERE ct.userId = :userId AND ct.reason = :reason")
    int sumByUserIdAndReason(@Param("userId") String userId, @Param("reason") CoinReason reason);

    @Query("SELECT COALESCE(SUM(ct.amount), 0L) FROM CoinTransaction ct")
    Long sumAllCoins();

    @Query("SELECT ct.userId as userId, CAST(SUM(ct.amount) AS integer) as totalCoins FROM CoinTransaction ct JOIN User u ON ct.userId = u.id WHERE u.role != 'ADMIN' AND ct.createdAt >= :startDate AND ct.createdAt <= :endDate GROUP BY ct.userId HAVING SUM(ct.amount) > 0 ORDER BY SUM(ct.amount) DESC")
    List<UserCoinSum> getCoinSumsBetween(@Param("startDate") java.time.LocalDateTime startDate, @Param("endDate") java.time.LocalDateTime endDate);

    interface UserCoinSum {
        String getUserId();
        Integer getTotalCoins();
    }
}
