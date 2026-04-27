-- Create all main tables for Ween platform

CREATE TABLE users (
                       id CHAR(36) PRIMARY KEY,
                       username VARCHAR(50) UNIQUE NOT NULL,
                       email VARCHAR(150) UNIQUE NOT NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       full_name VARCHAR(120) NOT NULL,
                       category ENUM('HUMAN_RIGHTS','ENVIRONMENT','EDUCATION','HEALTH','TECHNOLOGY','CULTURE','INTERNATIONAL') DEFAULT 'INTERNATIONAL',
                       birth_date DATE,
                       phone VARCHAR(20),
                       university VARCHAR(150),
                       major VARCHAR(100),
                       bio TEXT,
                       profile_photo_url VARCHAR(500),
                       ween_coin_balance INT DEFAULT 0,
                       role ENUM('VOLUNTEER','ORGANIZER','ORGANIZATION_ADMIN','ADMIN') DEFAULT 'VOLUNTEER',
                       is_email_verified TINYINT(1) DEFAULT 0,
                       linkedin_url VARCHAR(300),
                       github_url VARCHAR(300),
                       interests JSON,
                       skills JSON,
                       referral_code VARCHAR(20) UNIQUE,
                       created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                       updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                       is_banned TINYINT(1) DEFAULT 0,
                       ban_reason TEXT NULL,
                       FULLTEXT KEY ft_user_search (username, full_name),
                       INDEX idx_email (email),
                       INDEX idx_username (username),
                       INDEX idx_referral_code (referral_code),
                       INDEX idx_role (role),
                       INDEX idx_created_at (created_at DESC),
                       INDEX idx_is_email_verified (is_email_verified),
                       INDEX idx_is_banned (is_banned)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE organizations (
                               id CHAR(36) PRIMARY KEY,
                               username VARCHAR(50) UNIQUE,
                               email VARCHAR(150) UNIQUE,
                               password_hash VARCHAR(255) NOT NULL,
                               name VARCHAR(200) NOT NULL,
                               description TEXT,
                               logo_url VARCHAR(500),
                               contact_email VARCHAR(150),
                               website VARCHAR(300),
                               category VARCHAR(50),
                               subscription_plan ENUM('FREE','STARTER','PROFESSIONAL','ENTERPRISE') DEFAULT 'FREE',
                               owner_id CHAR(36),
                               is_verified TINYINT(1) DEFAULT 0,
                               verification_note TEXT NULL,
                               created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                               INDEX idx_owner_id (owner_id),
                               INDEX idx_subscription_plan (subscription_plan),
                               INDEX idx_created_at (created_at DESC),
                               INDEX idx_org_username (username),
                               INDEX idx_org_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE events (
                        id CHAR(36) PRIMARY KEY,
                        title VARCHAR(300) NOT NULL,
                        description TEXT,
                        category ENUM('HUMAN_RIGHTS','ENVIRONMENT','EDUCATION','HEALTH','TECHNOLOGY','CULTURE','INTERNATIONAL') NOT NULL,
                        city VARCHAR(100),
                        address VARCHAR(300),
                        is_online TINYINT(1) DEFAULT 0,
                        start_date DATETIME(6),
                        end_date DATETIME(6),
                        registration_deadline DATETIME(6),
                        max_participants INT,
                        organization_id CHAR(36) NOT NULL,
                        status ENUM('DRAFT','PUBLISHED','ONGOING','COMPLETED','CANCELLED') DEFAULT 'DRAFT',
                        cover_image_url VARCHAR(500),
                        custom_fields JSON,
                        created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                        updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                        FULLTEXT KEY ft_event_search (title, description),
                        INDEX idx_organization_id (organization_id),
                        INDEX idx_status (status),
                        INDEX idx_start_date (start_date),
                        INDEX idx_category (category),
                        INDEX idx_city (city),
                        INDEX idx_organization_created (organization_id, created_at DESC),
                        INDEX idx_category_status (category, status),
                        INDEX idx_city_status (city, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE event_registrations (
                                     id CHAR(36) PRIMARY KEY,
                                     event_id CHAR(36) NOT NULL,
                                     user_id CHAR(36) NOT NULL,
                                     registered_at DATETIME(6),
                                     custom_answers JSON,
                                     is_joined TINYINT(1) DEFAULT 0,
                                     joined_at DATETIME(6),
                                     created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                                     updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                     UNIQUE KEY uq_event_user (event_id, user_id),
                                     INDEX idx_event_id (event_id),
                                     INDEX idx_user_id (user_id),
                                     INDEX idx_joined (is_joined),
                                     INDEX idx_user_event (user_id, event_id),
                                     INDEX idx_joined_event (is_joined, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE qr_tokens (
                           id CHAR(36) PRIMARY KEY,
                           user_id CHAR(36) NOT NULL,
                           token_hash VARCHAR(1000) NOT NULL,
                           issued_at DATETIME(6),
                           expires_at DATETIME(6),
                           is_revoked TINYINT(1) DEFAULT 0,
                           created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                           updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                           INDEX idx_user_id (user_id),
                           INDEX idx_revoked (is_revoked)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE certificates (
                              id CHAR(36) PRIMARY KEY,
                              user_id CHAR(36) NOT NULL,
                              event_id CHAR(36) NOT NULL,
                              certificate_number VARCHAR(30) UNIQUE,
                              pdf_url VARCHAR(500),
                              template_type ENUM('GENERAL','INTERNATIONAL','SEMINAR','PROJECT','LEADER','SPECIAL') DEFAULT 'GENERAL',
                              issued_at DATETIME(6),
                              created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                              updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                              UNIQUE KEY uq_user_event_cert (user_id, event_id),
                              INDEX idx_user_id (user_id),
                              INDEX idx_event_id (event_id),
                              INDEX idx_certificate_number (certificate_number),
                              INDEX idx_user_created (user_id, created_at DESC),
                              INDEX idx_user_event (user_id, event_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE coin_transactions (
                                   id CHAR(36) PRIMARY KEY,
                                   user_id CHAR(36) NOT NULL,
                                   amount INT NOT NULL,
                                   reason ENUM('SIGNUP','REGISTRATION','ATTENDANCE','CERTIFICATE','PROFILE_COMPLETE','REFERRAL','INTERNATIONAL','LEADERBOARD_BONUS','ANNUAL_ACHIEVEMENT') NOT NULL,
                                   related_entity_id CHAR(36),
                                   created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                                   updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                   INDEX idx_user_id (user_id),
                                   INDEX idx_reason (reason),
                                   INDEX idx_created_at (created_at),
                                   INDEX idx_user_reason (user_id, reason),
                                   INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE leaderboard_entries (
                                     id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                     user_id CHAR(36) NOT NULL,
                                     period ENUM('MONTHLY','QUARTERLY','ANNUAL','ALL_TIME') NOT NULL,
                                     scope ENUM('GLOBAL','REGIONAL','UNIVERSITY','FRIENDS') NOT NULL,
                                     rank_position INT,
                                     coin_count INT,
                                     calculated_at DATETIME(6),
                                     created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                                     updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                                     UNIQUE KEY uq_user_period_scope (user_id, period, scope),
                                     INDEX idx_user_id (user_id),
                                     INDEX idx_period_scope (period, scope),
                                     INDEX idx_rank (rank_position),
                                     INDEX idx_period_scope_rank (period, scope, rank_position)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE notifications (
                               id CHAR(36) PRIMARY KEY,
                               user_id CHAR(36) NOT NULL,
                               type ENUM('EVENT_REMINDER','ATTENDANCE_CONFIRMED','CERTIFICATE_READY','COIN_EARNED','SYSTEM') NOT NULL,
                               title VARCHAR(200),
                               body TEXT,
                               is_read TINYINT(1) DEFAULT 0,
                               created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                               updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                               INDEX idx_user_id (user_id),
                               INDEX idx_is_read (is_read),
                               INDEX idx_created_at (created_at),
                               INDEX idx_user_unread (user_id, is_read),
                               INDEX idx_user_created (user_id, created_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE referrals (
                           id CHAR(36) PRIMARY KEY,
                           referrer_id CHAR(36) NOT NULL,
                           referred_id CHAR(36) NOT NULL,
                           coin_awarded TINYINT(1) DEFAULT 0,
                           created_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6),
                           updated_at DATETIME(6) DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
                           UNIQUE KEY uq_referrer_referred (referrer_id, referred_id),
                           INDEX idx_referrer_id (referrer_id),
                           INDEX idx_referred_id (referred_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;