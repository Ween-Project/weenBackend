package com.ween.controller;

import com.ween.dto.request.ChatMessageRequest;
import com.ween.dto.request.GroupMessageRequest;
import com.ween.dto.response.ChatMessageResponse;
import com.ween.dto.response.GroupMessageResponse;
import com.ween.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;

import java.security.Principal;

@Controller
@Validated
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.send")
    public void sendMessage(@Valid @Payload ChatMessageRequest request, Principal principal) {
        ChatMessageResponse response = chatService.sendMessage(principal.getName(), request);
        messagingTemplate.convertAndSendToUser(response.getRecipientId(), "/queue/messages", response);
        messagingTemplate.convertAndSendToUser(response.getSenderId(), "/queue/messages", response);
    }

    @MessageMapping("/chat.group.send/{roomId}")
    public void sendGroupMessage(@DestinationVariable String roomId, @Valid @Payload GroupMessageRequest request, Principal principal) {
        GroupMessageResponse response = chatService.sendGroupMessage(principal.getName(), roomId, request);
        messagingTemplate.convertAndSend("/topic/group/" + roomId, response);
    }
}
