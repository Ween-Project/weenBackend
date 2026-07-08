package com.ween.controller;

import com.ween.dto.request.ChatMessageRequest;
import com.ween.dto.request.GroupMessageRequest;
import com.ween.dto.response.ChatMessageResponse;
import com.ween.dto.response.GroupMessageResponse;
import com.ween.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatWebSocketControllerTest {

    private ChatService chatService;
    private SimpMessagingTemplate messagingTemplate;
    private ChatWebSocketController controller;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        controller = new ChatWebSocketController(chatService, messagingTemplate);
    }

    @Test
    void sendMessagePublishesToSenderAndRecipientQueues() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipientId("user-2");
        request.setContent("Salam");
        ChatMessageResponse response = ChatMessageResponse.builder()
                .senderId("user-1")
                .recipientId("user-2")
                .content("Salam")
                .build();
        when(chatService.sendMessage("user-1", request)).thenReturn(response);

        controller.sendMessage(request, ControllerTestSupport.principal("user-1"));

        verify(messagingTemplate).convertAndSendToUser("user-2", "/queue/messages", response);
        verify(messagingTemplate).convertAndSendToUser("user-1", "/queue/messages", response);
    }

    @Test
    void sendGroupMessagePublishesToTopic() {
        GroupMessageRequest request = new GroupMessageRequest();
        request.setContent("Hello");
        GroupMessageResponse response = GroupMessageResponse.builder().chatRoomId("room-1").content("Hello").build();
        when(chatService.sendGroupMessage("user-1", "room-1", request)).thenReturn(response);

        controller.sendGroupMessage("room-1", request, ControllerTestSupport.principal("user-1"));

        verify(messagingTemplate).convertAndSend("/topic/group/room-1", response);
    }
}
