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
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.OrganizerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
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
    private final OrganizerRepository organizerRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final NotificationService notificationService;
    private final CloudinaryService cloudinaryService;

    @Value("${ween.chat.default-group-photo-url:https://placehold.co/200x200/e5e7eb/9ca3af.png}")
    private String defaultGroupPhotoUrl;

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
                        .photoUrl(defaultGroupPhotoUrl)
                        .creatorId(creatorId)
                        .build()));

        addMemberIfNotExists(room.getId(), creatorId, ChatRoomRole.ADMIN);
    }

    public ChatRoom createGroupRoom(String creatorId, String name, MultipartFile photo) {
        String roomName = name == null ? "" : name.trim();
        if (roomName.isBlank()) {
            throw new IllegalArgumentException("Group name is required");
        }

        String photoUrl = defaultGroupPhotoUrl;
        if (photo != null && !photo.isEmpty()) {
            photoUrl = uploadGroupPhoto(photo);
        }

        ChatRoom room = chatRoomRepository.save(ChatRoom.builder()
                .name(roomName)
                .type(ChatRoomType.GROUP)
                .photoUrl(photoUrl)
                .creatorId(creatorId)
                .build());
        addMemberIfNotExists(room.getId(), creatorId, ChatRoomRole.ADMIN);
        return room;
    }

    public void addUserToEventGroup(String eventId, String userId) {
        ChatRoom room = chatRoomRepository.findByEventId(eventId)
                .orElseThrow(() -> new ResourceNotFoundException("Event group chat has not been created yet"));

        addMemberIfNotExists(room.getId(), userId, ChatRoomRole.MEMBER);
    }

    public void removeUserFromEventGroup(String eventId, String userId) {
        chatRoomRepository.findByEventId(eventId).ifPresent(room -> {
            leaveRoom(userId, room.getId());
        });
    }

    public void addMemberToRoom(String requesterId, String roomId, String targetUsername) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getType() == ChatRoomType.DIRECT) {
            throw new IllegalArgumentException("Cannot add members to a direct chat");
        }

        ChatRoomMember requester = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new UnauthorizedException("You are not in this group"));
        
        if (requester.getRole() != ChatRoomRole.ADMIN) {
            throw new UnauthorizedException("Only group admins can add members");
        }

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + targetUsername));

        ChatRoomRole assignedRole = ChatRoomRole.MEMBER;

        if (room.getType() == ChatRoomType.EVENT) {
            Event event = eventRepository.findById(room.getEventId())
                    .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
            
            boolean isOrganizer = organizerRepository.findByUserId(targetUser.getId())
                    .map(org -> org.getOrganization().getId().equals(event.getOrganizationId()))
                    .orElse(false);
            
            if (isOrganizer) {
                assignedRole = ChatRoomRole.ADMIN;
            } else {
                boolean isRegistered = eventRegistrationRepository.findByEventIdAndUserId(event.getId(), targetUser.getId()).isPresent();
                if (!isRegistered) {
                    throw new IllegalArgumentException("Cannot add non-registered users to event group");
                }
            }
        }

        addMemberIfNotExists(roomId, targetUser.getId(), assignedRole);
    }

    public void changeMemberRole(String requesterId, String roomId, String targetUsername, ChatRoomRole newRole) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getType() == ChatRoomType.DIRECT) {
            throw new IllegalArgumentException("Cannot change roles in a direct chat");
        }

        ChatRoomMember requester = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new UnauthorizedException("You are not in this group"));

        if (requester.getRole() != ChatRoomRole.ADMIN) {
            throw new UnauthorizedException("Only group admins can change roles");
        }

        User targetUser = userRepository.findByUsername(targetUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with username: " + targetUsername));

        ChatRoomMember targetMember = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, targetUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Target user is not in this group"));

        if (targetMember.getRole() == newRole) {
            throw new IllegalArgumentException("User already has this role");
        }

        if (targetUser.getId().equals(requesterId) && newRole == ChatRoomRole.MEMBER) {
            long adminCount = chatRoomMemberRepository.findByChatRoomId(roomId).stream()
                    .filter(m -> m.getRole() == ChatRoomRole.ADMIN)
                    .count();
            if (adminCount <= 1) {
                throw new IllegalArgumentException("You are the only admin. Assign someone else as admin before demoting yourself.");
            }
        }

        targetMember.setRole(newRole);
        chatRoomMemberRepository.save(targetMember);
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

    public void updateRoomInfo(String requesterId, String roomId, String newName, MultipartFile photo) {
        ChatRoom room = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new ResourceNotFoundException("Room not found"));

        if (room.getType() == ChatRoomType.DIRECT) {
            throw new IllegalArgumentException("Cannot update direct chat info");
        }

        ChatRoomMember requester = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, requesterId)
                .orElseThrow(() -> new UnauthorizedException("You are not in this group"));

        if (requester.getRole() != ChatRoomRole.ADMIN) {
            throw new UnauthorizedException("Only group admins can update room info");
        }

        if (newName != null && !newName.isBlank()) {
            room.setName(newName);
        }
        if (photo != null && !photo.isEmpty()) {
            room.setPhotoUrl(uploadGroupPhoto(photo));
        }
        chatRoomRepository.save(room);
    }

    public void leaveRoom(String userId, String roomId) {
        ChatRoomMember leavingMember = chatRoomMemberRepository.findByChatRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));

        List<ChatRoomMember> members = chatRoomMemberRepository.findByChatRoomId(roomId);
        if (members.size() == 1) {
            groupChatMessageRepository.deleteByChatRoomId(roomId);
            chatRoomMemberRepository.deleteByChatRoomId(roomId);
            chatRoomRepository.deleteById(roomId);
            return;
        }

        if (leavingMember.getRole() == ChatRoomRole.ADMIN) {
            long adminCount = members.stream().filter(m -> m.getRole() == ChatRoomRole.ADMIN).count();
            if (adminCount == 1) {
                ChatRoomMember oldestMember = members.stream()
                        .filter(m -> !m.getUserId().equals(userId))
                        .min(java.util.Comparator.comparing(ChatRoomMember::getJoinedAt))
                        .orElse(null);
                if (oldestMember != null) {
                    oldestMember.setRole(ChatRoomRole.ADMIN);
                    chatRoomMemberRepository.save(oldestMember);
                }
            }
        }

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

    private String uploadGroupPhoto(MultipartFile photo) {
        try {
            return cloudinaryService.uploadFile(photo, "chat/groups");
        } catch (IOException e) {
            log.error("Failed to upload group photo to Cloudinary", e);
            throw new RuntimeException("Group photo upload failed", e);
        }
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
                .isRequest(message.getRequest())
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
