package com.ween.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ween.dto.request.ChatMessageRequest;
import com.ween.dto.request.GroupMessageRequest;
import com.ween.dto.request.GroupRoomRequest;
import com.ween.dto.response.ChatMessageResponse;
import com.ween.dto.response.GroupMessageResponse;
import com.ween.entity.ChatRoom;
import com.ween.security.SecurityUtil;
import com.ween.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class ChatControllerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private ChatService chatService;
    private SimpMessagingTemplate messagingTemplate;
    private SecurityUtil securityUtil;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        chatService = mock(ChatService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        securityUtil = mock(SecurityUtil.class);
        mockMvc = standaloneSetup(new ChatController(chatService, messagingTemplate, securityUtil))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
        when(securityUtil.getCurrentUserId()).thenReturn("user-1");
    }

    @Test
    void getConversationsUsesCurrentUser() throws Exception {
        when(chatService.getConversations("user-1")).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/chat/conversations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Conversations retrieved successfully"));
    }

    @Test
    void sendDirectMessagePushesToSenderAndRecipient() throws Exception {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setRecipientId("user-2");
        request.setContent("Salam");
        ChatMessageResponse response = ChatMessageResponse.builder()
                .id("msg-1")
                .senderId("user-1")
                .recipientId("user-2")
                .content("Salam")
                .build();
        when(chatService.sendMessage(any(), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/chat/messages")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value("msg-1"));

        verify(messagingTemplate).convertAndSendToUser("user-2", "/queue/messages", response);
        verify(messagingTemplate).convertAndSendToUser("user-1", "/queue/messages", response);
    }

    @Test
    void groupEndpointsDelegateToService() throws Exception {
        ChatRoom room = ChatRoom.builder().name("Team").build();
        room.setId("room-1");
        when(chatService.createGroupRoom("user-1", "Team", null)).thenReturn(room);
        when(chatService.getUserGroupRooms("user-1")).thenReturn(List.of(room));
        when(chatService.getGroupMessages(any(), any(), any()))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 30), 0));

        mockMvc.perform(get("/api/v1/chat/rooms")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/chat/rooms/room-1/messages")).andExpect(status().isOk());
        GroupRoomRequest request = new GroupRoomRequest();
        request.setName("Team");
        mockMvc.perform(multipart("/api/v1/chat/rooms/group")
                        .file(new org.springframework.mock.web.MockMultipartFile(
                                "request", "", "application/json", objectMapper.writeValueAsBytes(request))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Team"));
    }

    @Test
    void sendGroupMessagePushesToMembers() throws Exception {
        GroupMessageRequest request = new GroupMessageRequest();
        request.setContent("Hello group");
        GroupMessageResponse response = GroupMessageResponse.builder()
                .id("gmsg-1")
                .chatRoomId("room-1")
                .content("Hello group")
                .build();
        when(chatService.sendGroupMessage(any(), any(), any())).thenReturn(response);
        when(chatService.getRoomMemberIds("room-1")).thenReturn(List.of("user-1", "user-2"));

        mockMvc.perform(post("/api/v1/chat/rooms/room-1/messages")
                        .contentType("application/json")
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Group message sent successfully"));

        verify(messagingTemplate).convertAndSendToUser("user-1", "/queue/messages", response);
        verify(messagingTemplate).convertAndSendToUser("user-2", "/queue/messages", response);
    }

    @Test
    void acceptAndReadRequestsReturnCounts() throws Exception {
        when(chatService.acceptMessageRequest("user-1", "user-2")).thenReturn(1);
        when(chatService.markConversationAsRead("user-1", "user-2")).thenReturn(2);

        mockMvc.perform(put("/api/v1/chat/requests/user-2/accept"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(1));
        mockMvc.perform(put("/api/v1/chat/messages/user-2/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").value(2));
    }
}
