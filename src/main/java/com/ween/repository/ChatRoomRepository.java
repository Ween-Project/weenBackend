package com.ween.repository;

import com.ween.entity.ChatRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ChatRoomRepository extends JpaRepository<ChatRoom, String> {

    Optional<ChatRoom> findByEventId(String eventId);

    @Query("SELECT r FROM ChatRoom r WHERE r.type = com.ween.enums.ChatRoomType.DIRECT AND " +
            "((r.participantOneId = :p1 AND r.participantTwoId = :p2) OR " +
            "(r.participantOneId = :p2 AND r.participantTwoId = :p1))")
    Optional<ChatRoom> findDirectRoom(@Param("p1") String p1, @Param("p2") String p2);

    @Query("SELECT r FROM ChatRoom r WHERE r.participantOneId = :userId OR r.participantTwoId = :userId")
    List<ChatRoom> findDirectRoomsForUser(@Param("userId") String userId);
}
