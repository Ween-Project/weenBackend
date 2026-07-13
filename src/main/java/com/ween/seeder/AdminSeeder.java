package com.ween.seeder;

import com.ween.entity.User;
import com.ween.enums.UserRole;
import com.ween.enums.MessagePermission;
import com.ween.repository.UserRepository;
import com.ween.repository.AuditLogRepository;
import com.ween.entity.AuditLog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogRepository auditLogRepository;

    @Value("${admin.seed.email:super_admin@ween.com}")
    private String adminEmail;

    @Value("${admin.seed.password:super1234!}")
    private String adminPassword;

    @Override
    public void run(String... args) {
        if (userRepository.existsByEmail(adminEmail)) {
            log.info("Super admin account already exists with email: {}", adminEmail);
            return;
        }

        User admin = User.builder()
                .username("super_admin")
                .email(adminEmail)
                .passwordHash(passwordEncoder.encode(adminPassword))
                .fullName("Super Admin")
                .role(UserRole.ADMIN)
                .messagePermission(MessagePermission.EVERYONE)
                .isEmailVerified(true)
                .referralCode("SUPERADMIN")
                .weenCoinBalance(0)
                .banned(false)
                .build();

        User savedAdmin = userRepository.save(admin);
        auditLogRepository.save(AuditLog.builder()
                .action("CREATE_ADMIN")
                .actorUsername("SYSTEM")
                .targetId(savedAdmin.getId())
                .targetName(savedAdmin.getUsername())
                .details("Created the configured startup super administrator account")
                .build());
        log.info("Successfully seeded super admin account with email: {}", adminEmail);
    }
}
