package com.ween.repository;

import com.ween.entity.GroupChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GroupChatMessageRepository extends JpaRepository<GroupChatMessage, String> {

    Page<GroupChatMessage> findByChatRoomId(String chatRoomId, Pageable pageable);
    void deleteByChatRoomId(String chatRoomId);

}
