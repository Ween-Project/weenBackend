package com.ween.service;

import com.ween.dto.request.ChatMessageRequest;
import com.ween.dto.request.GroupMessageRequest;
import com.ween.dto.response.ChatConversationResponse;
import com.ween.dto.response.ChatMessageResponse;
import com.ween.dto.response.GroupMessageResponse;
import com.ween.entity.ChatMessage;
import com.ween.entity.GroupChatMessage;
import com.ween.entity.ChatRoom;
import com.ween.entity.ChatRoomMember;
import com.ween.entity.Event;
import com.ween.entity.User;
import com.ween.enums.ChatRoomRole;
import com.ween.enums.ChatRoomType;
import com.ween.enums.MessagePermission;
import com.ween.exception.ResourceNotFoundException;
import com.ween.exception.UnauthorizedException;
import com.ween.repository.ChatMessageRepository;
import com.ween.repository.GroupChatMessageRepository;
import com.ween.repository.ChatRoomMemberRepository;
import com.ween.repository.ChatRoomRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.UserRepository;
import com.ween.repository.FollowRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class ChatService {

    private final ChatMessageRepository chatMessageRepository;
    private final GroupChatMessageRepository groupChatMessageRepository;
    private final ChatRoomRepository chatRoomRepository;
    private final ChatRoomMemberRepository chatRoomMemberRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;
    private final EventRepository eventRepository;
    private final NotificationService notificationService;
    private final CloudinaryService cloudinaryService;

    public ChatMessageResponse sendMessage(String senderId, ChatMessageRequest request) {
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isBlank()) {
            throw new IllegalArgumentException("Message content is required");
        }

        String recipientId = request.getRecipientId();
        if (senderId.equals(recipientId)) {
            throw new IllegalArgumentException("Cannot send a message to yourself");
        }
        User sender = userRepository.findById(senderId)
                .orElseThrow(() -> new ResourceNotFoundException("Sender not found"));
        User recipient = userRepository.findById(recipientId)
                .orElseThrow(() -> new ResourceNotFoundException("Recipient not found"));
        boolean existingConversation = chatMessageRepository.conversationExists(senderId, recipientId);
        boolean acceptedConversation = chatMessageRepository.acceptedConversationExists(senderId, recipientId);
        boolean senderFollowsRecipient = followRepository.existsByFollowerAndFollowing(sender, recipient);
        MessagePermission permission = recipient.getMessagePermission() == null
                ? MessagePermission.EVERYONE : recipient.getMessagePermission();
        if (!existingConversation && (permission == MessagePermission.NOBODY
                || (permission == MessagePermission.FOLLOWERS && !senderFollowsRecipient))) {
            throw new UnauthorizedException("This user only accepts messages from allowed connections");
        }
        boolean isRequest = !acceptedConversation
                && !followRepository.existsByFollowerAndFollowing(recipient, sender);

        ChatRoom room = chatRoomRepository.findDirectRoom(senderId, recipientId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .type(ChatRoomType.DIRECT)
                        .participantOneId(senderId)
                        .participantTwoId(recipientId)
                        .build()));

        ChatMessage message = ChatMessage.builder()
                .chatRoomId(room.getId())
                .senderId(senderId)
                .recipientId(recipientId)
                .content(content)
                .request(isRequest)
                .build();

        ChatMessage saved = chatMessageRepository.save(message);
        log.info("Direct chat message sent by {} to {}", senderId, recipientId);

        // Notify recipient
        userRepository.findById(senderId).ifPresent(senderUser -> {
            notificationService.createMessageNotification(recipientId, senderUser.getUsername(), content);
        });

        return toResponse(saved);
    }

    public GroupMessageResponse sendGroupMessage(String senderId, String roomId, GroupMessageRequest request) {
        String content = request.getContent() == null ? "" : request.getContent().trim();
        if (content.isBlank()) {
            throw new IllegalArgumentException("Message content is required");
        }

        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));

        if (room.getType() == ChatRoomType.DIRECT) {
            throw new IllegalArgumentException("Use direct message endpoint for direct chats");
        }

        chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, senderId)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this group"));

        GroupChatMessage message = GroupChatMessage.builder()
                .chatRoomId(roomId)
                .senderId(senderId)
                .content(content)
                .build();

        GroupChatMessage saved = groupChatMessageRepository.save(message);
        log.info("Group chat message sent by {} to room {}", senderId, roomId);

        // Notify other group members
        userRepository.findById(senderId).ifPresent(sender -> {
            String roomName = room.getName() != null ? room.getName() : "Qrup";
            List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
            for (ChatRoomMember member : members) {
                if (!member.getUserId().equals(senderId)) {
                    notificationService.createMessageNotification(member.getUserId(), sender.getUsername() + " (" + roomName + ")", content);
                }
            }
        });

        return toGroupResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ChatMessageResponse> getConversation(String userId, String partnerId, Pageable pageable) {
        if (!userRepository.existsById(partnerId)) {
            throw new ResourceNotFoundException("Chat partner not found with id: " + partnerId);
        }
        return chatMessageRepository.findConversationMessages(userId, partnerId, pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<GroupMessageResponse> getGroupMessages(String userId, String roomId, Pageable pageable) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Chat room not found"));

        if (room.getType() == ChatRoomType.DIRECT) {
            throw new IllegalArgumentException("Cannot use group endpoint for direct messages");
        }

        chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new UnauthorizedException("You are not a member of this group"));

        return groupChatMessageRepository.findByChatRoomId(roomId, pageable)
                .map(this::toGroupResponse);
    }

    @Transactional(readOnly = true)
    public List<ChatConversationResponse> getConversations(String userId) {
        return chatMessageRepository.findLatestMessagesByUser(userId).stream()
                .map(message -> toConversationResponse(userId, message))
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ChatConversationResponse> getMessageRequests(String userId, Pageable pageable) {
        return chatMessageRepository.findByRecipientIdAndRequestTrueOrderByCreatedAtDesc(userId, pageable)
                .map(message -> toConversationResponse(userId, message));
    }

    public int acceptMessageRequest(String userId, String partnerId) {
        return chatMessageRepository.acceptMessageRequest(userId, partnerId);
    }

    @Transactional(readOnly = true)
    public List<ChatRoom> getUserGroupRooms(String userId) {
        List<String> roomIds = chatRoomMemberRepository.findByUserId(userId).stream()
                .map(ChatRoomMember::getChatRoomId)
                .toList();
        return chatRoomRepository.findAllById(roomIds).stream()
                .filter(room -> room.getType() != ChatRoomType.DIRECT)
                .toList();
    }

    public int markConversationAsRead(String userId, String partnerId) {
        if (!userRepository.existsById(partnerId)) {
            throw new ResourceNotFoundException("Chat partner not found with id: " + partnerId);
        }
        return chatMessageRepository.markConversationAsRead(userId, partnerId, LocalDateTime.now());
    }

    // --- GROUP FEATURE METHODS ---


    public void createEventGroup(String eventId, String creatorId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        ChatRoom room = chatRoomRepository.findByEventId(eventId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .name(event.getTitle())
                        .type(ChatRoomType.EVENT)
                        .eventId(eventId)
                        .creatorId(creatorId)
                        .build()));

        addMemberIfNotExists(room.getId(), creatorId, ChatRoomRole.ADMIN);
    }

    public void addUserToEventGroup(String eventId, String userId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));

        ChatRoom room = chatRoomRepository.findByEventId(eventId)
                .orElseGet(() -> chatRoomRepository.save(ChatRoom.builder()
                        .name(event.getTitle())
                        .type(ChatRoomType.EVENT)
                        .eventId(eventId)
                        .build()));

        addMemberIfNotExists(room.getId(), userId, ChatRoomRole.MEMBER);
    }

    public void addMemberToRoom(String requesterId, String roomId, String targetUsername) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getType() == ChatRoomType.DIRECT) {
            throw new IllegalArgumentException("Cannot add members to a direct chat");
        }

        if (room.getType() == ChatRoomType.GROUP) {
            chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, requesterId)
                    .orElseThrow(() -> new UnauthorizedException("You are not in this group"));
        }

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + targetUsername));

        addMemberIfNotExists(roomId, targetUser.getId(), ChatRoomRole.MEMBER);
    }

    public void addOrganizerToEventGroup(String requesterId, String eventId, String targetUsername) {
        ChatRoom room = chatRoomRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event chat room not found"));

        ChatRoomMember requester = chatRoomMemberRepository.findByChatRoomIdAndUserId(room.getId(), requesterId)
                .orElseThrow(() -> new UnauthorizedException("You are not in this group"));

        if (requester.getRole() != ChatRoomRole.ADMIN) {
            throw new UnauthorizedException("Only admins can add organizers");
        }

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + targetUsername));

        addMemberIfNotExists(room.getId(), targetUser.getId(), ChatRoomRole.ADMIN);
    }

    public void removeMemberFromRoom(String requesterId, String roomId, String targetUsername) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        ChatRoomMember requester = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new UnauthorizedException("You are not in this group"));

        if (requester.getRole() != ChatRoomRole.ADMIN) {
            throw new UnauthorizedException("Only group admins can remove members");
        }

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        chatRoomMemberRepository.deleteByChatRoomIdAndUserId(roomId, targetUser.getId());
    }

    public void updateRoomInfo(String requesterId, String roomId, String newName, org.springframework.web.multipart.MultipartFile photo) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getType() == ChatRoomType.DIRECT) {
            throw new IllegalArgumentException("Cannot update direct chat info");
        }

        chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new UnauthorizedException("You are not in this group"));

        if (newName != null && !newName.isBlank()) {
            room.setName(newName);
        }
        if (photo != null && !photo.isEmpty()) {
            try {
                String photoUrl = cloudinaryService.uploadFile(photo, "profiles");
                room.setPhotoUrl(photoUrl);
            } catch (java.io.IOException e) {
                log.error("Failed to upload group photo to Cloudinary during update", e);
                throw new RuntimeException("Group photo upload failed", e);
            }
        }
        chatRoomRepository.save(room);
    }

    public void leaveRoom(String userId, String roomId) {
        chatRoomMemberRepository.deleteByChatRoomIdAndUserId(roomId, userId);
    }

    private void addMemberIfNotExists(String roomId, String userId, ChatRoomRole role) {
        Optional<ChatRoomMember> existing = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId);
        if (existing.isEmpty()) {
            chatRoomMemberRepository.save(ChatRoomMember.builder()
                    .chatRoomId(roomId)
                    .userId(userId)
                    .role(role)
                    .joinedAt(LocalDateTime.now())
                    .build());
        }
    }

    public List<String> getRoomMemberIds(String roomId) {
        return chatRoomMemberRepository.findByChatRoomId(roomId).stream()
                .map(ChatRoomMember::getUserId)
                .toList();
    }

    private ChatConversationResponse toConversationResponse(String userId, ChatMessage message) {
        String partnerId = null;
        if (message.getRecipientId() != null) {
            partnerId = message.getSenderId().equals(userId) ? message.getRecipientId() : message.getSenderId();
        }
        long unreadCount = partnerId != null ? chatMessageRepository.countBySenderIdAndRecipientIdAndReadAtIsNull(partnerId, userId) : 0;

        User partner = partnerId == null ? null : userRepository.findById(partnerId).orElse(null);
        return ChatConversationResponse.builder()
                .partnerId(partnerId)
                .partnerUsername(partner != null ? partner.getUsername() : null)
                .partnerFullName(partner != null ? partner.getFullName() : null)
                .partnerPhotoUrl(partner != null ? partner.getProfilePhotoUrl() : null)
                .lastMessageId(message.getId())
                .lastMessage(message.getContent())
                .lastSenderId(message.getSenderId())
                .lastMessageRead(message.getReadAt() != null)
                .unreadCount(unreadCount)
                .lastMessageAt(message.getCreatedAt())
                .build();
    }

    private ChatMessageResponse toResponse(ChatMessage message) {
        return ChatMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .recipientId(message.getRecipientId())
                .content(message.getContent())
                .read(message.getReadAt() != null)
                .readAt(message.getReadAt())
                .createdAt(message.getCreatedAt())
                .request(message.getRequest())
                .build();
    }

    private GroupMessageResponse toGroupResponse(GroupChatMessage message) {
        return GroupMessageResponse.builder()
                .id(message.getId())
                .senderId(message.getSenderId())
                .chatRoomId(message.getChatRoomId())
                .content(message.getContent())
                .createdAt(message.getCreatedAt())
                .build();
    }
}
