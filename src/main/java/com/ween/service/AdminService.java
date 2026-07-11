package com.ween.service;

import com.ween.dto.response.AdminStatsResponse;
import com.ween.dto.response.OrganizationResponse;
import com.ween.dto.response.UserResponse;
import com.ween.entity.Organization;
import com.ween.entity.User;
import com.ween.enums.EventStatus;
import com.ween.mapper.OrganizationMapper;
import com.ween.mapper.UserMapper;
import com.ween.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

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

    public Page<com.ween.dto.response.UserResponse> getAllUsers(String search, Pageable pageable) {
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

    public void unbanUser(String userId) {
        log.debug("Unbanning user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setBanned(false);
        user.setBanReason(null);
        userRepository.save(user);
        
        log.info("User unbanned successfully: {}", userId);
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
        
        organization.setVerified(true);
        organization.setVerificationNote(verificationNote);
        organizationRepository.save(organization);
        
        log.info("Organization verified successfully: {}", organizationId);
        return organizationMapper.toOrganizationResponse(organization);
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

    public AdminStatsResponse getAdminStats() {
        log.debug("Calculating admin statistics");
        
        long totalUsers = userRepository.count();
        long totalOrganizations = organizationRepository.count();
        long totalEvents = eventRepository.count();
        long totalRegistrations = eventRegistrationRepository.count();
        long totalAttendees = eventRegistrationRepository.countByIsJoinedTrue();
        long totalCertificatesIssued = certificateRepository.count();
        
        // Sum all coins from all transactions
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

    public void deleteUser(String userId) {
        log.debug("Deleting user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        jdbcTemplate.update("DELETE FROM chat_room_members WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM chat_messages WHERE sender_id = ? OR recipient_id = ?", userId, userId);
        jdbcTemplate.update("DELETE FROM group_chat_messages WHERE sender_id = ?", userId);
        
        jdbcTemplate.update("DELETE FROM post_likes WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM post_saves WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM post_comments WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM post_reposts WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM posts WHERE user_id = ?", userId);
        
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

    public UserResponse banUnbanUser(String id, Boolean ban, String reason) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found: " + id));

        if (Boolean.TRUE.equals(ban)) {
            if (reason == null || reason.isBlank()) {
                throw new IllegalArgumentException("Ban reason is required");
            }
            user.setBanned(true);
            user.setBanReason(reason);
            log.info("User banned: {} | Reason: {}", id, reason);
        } else {
            user.setBanned(false);
            user.setBanReason(null);
            log.info("User unbanned: {}", id);
        }

        return userMapper.toUserResponse(user);
    }
    public AdminStatsResponse getPlatformStatistics() {
        return null;
    }
}
