package com.ween.repository;

import com.ween.entity.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, String> {

    @Query("""
            select m from ChatMessage m
            where (m.senderId = :userId and m.recipientId = :partnerId)
               or (m.senderId = :partnerId and m.recipientId = :userId)
            """)
    Page<ChatMessage> findConversationMessages(
            @Param("userId") String userId,
            @Param("partnerId") String partnerId,
            Pageable pageable);

    @Query("""
            select m from ChatMessage m
            where (m.senderId = :userId or m.recipientId = :userId)
              and (m.request = false or (m.request = true and m.senderId = :userId))
              and not exists (
                select 1 from ChatMessage newer
                where ((newer.senderId = m.senderId and newer.recipientId = m.recipientId)
                       or (newer.senderId = m.recipientId and newer.recipientId = m.senderId))
                  and (newer.request = false or (newer.request = true and newer.senderId = :userId))
                  and newer.createdAt > m.createdAt
              )
            order by m.createdAt desc
            """)
    List<ChatMessage> findLatestMessagesByUser(@Param("userId") String userId);

    Page<ChatMessage> findByRecipientIdAndRequestTrueOrderByCreatedAtDesc(String recipientId, Pageable pageable);

    @Query("""
            select count(m) > 0 from ChatMessage m
            where (m.senderId = :userId and m.recipientId = :partnerId)
               or (m.senderId = :partnerId and m.recipientId = :userId)
            """)
    boolean conversationExists(@Param("userId") String userId, @Param("partnerId") String partnerId);

    @Query("""
            select count(m) > 0 from ChatMessage m
            where m.request = false and (
                (m.senderId = :userId and m.recipientId = :partnerId)
                or (m.senderId = :partnerId and m.recipientId = :userId)
            )
            """)
    boolean acceptedConversationExists(@Param("userId") String userId, @Param("partnerId") String partnerId);

    @Modifying
    @Query("""
            update ChatMessage m set m.request = false
            where m.recipientId = :userId and m.senderId = :partnerId and m.request = true
            """)
    int acceptMessageRequest(@Param("userId") String userId, @Param("partnerId") String partnerId);

    Page<ChatMessage> findByChatRoomId(String chatRoomId, Pageable pageable);

    long countBySenderIdAndRecipientIdAndReadAtIsNull(String senderId, String recipientId);

    @Modifying
    @Query("""
            update ChatMessage m
            set m.readAt = :readAt
            where m.senderId = :partnerId
              and m.recipientId = :userId
              and m.readAt is null
            """)
    int markConversationAsRead(
            @Param("userId") String userId,
            @Param("partnerId") String partnerId,
            @Param("readAt") LocalDateTime readAt);
}
