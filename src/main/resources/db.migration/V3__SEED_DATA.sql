-- Seed initial data for development/testing

-- Insert admin user (password: admin123)
INSERT INTO users (id, username, email, password_hash, full_name, ween_coin_balance, role, is_email_verified, referral_code, created_at)
VALUES (
    UUID(),
    'admin',
    'admin@ween.az',
    '$2a$12$5dGTSjTXjEbVOKiH8V8guu1mUETl.lXmxGVWRcAW.K.TqSaJ2TvDe',
    'Admin User',
    1000,
    'ADMIN',
    1,
    'ADMIN001',
    CURRENT_TIMESTAMP(6)
);

-- Insert sample organizers
INSERT INTO users (id, username, email, password_hash, full_name, university, ween_coin_balance, role, is_email_verified, referral_code, created_at)
VALUES (
    UUID(),
    'organizer1',
    'organizer1@ween.az',
    '$2a$12$5dGTSjTXjEbVOKiH8V8guu1mUETl.lXmxGVWRcAW.K.TqSaJ2TvDe',
    'Sarah Johnson',
    'Baku State University',
    500,
    'ORGANIZER',
    1,
    'ORG00001',
    CURRENT_TIMESTAMP(6)
);

-- Insert sample volunteers
INSERT INTO users (id, username, email, password_hash, full_name, university, ween_coin_balance, role, is_email_verified, referral_code, created_at)
VALUES 
(
    UUID(),
    'volunteer1',
    'volunteer1@ween.az',
    '$2a$12$5dGTSjTXjEbVOKiH8V8guu1mUETl.lXmxGVWRcAW.K.TqSaJ2TvDe',
    'Leyla Aliyeva',
    'Baku State University',
    150,
    'VOLUNTEER',
    1,
    'VOL00001',
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'volunteer2',
    'volunteer2@ween.az',
    '$2a$12$5dGTSjTXjEbVOKiH8V8guu1mUETl.lXmxGVWRcAW.K.TqSaJ2TvDe',
    'Ramin Hasanov',
    'ADA University',
    200,
    'VOLUNTEER',
    1,
    'VOL00002',
    CURRENT_TIMESTAMP(6)
);

-- Insert sample organizations
INSERT INTO organizations (id, name, description, subscription_plan, owner_id, is_verified, created_at)
VALUES (
    UUID(),
    'Green Future Initiative',
    'Environmental conservation and sustainability organization',
    'STARTER',
    (SELECT id FROM users WHERE username = 'organizer1'),
    1,
    CURRENT_TIMESTAMP(6)
);

-- Insert sample events
INSERT INTO events (id, title, description, category, city, is_online, start_date, end_date, registration_deadline, max_participants, organization_id, status, created_at)
VALUES 
(
    UUID(),
    'Beach Cleanup Drive',
    'Join us for a community effort to clean up our beaches and protect marine life.',
    'ENVIRONMENT',
    'Baku',
    0,
    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 7 DAY),
    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 7 DAY) + INTERVAL 2 HOUR,
    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 3 DAY),
    50,
    (SELECT id FROM organizations LIMIT 1),
    'PUBLISHED',
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'Environmental Workshop Series',
    'Learn about sustainable practices and climate action strategies.',
    'ENVIRONMENT',
    'Baku',
    1,
    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 14 DAY),
    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 14 DAY) + INTERVAL 3 HOUR,
    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 10 DAY),
    100,
    (SELECT id FROM organizations LIMIT 1),
    'PUBLISHED',
    CURRENT_TIMESTAMP(6)
),
(
    UUID(),
    'Tech for Good Hackathon',
    'Use your coding skills to solve real-world social problems.',
    'TECHNOLOGY',
    'Baku',
    1,
    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 30 DAY),
    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 31 DAY),
    DATE_ADD(CURRENT_TIMESTAMP(6), INTERVAL 25 DAY),
    75,
    (SELECT id FROM organizations LIMIT 1),
    'PUBLISHED',
    CURRENT_TIMESTAMP(6)
);