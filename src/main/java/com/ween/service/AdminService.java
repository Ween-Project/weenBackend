package com.ween.service;

import com.ween.dto.response.AdminStatsResponse;
import com.ween.dto.response.OrganizationResponse;
import com.ween.entity.Organization;
import com.ween.entity.User;
import com.ween.mapper.OrganizationMapper;
import com.ween.mapper.UserMapper;
import com.ween.repository.CertificateRepository;
import com.ween.repository.CoinTransactionRepository;
import com.ween.repository.EventRepository;
import com.ween.repository.EventRegistrationRepository;
import com.ween.repository.OrganizationRepository;
import com.ween.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

//    UserBanlamaq əlavə etmək

    public void banUser(String userId, String reason) {
        log.debug("Banning user: {} with reason: {}", userId, reason);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setBanned(true);
        user.setBanReason(reason);
        userRepository.save(user);
        
        log.info("User banned successfully: {}", userId);
    }
//    User bandan çıxarmaq əlavə etmək

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
        return null;
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
        
        log.info("Admin stats calculated: Users={}, Orgs={}, Events={}", totalUsers, totalOrganizations, totalEvents);
        return stats;
    }

    public void deleteUser(String userId) {
        log.debug("Deleting user: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        userRepository.delete(user);
        
        log.info("User deleted successfully: {}", userId);
    }

    public void deleteOrganization(String organizationId) {
        log.debug("Deleting organization: {}", organizationId);
        
        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new RuntimeException("Organization not found"));
        
        organizationRepository.delete(organization);
        
        log.info("Organization deleted successfully: {}", organizationId);
    }

    public com.ween.dto.response.UserResponse banUnbanUser(String id, Boolean ban, String reason) {
        return null;
    }

    public AdminStatsResponse getPlatformStatistics() {
        return null;
    }
}
