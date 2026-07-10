package com.ween.controller;

import com.ween.dto.request.ChatMessageRequest;
import com.ween.dto.request.GroupMessageRequest;
import com.ween.dto.request.GroupRoomRequest;
import com.ween.dto.response.ApiResponse;
import com.ween.dto.response.ChatConversationResponse;
import com.ween.dto.response.ChatMessageResponse;
import com.ween.dto.response.GroupMessageResponse;
import com.ween.entity.ChatRoom;
import com.ween.security.SecurityUtil;
import com.ween.service.ChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
@Tag(name = "Chat", description = "Real-time and persisted user chat")
@SecurityRequirement(name = "Bearer")
public class ChatController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecurityUtil securityUtil;

    @GetMapping("/conversations")
    @Operation(summary = "Get chat conversations", description = "Retrieve latest message per conversation")
    public ResponseEntity<ApiResponse<List<ChatConversationResponse>>> getConversations() {
        String userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(chatService.getConversations(userId), "Conversations retrieved successfully"));
    }

    @GetMapping("/requests")
    @Operation(summary = "Get message requests", description = "Retrieve first-contact messages awaiting acceptance")
    public ResponseEntity<ApiResponse<Page<ChatConversationResponse>>> getMessageRequests(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(chatService.getMessageRequests(userId, pageable), "Message requests retrieved successfully"));
    }

    @PutMapping("/requests/{partnerId}/accept")
    @Operation(summary = "Accept a message request")
    public ResponseEntity<ApiResponse<Integer>> acceptMessageRequest(@PathVariable String partnerId) {
        String userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(chatService.acceptMessageRequest(userId, partnerId), "Message request accepted"));
    }

    @GetMapping("/rooms")
    @Operation(summary = "Get my group rooms", description = "Retrieve group and event chat rooms for the current user")
    public ResponseEntity<ApiResponse<List<ChatRoom>>> getRooms() {
        String userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(chatService.getUserGroupRooms(userId), "Rooms retrieved successfully"));
    }

    @GetMapping("/messages/{partnerId}")
    @Operation(summary = "Get conversation messages", description = "Retrieve paged messages with another user")
    public ResponseEntity<ApiResponse<Page<ChatMessageResponse>>> getMessages(
            @PathVariable String partnerId,
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(chatService.getConversation(userId, partnerId, pageable), "Messages retrieved successfully"));
    }

    @PostMapping("/messages")
    @Operation(summary = "Send chat message", description = "Send a persisted direct message and push it over WebSocket")
    public ResponseEntity<ApiResponse<ChatMessageResponse>> sendMessage(@Valid @RequestBody ChatMessageRequest request) {
        String userId = securityUtil.getCurrentUserId();
        ChatMessageResponse response = chatService.sendMessage(userId, request);

        messagingTemplate.convertAndSendToUser(response.getRecipientId(), "/queue/messages", response);
        messagingTemplate.convertAndSendToUser(response.getSenderId(), "/queue/messages", response);

        return ResponseEntity.ok(ApiResponse.ok(response, "Message sent successfully"));
    }

    @PutMapping("/messages/{partnerId}/read")
    @Operation(summary = "Mark conversation as read", description = "Mark all received messages from a user as read")
    public ResponseEntity<ApiResponse<Integer>> markAsRead(@PathVariable String partnerId) {
        String userId = securityUtil.getCurrentUserId();
        int updated = chatService.markConversationAsRead(userId, partnerId);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Messages marked as read"));
    }

    // --- GROUP ENDPOINTS ---

    @GetMapping("/rooms/{roomId}/messages")
    @Operation(summary = "Get group conversation messages", description = "Retrieve paged messages for a group room")
    public ResponseEntity<ApiResponse<Page<GroupMessageResponse>>> getGroupMessages(
            @PathVariable String roomId,
            @PageableDefault(size = 30, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        String userId = securityUtil.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.ok(chatService.getGroupMessages(userId, roomId, pageable), "Messages retrieved successfully"));
    }

    @PostMapping("/rooms/{roomId}/messages")
    @Operation(summary = "Send group chat message", description = "Send a persisted message to a group and push it over WebSocket")
    public ResponseEntity<ApiResponse<GroupMessageResponse>> sendGroupMessage(
            @PathVariable String roomId,
            @Valid @RequestBody GroupMessageRequest request) {
        String userId = securityUtil.getCurrentUserId();
        GroupMessageResponse response = chatService.sendGroupMessage(userId, roomId, request);

        List<String> memberIds = chatService.getRoomMemberIds(response.getChatRoomId());
        for (String memberId : memberIds) {
            messagingTemplate.convertAndSendToUser(memberId, "/queue/messages", response);
        }

        return ResponseEntity.ok(ApiResponse.ok(response, "Group message sent successfully"));
    }

    @PostMapping(value = "/rooms/group", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Create a custom group chat", description = "Create a new group and add creator as ADMIN")
    public ResponseEntity<ApiResponse<ChatRoom>> createGroup(
            @io.swagger.v3.oas.annotations.Parameter(content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json")) @Valid @org.springframework.web.bind.annotation.RequestPart("request") GroupRoomRequest request,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        String userId = securityUtil.getCurrentUserId();
        ChatRoom room = chatService.createGroupRoom(userId, request.getName(), photo);
        return ResponseEntity.ok(ApiResponse.ok(room, "Group created successfully"));
    }

    @PutMapping(value = "/rooms/{roomId}", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Update group info", description = "Update the name or photo of a group")
    public ResponseEntity<ApiResponse<Void>> updateGroupInfo(
            @PathVariable String roomId,
            @io.swagger.v3.oas.annotations.Parameter(content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json")) @Valid @org.springframework.web.bind.annotation.RequestPart("request") GroupRoomRequest request,
            @RequestParam(value = "photo", required = false) MultipartFile photo) {
        String userId = securityUtil.getCurrentUserId();
        chatService.updateRoomInfo(userId, roomId, request.getName(), photo);
        return ResponseEntity.ok(ApiResponse.ok(null, "Group updated successfully"));
    }

    @PostMapping("/rooms/{roomId}/members")
    @Operation(summary = "Add member to group", description = "Add a user by their username")
    public ResponseEntity<ApiResponse<Void>> addMember(
            @PathVariable String roomId,
            @RequestParam String username) {
        String userId = securityUtil.getCurrentUserId();
        chatService.addMemberToRoom(userId, roomId, username);
        return ResponseEntity.ok(ApiResponse.ok(null, "Member added successfully"));
    }

    @DeleteMapping("/rooms/{roomId}/members/{username}")
    @Operation(summary = "Remove member from group", description = "Remove a user by their username (Admin only)")
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @PathVariable String roomId,
            @PathVariable String username) {
        String userId = securityUtil.getCurrentUserId();
        chatService.removeMemberFromRoom(userId, roomId, username);
        return ResponseEntity.ok(ApiResponse.ok(null, "Member removed successfully"));
    }

    @DeleteMapping("/rooms/{roomId}/leave")
    @Operation(summary = "Leave a group", description = "Voluntarily leave a group chat")
    public ResponseEntity<ApiResponse<Void>> leaveGroup(@PathVariable String roomId) {
        String userId = securityUtil.getCurrentUserId();
        chatService.leaveRoom(userId, roomId);
        return ResponseEntity.ok(ApiResponse.ok(null, "Left group successfully"));
    }
}
