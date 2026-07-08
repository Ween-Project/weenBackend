package com.ween.service;

import com.ween.repository.ChatMessageRepository;
import com.ween.repository.ChatRoomMemberRepository;
import com.ween.repository.ChatRoomRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.FollowRepository;
import com.ween.repository.GroupChatMessageRepository;
import com.ween.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatMessageRepository chatMessageRepository;
    @Mock GroupChatMessageRepository groupChatMessageRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;
    @Mock EventRepository eventRepository;
    @Mock NotificationService notificationService;
    @InjectMocks ChatService chatService;

    @Test
    void createsWithMockitoDependencies() {
        assertThat(chatService).isNotNull();
    }
}
