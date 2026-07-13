package com.ween.service;

import com.ween.dto.request.ChatMessageRequest;
import com.ween.dto.request.GroupMessageRequest;
import com.ween.dto.response.ChatMessageResponse;
import com.ween.dto.response.GroupMessageResponse;
import com.ween.entity.ChatMessage;
import com.ween.entity.GroupChatMessage;
import com.ween.entity.ChatRoom;
import com.ween.entity.ChatRoomMember;
import com.ween.entity.User;
import com.ween.entity.Event;
import com.ween.enums.ChatRoomRole;
import com.ween.enums.ChatRoomType;
import com.ween.enums.MessagePermission;
import com.ween.exception.ResourceNotFoundException;
import com.ween.exception.UnauthorizedException;
import com.ween.repository.ChatMessageRepository;
import com.ween.repository.ChatRoomMemberRepository;
import com.ween.repository.ChatRoomRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.FollowRepository;
import com.ween.repository.GroupChatMessageRepository;
import com.ween.repository.UserRepository;
import com.ween.repository.OrganizerRepository;
import com.ween.repository.EventRegistrationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock ChatMessageRepository chatMessageRepository;
    @Mock GroupChatMessageRepository groupChatMessageRepository;
    @Mock ChatRoomRepository chatRoomRepository;
    @Mock ChatRoomMemberRepository chatRoomMemberRepository;
    @Mock UserRepository userRepository;
    @Mock FollowRepository followRepository;
    @Mock EventRepository eventRepository;
    @Mock OrganizerRepository organizerRepository;
    @Mock EventRegistrationRepository eventRegistrationRepository;
    @Mock NotificationService notificationService;
    @Mock CloudinaryService cloudinaryService;

    @InjectMocks ChatService chatService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(chatService, "defaultGroupPhotoUrl", "https://placehold.co/200x200/e5e7eb/9ca3af.png");
    }

    @Test
    void createsWithMockitoDependencies() {
        assertThat(chatService).isNotNull();
    }

    @Test
    void sendMessageThrowsWhenContentIsBlank() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("  ");
        request.setRecipientId("user-2");

        assertThatThrownBy(() -> chatService.sendMessage("user-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message content is required");
    }

    @Test
    void sendMessageThrowsWhenRecipientIsSelf() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Salam");
        request.setRecipientId("user-1");

        assertThatThrownBy(() -> chatService.sendMessage("user-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot send a message to yourself");
    }

    @Test
    void sendMessageThrowsWhenSenderNotFound() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Salam");
        request.setRecipientId("user-2");

        when(userRepository.findById("user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage("user-1", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Sender not found");
    }

    @Test
    void sendMessageThrowsWhenRecipientNotFound() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Salam");
        request.setRecipientId("user-2");

        User sender = User.builder().username("sender").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sender));
        when(userRepository.findById("user-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendMessage("user-1", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Recipient not found");
    }

    @Test
    void sendMessageThrowsWhenNoPermissionNobody() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Salam");
        request.setRecipientId("user-2");

        User sender = User.builder().username("sender").build();
        User recipient = User.builder().username("recipient").messagePermission(MessagePermission.NOBODY).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sender));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(recipient));
        when(chatMessageRepository.conversationExists("user-1", "user-2")).thenReturn(false);

        assertThatThrownBy(() -> chatService.sendMessage("user-1", request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("This user only accepts messages from allowed connections");
    }

    @Test
    void sendMessageThrowsWhenNoPermissionFollowersOnlyAndSenderNotFollower() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Salam");
        request.setRecipientId("user-2");

        User sender = User.builder().username("sender").build();
        User recipient = User.builder().username("recipient").messagePermission(MessagePermission.FOLLOWERS).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sender));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(recipient));
        when(chatMessageRepository.conversationExists("user-1", "user-2")).thenReturn(false);
        when(followRepository.existsByFollowerAndFollowing(sender, recipient)).thenReturn(false);

        assertThatThrownBy(() -> chatService.sendMessage("user-1", request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("This user only accepts messages from allowed connections");
    }

    @Test
    void sendMessageSucceedsAndCreatesRoom() {
        ChatMessageRequest request = new ChatMessageRequest();
        request.setContent("Salam");
        request.setRecipientId("user-2");

        User sender = User.builder().username("sender").build();
        User recipient = User.builder().username("recipient").messagePermission(MessagePermission.EVERYONE).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sender));
        when(userRepository.findById("user-2")).thenReturn(Optional.of(recipient));
        when(chatMessageRepository.conversationExists("user-1", "user-2")).thenReturn(true);
        when(chatMessageRepository.acceptedConversationExists("user-1", "user-2")).thenReturn(true);
        when(chatRoomRepository.findDirectRoom("user-1", "user-2")).thenReturn(Optional.empty());

        ChatRoom savedRoom = ChatRoom.builder().type(ChatRoomType.DIRECT).participantOneId("user-1").participantTwoId("user-2").build();
        savedRoom.setId("room-1");
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(savedRoom);

        ChatMessage savedMessage = ChatMessage.builder()
                .chatRoomId("room-1")
                .senderId("user-1")
                .recipientId("user-2")
                .content("Salam")
                .request(false)
                .build();
        savedMessage.setId("msg-1");
        when(chatMessageRepository.save(any(ChatMessage.class))).thenReturn(savedMessage);

        ChatMessageResponse response = chatService.sendMessage("user-1", request);

        assertThat(response.getId()).isEqualTo("msg-1");
        assertThat(response.getContent()).isEqualTo("Salam");
        verify(chatMessageRepository).save(any(ChatMessage.class));
        verify(notificationService).createMessageNotification("user-2", "sender", "Salam");
    }

    @Test
    void sendGroupMessageThrowsWhenContentIsBlank() {
        GroupMessageRequest request = new GroupMessageRequest();
        request.setContent("   ");

        assertThatThrownBy(() -> chatService.sendGroupMessage("user-1", "room-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Message content is required");
    }

    @Test
    void sendGroupMessageThrowsWhenRoomNotFound() {
        GroupMessageRequest request = new GroupMessageRequest();
        request.setContent("Hello group");

        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendGroupMessage("user-1", "room-1", request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Chat room not found");
    }

    @Test
    void sendGroupMessageThrowsWhenRoomIsDirect() {
        GroupMessageRequest request = new GroupMessageRequest();
        request.setContent("Hello group");

        ChatRoom room = ChatRoom.builder().type(ChatRoomType.DIRECT).build();
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> chatService.sendGroupMessage("user-1", "room-1", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Use direct message endpoint for direct chats");
    }

    @Test
    void sendGroupMessageThrowsWhenSenderNotMember() {
        GroupMessageRequest request = new GroupMessageRequest();
        request.setContent("Hello group");

        ChatRoom room = ChatRoom.builder().type(ChatRoomType.GROUP).build();
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.sendGroupMessage("user-1", "room-1", request))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You are not a member of this group");
    }

    @Test
    void sendGroupMessageSucceedsAndNotifiesMembers() {
        GroupMessageRequest request = new GroupMessageRequest();
        request.setContent("Hello group");

        ChatRoom room = ChatRoom.builder().type(ChatRoomType.GROUP).name("Team").build();
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-1"))
                .thenReturn(Optional.of(ChatRoomMember.builder().chatRoomId("room-1").userId("user-1").build()));

        GroupChatMessage savedMessage = GroupChatMessage.builder()
                .chatRoomId("room-1")
                .senderId("user-1")
                .content("Hello group")
                .build();
        savedMessage.setId("gmsg-1");
        when(groupChatMessageRepository.save(any(GroupChatMessage.class))).thenReturn(savedMessage);

        User sender = User.builder().username("senderUser").build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(sender));

        ChatRoomMember member2 = ChatRoomMember.builder().chatRoomId("room-1").userId("user-2").build();
        when(chatRoomMemberRepository.findByChatRoomId("room-1")).thenReturn(List.of(
                ChatRoomMember.builder().chatRoomId("room-1").userId("user-1").build(),
                member2
        ));

        GroupMessageResponse response = chatService.sendGroupMessage("user-1", "room-1", request);

        assertThat(response.getId()).isEqualTo("gmsg-1");
        verify(groupChatMessageRepository).save(any(GroupChatMessage.class));
        verify(notificationService).createMessageNotification("user-2", "senderUser (Team)", "Hello group");
    }

    @Test
    void createGroupRoomThrowsWhenNameIsBlank() {
        assertThatThrownBy(() -> chatService.createGroupRoom("user-1", "  ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Group name is required");
    }

    @Test
    void createGroupRoomSucceeds() throws IOException {
        MultipartFile photo = mock(MultipartFile.class);
        when(photo.isEmpty()).thenReturn(false);
        when(cloudinaryService.uploadFile(photo, "chat/groups")).thenReturn("http://cloudinary.com/group-photo.png");

        ChatRoom room = ChatRoom.builder().name("Team").type(ChatRoomType.GROUP).photoUrl("http://cloudinary.com/group-photo.png").creatorId("user-1").build();
        room.setId("room-1");
        when(chatRoomRepository.save(any(ChatRoom.class))).thenReturn(room);

        ChatRoom created = chatService.createGroupRoom("user-1", "Team", photo);

        assertThat(created.getId()).isEqualTo("room-1");
        assertThat(created.getPhotoUrl()).isEqualTo("http://cloudinary.com/group-photo.png");
        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    void addMemberToRoomThrowsWhenRoomNotFound() {
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.addMemberToRoom("user-1", "room-1", "target"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Room not found");
    }

    @Test
    void addMemberToRoomThrowsWhenRoomIsDirect() {
        ChatRoom room = ChatRoom.builder().type(ChatRoomType.DIRECT).build();
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));

        assertThatThrownBy(() -> chatService.addMemberToRoom("user-1", "room-1", "target"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot add members to a direct chat");
    }

    @Test
    void addMemberToRoomThrowsWhenRequesterNotMember() {
        ChatRoom room = ChatRoom.builder().type(ChatRoomType.GROUP).build();
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.addMemberToRoom("user-1", "room-1", "target"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("You are not in this group");
    }

    @Test
    void addMemberToRoomThrowsWhenRequesterNotAdmin() {
        ChatRoom room = ChatRoom.builder().type(ChatRoomType.GROUP).build();
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-1"))
                .thenReturn(Optional.of(ChatRoomMember.builder().role(ChatRoomRole.MEMBER).build()));

        assertThatThrownBy(() -> chatService.addMemberToRoom("user-1", "room-1", "target"))
                .isInstanceOf(UnauthorizedException.class)
                .hasMessageContaining("Only group admins can add members");
    }

    @Test
    void addMemberToRoomThrowsWhenUserNotFound() {
        ChatRoom room = ChatRoom.builder().type(ChatRoomType.GROUP).build();
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-1"))
                .thenReturn(Optional.of(ChatRoomMember.builder().role(ChatRoomRole.ADMIN).build()));
        when(userRepository.findByUsername("target")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.addMemberToRoom("user-1", "room-1", "target"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with username: target");
    }

    @Test
    void addMemberToRoomSucceedsForGroupChat() {
        ChatRoom room = ChatRoom.builder().type(ChatRoomType.GROUP).build();
        room.setId("room-1");
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-1"))
                .thenReturn(Optional.of(ChatRoomMember.builder().role(ChatRoomRole.ADMIN).build()));

        User targetUser = User.builder().username("target").build();
        targetUser.setId("user-2");
        when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-2")).thenReturn(Optional.empty());

        chatService.addMemberToRoom("user-1", "room-1", "target");

        verify(chatRoomMemberRepository).save(any(ChatRoomMember.class));
    }

    @Test
    void changeMemberRoleThrowsWhenTargetMemberNotPresent() {
        ChatRoom room = ChatRoom.builder().type(ChatRoomType.GROUP).build();
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-1"))
                .thenReturn(Optional.of(ChatRoomMember.builder().role(ChatRoomRole.ADMIN).build()));

        User targetUser = User.builder().username("target").build();
        targetUser.setId("user-2");
        when(userRepository.findByUsername("target")).thenReturn(Optional.of(targetUser));
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-2")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatService.changeMemberRole("user-1", "room-1", "target", ChatRoomRole.ADMIN))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Target user is not in this group");
    }

    @Test
    void changeMemberRoleThrowsWhenDemotingOnlyAdmin() {
        ChatRoom room = ChatRoom.builder().type(ChatRoomType.GROUP).build();
        room.setId("room-1");
        when(chatRoomRepository.findById("room-1")).thenReturn(Optional.of(room));

        ChatRoomMember requester = ChatRoomMember.builder().chatRoomId("room-1").userId("user-1").role(ChatRoomRole.ADMIN).build();
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-1")).thenReturn(Optional.of(requester));

        User targetUser = User.builder().username("user-1").build();
        targetUser.setId("user-1");
        when(userRepository.findByUsername("user-1")).thenReturn(Optional.of(targetUser));

        when(chatRoomMemberRepository.findByChatRoomId("room-1")).thenReturn(List.of(requester));

        assertThatThrownBy(() -> chatService.changeMemberRole("user-1", "room-1", "user-1", ChatRoomRole.MEMBER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("You are the only admin");
    }

    @Test
    void leaveRoomDeletesRoomAndMessagesWhenLastMember() {
        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-1"))
                .thenReturn(Optional.of(ChatRoomMember.builder().build()));
        when(chatRoomMemberRepository.findByChatRoomId("room-1"))
                .thenReturn(List.of(ChatRoomMember.builder().build()));

        chatService.leaveRoom("user-1", "room-1");

        verify(groupChatMessageRepository).deleteByChatRoomId("room-1");
        verify(chatRoomMemberRepository).deleteByChatRoomId("room-1");
        verify(chatRoomRepository).deleteById("room-1");
    }

    @Test
    void leaveRoomPromotesOldestMemberWhenAdminLeavesAndOthersExist() {
        ChatRoomMember leavingAdmin = ChatRoomMember.builder().chatRoomId("room-1").userId("user-1").role(ChatRoomRole.ADMIN).build();
        ChatRoomMember otherMember = ChatRoomMember.builder().chatRoomId("room-1").userId("user-2").role(ChatRoomRole.MEMBER).joinedAt(LocalDateTime.now().minusHours(1)).build();
        ChatRoomMember anotherMember = ChatRoomMember.builder().chatRoomId("room-1").userId("user-3").role(ChatRoomRole.MEMBER).joinedAt(LocalDateTime.now()).build();

        when(chatRoomMemberRepository.findByChatRoomIdAndUserId("room-1", "user-1")).thenReturn(Optional.of(leavingAdmin));
        when(chatRoomMemberRepository.findByChatRoomId("room-1")).thenReturn(List.of(leavingAdmin, otherMember, anotherMember));

        chatService.leaveRoom("user-1", "room-1");

        assertThat(otherMember.getRole()).isEqualTo(ChatRoomRole.ADMIN);
        verify(chatRoomMemberRepository).save(otherMember);
        verify(chatRoomMemberRepository).deleteByChatRoomIdAndUserId("room-1", "user-1");
    }
}
