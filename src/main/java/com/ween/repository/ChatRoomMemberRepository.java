package com.ween.repository;

import com.ween.entity.ChatRoomMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomMemberRepository extends JpaRepository<ChatRoomMember, String> {

    List<ChatRoomMember> findByChatRoomId(String chatRoomId);

    Optional<ChatRoomMember> findByChatRoomIdAndUserId(String chatRoomId, String userId);

    void deleteByChatRoomIdAndUserId(String chatRoomId, String userId);
    void deleteByChatRoomId(String chatRoomId);

    List<ChatRoomMember> findByUserId(String userId);
}
