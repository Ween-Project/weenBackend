package com.ween.service;

import com.ween.dto.request.AdjustCoinsRequest;
import com.ween.dto.request.UpdateEventRequest;
import com.ween.dto.response.*;
import com.ween.entity.*;
import com.ween.enums.CoinReason;
import com.ween.enums.EventStatus;
import com.ween.enums.UserRole;
import com.ween.mapper.*;
import com.ween.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;
    private final EventRepository eventRepository;
    private final EventRegistrationRepository eventRegistrationRepository;
    private final CertificateRepository certificateRepository;
    private final CoinTransactionRepository coinTransactionRepository;
    private final UserMapper userMapper;
    private final OrganizationMapper organizationMapper;
    private final PostRepository postRepository;
    private final JdbcTemplate jdbcTemplate;

    private final AuditLogRepository auditLogRepository;
    private final PostCommentRepository postCommentRepository;
    private final CertificateMapper certificateMapper;
    private final CoinTransactionMapper coinTransactionMapper;
    private final EventMapper eventMapper;
    private final BadgeService badgeService;
    private final OrganizerRepository organizerRepository;
    private final ReferralRepository referralRepository;
    private final AiChatMessageRepository aiChatMessageRepository;
    private final PostMapper postMapper;

    private void logAction(String action, String actorId, String targetId, String targetName, String details) {
        User admin = userRepository.findById(actorId).orElse(null);
        String actorUsername = admin != null ? admin.getUsername() : "system";
        AuditLog auditLog = AuditLog.builder()
                .action(action)
                .actorId(actorId)
                .actorUsername(actorUsername)
                .targetId(targetId)
                .targetName(targetName)
                .details(details)
                .build();
        auditLogRepository.save(auditLog);
    }

    public Page<UserResponse> getAllUsers(String search, Pageable pageable) {
        log.debug("Fetching all users with search: {}", search);
        Page<User> users;
        if (search != null && !search.isBlank()) {
            users = userRepository.findByUsernameContainingIgnoreCaseOrEmailContainingIgnoreCase(
                    search, search, pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return users.map(userMapper::toUserResponse);
    }

    public void banUser(String userId, String reason) {
        log.debug("Banning user: {} with reason: {}", userId, reason);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBanned(true);
        user.setBanReason(reason);
        userRepository.save(user);
        log.info("User banned successfully: {}", userId);
    }

    public void banUser(String userId, String reason, String adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        banUser(userId, reason);
        logAction("BAN_USER", adminId, userId, user.getUsername(), "Banned user account. Reason: " + reason);
    }

    public void unbanUser(String userId) {
        log.debug("Unbanning user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setBanned(false);
        user.setBanReason(null);
        userRepository.save(user);
        log.info("User unbanned successfully: {}", userId);
    }

    public void unbanUser(String userId, String adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        unbanUser(userId);
        logAction("UNBAN_USER", adminId, userId, user.getUsername(), "Unbanned user account.");
    }

    public Page<OrganizationResponse> getAllOrganizations(String search, Pageable pageable) {
        log.debug("Fetching all organizations with search: {}", search);
        Page<Organization> organizations;
        if (search != null && !search.isBlank()) {
            organizations = organizationRepository.findByOrganizationNameContainingIgnoreCaseOrDescriptionContainingIgnoreCase(
                    search, search, pageable);
        } else {
            organizations = organizationRepository.findAll(pageable);
        }
        return organizations.map(organizationMapper::toOrganizationResponse);
    }

    public OrganizationResponse verifyOrganization(String organizationId, Boolean verify, String verificationNote) {
        log.debug("Verifying organization: {} with note: {}", organizationId, verificationNote);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        organization.setVerified(verify);
        organization.setVerificationNote(verificationNote);
        organizationRepository.save(organization);
        log.info("Organization verification status updated: {}", organizationId);
        return organizationMapper.toOrganizationResponse(organization);
    }

    public OrganizationResponse verifyOrganization(String organizationId, Boolean verify, String note, String adminId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        OrganizationResponse response = verifyOrganization(organizationId, verify, note);
        logAction("VERIFY_ORGANIZATION", adminId, organizationId, organization.getOrganizationName(), 
                "Updated verification status to: " + verify + ". Note: " + note);
        return response;
    }

    public void rejectOrganization(String organizationId, String rejectionReason) {
        log.debug("Rejecting organization: {} with reason: {}", organizationId, rejectionReason);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        organization.setVerified(false);
        organization.setVerificationNote(rejectionReason);
        organizationRepository.save(organization);
        log.info("Organization rejected: {}", organizationId);
    }

    public void rejectOrganization(String organizationId, String rejectionReason, String adminId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        rejectOrganization(organizationId, rejectionReason);
        logAction("REJECT_ORGANIZATION", adminId, organizationId, organization.getOrganizationName(), 
                "Rejected organization account. Reason: " + rejectionReason);
    }

    public AdminStatsResponse getPlatformStatistics() {
        log.debug("Calculating admin statistics");
        long totalUsers = userRepository.count();
        long totalOrganizations = organizationRepository.count();
        long totalEvents = eventRepository.count();
        long totalRegistrations = eventRegistrationRepository.count();
        long totalAttendees = eventRegistrationRepository.countByIsJoinedTrue();
        long totalCertificatesIssued = certificateRepository.count();

        Long totalCoinsDistributed = coinTransactionRepository.sumAllCoins();
        if (totalCoinsDistributed == null) {
            totalCoinsDistributed = 0L;
        }

        AdminStatsResponse stats = new AdminStatsResponse();
        stats.setTotalUsers(totalUsers);
        stats.setTotalOrganizations(totalOrganizations);
        stats.setTotalEvents(totalEvents);
        stats.setTotalRegistrations(totalRegistrations);
        stats.setTotalAttendees(totalAttendees);
        stats.setTotalCoinsDistributed(totalCoinsDistributed);
        stats.setTotalCertificatesIssued(totalCertificatesIssued);
        stats.setTotalPosts(postRepository.count());
        stats.setVerifiedOrganizations(organizationRepository.countByIsVerified(true));
        stats.setPendingOrganizations(organizationRepository.countByIsVerified(false));
        stats.setBannedUsers(userRepository.countByBanned(true));
        stats.setPublishedEvents(eventRepository.countByStatus(EventStatus.PUBLISHED));

        log.info("Admin stats calculated: Users={}, Orgs={}, Events={}", totalUsers, totalOrganizations, totalEvents);
        return stats;
    }

    public AdminUserDetailResponse getUserDetails(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserResponse userResp = userMapper.toUserResponse(user);

        List<CertificateResponse> certificates = certificateRepository.findByUserId(userId).stream()
                .map(certificateMapper::toCertificateResponse)
                .toList();

        List<UserBadgeResponse> badges = badgeService.getUserBadges(userId);

        List<EventResponse> eventsAttended = eventRepository.findEventsByRegisteredUserId(userId, PageRequest.of(0, 100))
                .getContent().stream()
                .map(eventMapper::toEventResponse)
                .toList();

        List<EventResponse> eventsOrganized = new ArrayList<>();
        organizerRepository.findByUserId(userId).ifPresent(org -> {
            eventRepository.findByOrganizationId(org.getOrganization().getId()).forEach(e -> {
                eventsOrganized.add(eventMapper.toEventResponse(e));
            });
        });

        List<CoinTransactionResponse> coinTransactions = coinTransactionRepository.findAllByUserId(userId).stream()
                .map(coinTransactionMapper::toCoinTransactionResponse)
                .toList();

        return AdminUserDetailResponse.builder()
                .user(userResp)
                .certificates(certificates)
                .badges(badges)
                .eventsAttended(eventsAttended)
                .eventsOrganized(eventsOrganized)
                .coinTransactions(coinTransactions)
                .build();
    }

    public UserResponse changeUserRole(String userId, UserRole role, String adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserRole oldRole = user.getRole();
        user.setRole(role);
        userRepository.save(user);

        logAction("CHANGE_ROLE", adminId, userId, user.getUsername(), "Changed role from " + oldRole + " to " + role);
        return userMapper.toUserResponse(user);
    }

    public void deleteUser(String userId) {
        log.debug("Deleting user: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        jdbcTemplate.update("DELETE FROM chat_room_members WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM chat_messages WHERE sender_id = ? OR recipient_id = ?", userId, userId);
        jdbcTemplate.update("DELETE FROM group_chat_messages WHERE sender_id = ?", userId);

        jdbcTemplate.update("DELETE FROM post_likes WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM post_saves WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM post_comments WHERE author_id = ?", userId);
        jdbcTemplate.update("DELETE FROM post_reposts WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM posts WHERE author_id = ?", userId);
        jdbcTemplate.update("DELETE FROM ai_chat_messages WHERE user_id = ?", userId);

        jdbcTemplate.update("DELETE FROM event_registrations WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM participations WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM certificates WHERE user_id = ?", userId);

        jdbcTemplate.update("DELETE FROM follows WHERE follower_id = ? OR following_id = ?", userId, userId);
        jdbcTemplate.update("DELETE FROM referrals WHERE referrer_id = ? OR referred_id = ?", userId, userId);

        jdbcTemplate.update("DELETE FROM coin_transactions WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM user_badges WHERE user_id = ?", userId);

        jdbcTemplate.update("DELETE FROM organizers WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM organization_invitations WHERE user_id = ?", userId);

        jdbcTemplate.update("DELETE FROM notifications WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM qr_tokens WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM password_reset_tokens WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM email_verification_tokens WHERE user_id = ?", userId);

        userRepository.delete(user);
        log.info("User deleted successfully: {}", userId);
    }

    public void deleteUser(String userId, String adminId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        deleteUser(userId);
        logAction("DELETE_USER", adminId, userId, user.getUsername(), "Deleted user account: " + user.getEmail());
    }

    public void deleteOrganization(String organizationId) {
        log.debug("Deleting organization: {}", organizationId);
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));

        List<String> eventIds = jdbcTemplate.queryForList("SELECT id FROM events WHERE organization_id = ?", String.class, organizationId);
        for (String eventId : eventIds) {
            jdbcTemplate.update("DELETE FROM event_registrations WHERE event_id = ?", eventId);
            jdbcTemplate.update("DELETE FROM participations WHERE event_id = ?", eventId);
            jdbcTemplate.update("DELETE FROM certificates WHERE event_id = ?", eventId);

            List<String> roomIds = jdbcTemplate.queryForList("SELECT id FROM chat_rooms WHERE event_id = ?", String.class, eventId);
            for (String roomId : roomIds) {
                jdbcTemplate.update("DELETE FROM group_chat_messages WHERE chat_room_id = ?", roomId);
                jdbcTemplate.update("DELETE FROM chat_room_members WHERE chat_room_id = ?", roomId);
                jdbcTemplate.update("DELETE FROM chat_rooms WHERE id = ?", roomId);
            }
        }
        jdbcTemplate.update("DELETE FROM events WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM organizers WHERE organization_id = ?", organizationId);
        jdbcTemplate.update("DELETE FROM organization_invitations WHERE organization_id = ?", organizationId);

        organizationRepository.delete(organization);
        log.info("Organization deleted successfully: {}", organizationId);
    }

    public void deleteOrganization(String organizationId, String adminId) {
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        deleteOrganization(organizationId);
        logAction("DELETE_ORGANIZATION", adminId, organizationId, organization.getOrganizationName(), "Deleted organization account: " + organization.getEmail());
    }

    public void adjustCoins(AdjustCoinsRequest request, String adminId) {
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setWeenCoinBalance(user.getWeenCoinBalance() + request.getAmount());
        userRepository.save(user);

        CoinTransaction transaction = CoinTransaction.builder()
                .userId(request.getUserId())
                .amount(request.getAmount())
                .reason(CoinReason.ADMIN_ADJUSTMENT)
                .relatedEntityId(adminId)
                .build();
        coinTransactionRepository.save(transaction);

        logAction("ADJUST_COINS", adminId, request.getUserId(), user.getUsername(), 
                "Adjusted coins by " + request.getAmount() + ". Reason: " + request.getReason());
    }

    public Page<AuditLog> getAuditLogs(Pageable pageable) {
        return auditLogRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Page<EventResponse> getAllEvents(String search, Pageable pageable) {
        Page<Event> events;
        if (search != null && !search.isBlank()) {
            events = eventRepository.searchEvents(search, pageable);
        } else {
            events = eventRepository.findAll(pageable);
        }
        return events.map(eventMapper::toEventResponse);
    }

    public EventResponse updateEvent(String eventId, UpdateEventRequest request, String adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        if (request.getTitle() != null) event.setTitle(request.getTitle());
        if (request.getDescription() != null) event.setDescription(request.getDescription());
        if (request.getCategory() != null) event.setCategory(request.getCategory());
        if (request.getCity() != null) event.setCity(request.getCity());
        if (request.getAddress() != null) event.setAddress(request.getAddress());
        if (request.getIsOnline() != null) event.setIsOnline(request.getIsOnline());
        if (request.getStartDate() != null) event.setStartDate(request.getStartDate());
        if (request.getEndDate() != null) event.setEndDate(request.getEndDate());
        if (request.getRegistrationDeadline() != null) event.setRegistrationDeadline(request.getRegistrationDeadline());
        if (request.getMaxParticipants() != null) event.setMaxParticipants(request.getMaxParticipants());
        if (request.getStatus() != null) event.setStatus(request.getStatus());

        Event saved = eventRepository.save(event);
        logAction("UPDATE_EVENT", adminId, eventId, event.getTitle(), "Updated event details");
        return eventMapper.toEventResponse(saved);
    }

    public void deleteEvent(String eventId, String adminId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new RuntimeException("Event not found"));

        jdbcTemplate.update("DELETE FROM event_registrations WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM participations WHERE event_id = ?", eventId);
        jdbcTemplate.update("DELETE FROM certificates WHERE event_id = ?", eventId);

        List<String> roomIds = jdbcTemplate.queryForList("SELECT id FROM chat_rooms WHERE event_id = ?", String.class, eventId);
        for (String roomId : roomIds) {
            jdbcTemplate.update("DELETE FROM group_chat_messages WHERE chat_room_id = ?", roomId);
            jdbcTemplate.update("DELETE FROM chat_room_members WHERE chat_room_id = ?", roomId);
            jdbcTemplate.update("DELETE FROM chat_rooms WHERE id = ?", roomId);
        }

        eventRepository.delete(event);
        logAction("DELETE_EVENT", adminId, eventId, event.getTitle(), "Deleted event: " + event.getTitle());
    }

    public Page<ParticipantResponse> getEventRegistrations(String eventId, Pageable pageable) {
        Page<EventRegistration> registrationsPage = eventRegistrationRepository.findByEventId(eventId, pageable);
        List<ParticipantResponse> participants = registrationsPage.getContent().stream()
                .map(reg -> userRepository.findById(reg.getUserId())
                        .map(user -> ParticipantResponse.builder()
                                .id(user.getId())
                                .username(user.getUsername())
                                .fullName(user.getFullName())
                                .profilePhotoUrl(user.getProfilePhotoUrl())
                                .weenCoinBalance(user.getWeenCoinBalance())
                                .registeredAt(reg.getRegisteredAt())
                                .joinedAt(reg.getJoinedAt())
                                .isJoined(reg.getIsJoined())
                                .build())
                        .orElse(null))
                .filter(p -> p != null)
                .toList();
        return new PageImpl<>(participants, pageable, registrationsPage.getTotalElements());
    }

    public Page<PostResponse> getAllPosts(String search, Pageable pageable) {
        Page<Post> posts;
        if (search != null && !search.isBlank()) {
            posts = postRepository.searchPosts(search, pageable);
        } else {
            posts = postRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return posts.map(postMapper::toAdminPostResponse);
    }

    public void deletePost(String postId, String adminId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));

        jdbcTemplate.update("DELETE FROM post_likes WHERE post_id = ?", postId);
        jdbcTemplate.update("DELETE FROM post_saves WHERE post_id = ?", postId);
        jdbcTemplate.update("DELETE FROM post_reposts WHERE original_post_id = ? OR repost_id = ?", postId, postId);
        jdbcTemplate.update("DELETE FROM post_comments WHERE post_id = ?", postId);

        postRepository.delete(post);
        logAction("DELETE_POST", adminId, postId, "Post", "Deleted post: " + postId);
    }

    public Page<PostCommentResponse> getPostComments(String postId, Pageable pageable) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        Page<PostComment> comments = postCommentRepository.findByPostOrderByCreatedAtAsc(post, pageable);
        return comments.map(postMapper::toCommentResponse);
    }

    public void deleteComment(String commentId, String adminId) {
        PostComment comment = postCommentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        postCommentRepository.delete(comment);
        logAction("DELETE_COMMENT", adminId, commentId, "Comment", "Deleted comment: " + commentId);
    }

    public Page<CertificateResponse> getAllCertificates(String search, Pageable pageable) {
        Page<Certificate> certificates;
        if (search != null && !search.isBlank()) {
            certificates = certificateRepository.searchCertificates(search, pageable);
        } else {
            certificates = certificateRepository.findAll(pageable);
        }
        return certificates.map(certificateMapper::toCertificateResponse);
    }

    public void revokeCertificate(String certificateId, String adminId) {
        Certificate certificate = certificateRepository.findById(certificateId)
                .orElseThrow(() -> new RuntimeException("Certificate not found"));

        certificateRepository.delete(certificate);
        logAction("REVOKE_CERTIFICATE", adminId, certificateId, certificate.getCertificateNumber(), 
                "Revoked certificate: " + certificate.getCertificateNumber());
    }

    public Page<ReferralResponse> getReferralStats(Pageable pageable) {
        Page<Referral> referrals = referralRepository.findAll(pageable);
        return referrals.map(ref -> {
            String referrerName = userRepository.findById(ref.getReferrerId()).map(User::getFullName).orElse("Unknown");
            String referredName = userRepository.findById(ref.getReferredId()).map(User::getFullName).orElse("Unknown");
            return ReferralResponse.builder()
                    .id(ref.getId())
                    .referrerId(ref.getReferrerId())
                    .referrerName(referrerName)
                    .referredId(ref.getReferredId())
                    .referredName(referredName)
                    .coinAwarded(ref.getCoinAwarded())
                    .createdAt(ref.getCreatedAt())
                    .build();
        });
    }

    public AiStatsResponse getAiStats() {
        long totalMessages = aiChatMessageRepository.count();
        Long totalUsers = jdbcTemplate.queryForObject("SELECT COUNT(DISTINCT user_id) FROM ai_chat_messages", Long.class);
        if (totalUsers == null) {
            totalUsers = 0L;
        }
        return AiStatsResponse.builder()
                .totalMessages(totalMessages)
                .totalUsers(totalUsers)
                .build();
    }
}
