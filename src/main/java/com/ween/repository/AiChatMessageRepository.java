package com.ween.repository;

import com.ween.entity.AiChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiChatMessageRepository extends JpaRepository<AiChatMessage, String> {
    Page<AiChatMessage> findByUserIdOrderByCreatedAtAsc(String userId, Pageable pageable);
    void deleteByUserId(String userId);
}
