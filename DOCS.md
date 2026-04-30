# 📖 Ween Backend - Technical Documentation

## 📋 Table of Contents

1. **[Installation & Setup](#installation--setup)**
2. **[Environment Variables](#environment-variables)**
3. **[API Endpoints](#api-endpoints)**
4. **[Database Schema](#database-schema)**
5. **[Usage Examples](#usage-examples)**
6. **[Troubleshooting](#troubleshooting)**

---

## Installation & Setup

### Prerequisites

- **Java 17** or higher
- **Maven 3.8+** (or use bundled `mvnw`)
- **MySQL 8.0+**
- **Git**

### Step 1: Clone Repository

```bash
git clone <repository-url>
cd weenBackend
```

### Step 2: Create MySQL Database

```sql
CREATE DATABASE weendb CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'ween_user'@'localhost' IDENTIFIED BY 'secure_password';
GRANT ALL PRIVILEGES ON weendb.* TO 'ween_user'@'localhost';
FLUSH PRIVILEGES;
```

### Step 3: Configure Environment

Create a `.env` file in the project root:

```bash
# Database Configuration
DB_URL=jdbc:mysql://localhost:3306/weendb
DB_USERNAME=ween_user
DB_PASSWORD=secure_password

# Email Configuration
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-email@gmail.com
MAIL_PASSWORD=your-app-password
MAIL_FROM=your-email@gmail.com

# JWT Configuration
JWT_SECRET=your-secret-key-min-32-characters-required
ACCESS_TOKEN_EXPIRY=900
REFRESH_TOKEN_EXPIRY=604800

# AES Encryption
AES_SECRET_KEY=16-character-key

# Firebase Configuration
FIREBASE_CREDS_PATH=/path/to/firebase-admin-key.json

# Application Settings
PORT=5050
ORGANIZER_API_KEY=your-api-key-here
CORS_ORIGINS=http://localhost:3000,http://localhost:5001
VERIFY_EMAIL_URL=http://localhost:5001/verify
RESET_PASSWORD_URL=http://localhost:5001/reset-password
```

### Step 4: Build & Run

```bash
# Build the project
./mvnw clean package

# Run the application
./mvnw spring-boot:run

# Application will be available at http://localhost:5050
```

### Step 5: Verify Installation

Open browser and navigate to:
```
http://localhost:5050/swagger-ui.html
```

You should see the Swagger API documentation.

---

## Environment Variables

### Database Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `DB_URL` | MySQL JDBC connection string | `jdbc:mysql://localhost:3306/weendb` | Yes |
| `DB_USERNAME` | Database user | `root` | Yes |
| `DB_PASSWORD` | Database password | `toor` | Yes |

### Email Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `MAIL_HOST` | SMTP server | `smtp.gmail.com` | Yes |
| `MAIL_PORT` | SMTP port | `587` | Yes |
| `MAIL_USERNAME` | Email account | Required | Yes |
| `MAIL_PASSWORD` | Email app password | Required | Yes |
| `MAIL_FROM` | Sender email | From MAIL_USERNAME | No |

**Note**: For Gmail, use [App Passwords](https://support.google.com/accounts/answer/185833) instead of regular password.

### JWT Configuration

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `JWT_SECRET` | Secret key for token signing (min 32 chars) | `your-secret-key-...` | Yes |
| `ACCESS_TOKEN_EXPIRY` | Access token lifetime (seconds) | `900` (15 min) | No |
| `REFRESH_TOKEN_EXPIRY` | Refresh token lifetime (seconds) | `604800` (7 days) | No |

### Security & Encryption

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `AES_SECRET_KEY` | AES encryption key (16 chars) | Required | Yes |
| `FIREBASE_CREDS_PATH` | Firebase admin credentials JSON | `/app/config/...` | No |
| `ORGANIZER_API_KEY` | API key for organizer operations | Required | Yes |

### Application Settings

| Variable | Description | Default | Required |
|----------|-------------|---------|----------|
| `PORT` | Application port | `5050` | No |
| `CORS_ORIGINS` | Allowed CORS origins (comma-separated) | See defaults | No |
| `VERIFY_EMAIL_URL` | Email verification link URL | `http://localhost:5001/verify` | No |
| `RESET_PASSWORD_URL` | Password reset link URL | `http://localhost:5001/reset-password` | No |

---

## API Endpoints

### Base URL
```
http://localhost:5050/api/v1
```

### Authentication Endpoints

#### Register User
```http
POST /auth/register
Content-Type: application/json

{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "SecurePassword123!",
  "firstName": "John",
  "lastName": "Doe",
  "referralCode": "OPTIONAL_CODE"
}

Response: 201 Created
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": {
    "id": "uuid",
    "username": "john_doe",
    "email": "john@example.com",
    "role": "USER"
  }
}
```

#### Register Organization
```http
POST /auth/register-organization
Content-Type: application/json

{
  "username": "org_admin",
  "email": "admin@org.com",
  "password": "SecurePassword123!",
  "organizationName": "Green Initiative",
  "registrationNumber": "ORG123456"
}

Response: 201 Created
```

#### Login
```http
POST /auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "SecurePassword123!"
}

Response: 200 OK
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc...",
  "user": { ... }
}
```

#### Refresh Token
```http
POST /auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGc..."
}

Response: 200 OK
{
  "accessToken": "eyJhbGc...",
  "refreshToken": "eyJhbGc..."
}
```

#### Verify Email
```http
POST /auth/verify-email
Content-Type: application/json

{
  "token": "email-verification-token"
}

Response: 200 OK
{
  "message": "Email verified successfully"
}
```

---

### Event Endpoints

#### List Events (with Filters)
```http
GET /events?category=ENVIRONMENT&status=ACTIVE&page=0&size=10
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "content": [
    {
      "id": "uuid",
      "title": "Beach Cleanup Drive",
      "description": "Join us for a beach cleanup initiative",
      "category": "ENVIRONMENT",
      "status": "ACTIVE",
      "startDate": "2026-05-15T09:00:00",
      "endDate": "2026-05-15T12:00:00",
      "maxCapacity": 50,
      "registeredCount": 42,
      "organization": { ... }
    }
  ],
  "totalElements": 150,
  "totalPages": 15
}
```

#### Create Event
```http
POST /events
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "title": "Beach Cleanup",
  "description": "Help clean our beaches",
  "category": "ENVIRONMENT",
  "startDate": "2026-05-15T09:00:00",
  "endDate": "2026-05-15T12:00:00",
  "location": "Santa Monica Beach",
  "maxCapacity": 50,
  "imageUrl": "https://example.com/image.jpg"
}

Response: 201 Created
{
  "id": "uuid",
  "title": "Beach Cleanup",
  ...
}
```

#### Get Event Details
```http
GET /events/{eventId}
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "id": "uuid",
  "title": "Beach Cleanup",
  "description": "Help clean our beaches",
  "category": "ENVIRONMENT",
  "status": "ACTIVE",
  "startDate": "2026-05-15T09:00:00",
  "endDate": "2026-05-15T12:00:00",
  "maxCapacity": 50,
  "registeredCount": 42,
  "registrations": [
    {
      "id": "uuid",
      "userId": "uuid",
      "status": "ACTIVE",
      "registeredAt": "2026-04-30T10:00:00",
      "checkedInAt": "2026-05-15T09:15:00"
    }
  ]
}
```

#### Register for Event
```http
POST /events/{eventId}/register
Authorization: Bearer {accessToken}

Response: 201 Created
{
  "id": "uuid",
  "eventId": "event-uuid",
  "userId": "user-uuid",
  "status": "PENDING",
  "registeredAt": "2026-04-30T10:00:00"
}
```

#### Unregister from Event
```http
DELETE /events/{eventId}/register
Authorization: Bearer {accessToken}

Response: 204 No Content
```

---

### User Endpoints

#### Get Current User
```http
GET /users/me
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "id": "uuid",
  "username": "john_doe",
  "email": "john@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "role": "USER",
  "coins": 250,
  "leaderboardRank": 5,
  "totalHoursVolunteered": 24,
  "eventCount": 8
}
```

#### Update Profile
```http
PUT /users/{userId}
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "firstName": "Jonathan",
  "lastName": "Doe",
  "bio": "Passionate volunteer"
}

Response: 200 OK
```

#### Change Password
```http
POST /users/change-password
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "currentPassword": "OldPassword123!",
  "newPassword": "NewPassword123!"
}

Response: 200 OK
```

---

### Certificate Endpoints

#### Generate Certificate
```http
POST /certificates/{eventId}/generate
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "id": "uuid",
  "eventId": "event-uuid",
  "userId": "user-uuid",
  "certificateNumber": "CERT-2026-00001",
  "qrCode": "data:image/png;base64,...",
  "generatedAt": "2026-05-15T16:00:00",
  "pdf": "base64-encoded-pdf"
}
```

#### Get User Certificates
```http
GET /certificates/user/{userId}
Authorization: Bearer {accessToken}

Response: 200 OK
[
  {
    "id": "uuid",
    "eventId": "event-uuid",
    "certificateNumber": "CERT-2026-00001",
    "eventTitle": "Beach Cleanup",
    "generatedAt": "2026-05-15T16:00:00"
  }
]
```

---

### Leaderboard Endpoints

#### Get Leaderboard (Daily)
```http
GET /leaderboard?period=DAILY&limit=10
Authorization: Bearer {accessToken}

Response: 200 OK
[
  {
    "rank": 1,
    "userId": "uuid",
    "username": "alice_wonder",
    "coins": 1500,
    "hours": 120
  },
  {
    "rank": 2,
    "userId": "uuid",
    "username": "bob_builder",
    "coins": 1200,
    "hours": 100
  }
]
```

#### Get Monthly Leaderboard
```http
GET /leaderboard?period=MONTHLY&limit=10
Authorization: Bearer {accessToken}
```

---

### Coin Endpoints

#### Get User Coins
```http
GET /coins/balance
Authorization: Bearer {accessToken}

Response: 200 OK
{
  "userId": "uuid",
  "totalCoins": 250,
  "transactions": [
    {
      "id": "uuid",
      "reason": "EVENT_COMPLETION",
      "amount": 50,
      "description": "Completed Beach Cleanup",
      "timestamp": "2026-05-15T12:30:00"
    }
  ]
}
```

---

### QR Code Endpoints

#### Generate QR Code
```http
POST /qr/generate
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "eventId": "event-uuid",
  "userId": "user-uuid"
}

Response: 200 OK
{
  "qrCode": "data:image/png;base64,...",
  "token": "qr-token-uuid"
}
```

---

### Organization Endpoints

#### Create Organization
```http
POST /organizations
Content-Type: application/json
Authorization: Bearer {accessToken}

{
  "name": "Green Initiative",
  "description": "Environmental conservation NGO",
  "registrationNumber": "ORG123456",
  "website": "https://greeninitiative.org",
  "contactEmail": "contact@greeninitiative.org"
}

Response: 201 Created
```

---

## Database Schema

### Core Entities

#### Users Table
```sql
CREATE TABLE users (
  id BINARY(16) PRIMARY KEY,
  username VARCHAR(50) UNIQUE NOT NULL,
  email VARCHAR(150) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  first_name VARCHAR(100),
  last_name VARCHAR(100),
  role ENUM('ADMIN', 'ORGANIZER', 'USER') DEFAULT 'USER',
  coins INT DEFAULT 0,
  referral_code VARCHAR(50) UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_email (email),
  INDEX idx_username (username),
  INDEX idx_referral_code (referral_code)
);
```

#### Events Table
```sql
CREATE TABLE events (
  id BINARY(16) PRIMARY KEY,
  organization_id BINARY(16) NOT NULL,
  title VARCHAR(300) NOT NULL,
  description TEXT,
  category ENUM(...) NOT NULL,
  status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
  start_date DATETIME NOT NULL,
  end_date DATETIME NOT NULL,
  max_capacity INT NOT NULL,
  location VARCHAR(300),
  image_url VARCHAR(500),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (organization_id) REFERENCES organizations(id),
  INDEX idx_organization_id (organization_id),
  INDEX idx_status (status),
  INDEX idx_start_date (start_date)
);
```

#### Event Registrations Table
```sql
CREATE TABLE event_registrations (
  id BINARY(16) PRIMARY KEY,
  event_id BINARY(16) NOT NULL,
  user_id BINARY(16) NOT NULL,
  status ENUM('PENDING', 'ACTIVE', 'COMPLETED', 'CANCELLED') DEFAULT 'PENDING',
  registered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  checked_in_at TIMESTAMP,
  checked_out_at TIMESTAMP,
  hours_volunteered DECIMAL(5, 2),
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  FOREIGN KEY (event_id) REFERENCES events(id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE KEY unique_registration (event_id, user_id)
);
```

#### Certificates Table
```sql
CREATE TABLE certificates (
  id BINARY(16) PRIMARY KEY,
  event_id BINARY(16) NOT NULL,
  user_id BINARY(16) NOT NULL,
  certificate_number VARCHAR(50) UNIQUE NOT NULL,
  qr_code LONGTEXT NOT NULL,
  generated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  FOREIGN KEY (event_id) REFERENCES events(id),
  FOREIGN KEY (user_id) REFERENCES users(id)
);
```

---

## Usage Examples

### Example 1: Complete User Journey

```bash
# 1. Register as a new user
curl -X POST http://localhost:5050/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "john_volunteer",
    "email": "john@example.com",
    "password": "SecurePass123!",
    "firstName": "John",
    "lastName": "Volunteer"
  }'

# 2. Login
curl -X POST http://localhost:5050/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "john@example.com",
    "password": "SecurePass123!"
  }'
# Response contains: accessToken and refreshToken

# 3. Browse events
curl -X GET "http://localhost:5050/api/v1/events?category=ENVIRONMENT&status=ACTIVE&page=0&size=10" \
  -H "Authorization: Bearer {accessToken}"

# 4. Register for an event
curl -X POST http://localhost:5050/api/v1/events/{eventId}/register \
  -H "Authorization: Bearer {accessToken}"

# 5. Get leaderboard ranking
curl -X GET "http://localhost:5050/api/v1/leaderboard?period=DAILY&limit=10" \
  -H "Authorization: Bearer {accessToken}"
```

### Example 2: Organization Event Creation

```bash
# 1. Register as organization
curl -X POST http://localhost:5050/api/v1/auth/register-organization \
  -H "Content-Type: application/json" \
  -d '{
    "username": "green_org",
    "email": "admin@green.org",
    "password": "OrgPass123!",
    "organizationName": "Green Initiative",
    "registrationNumber": "ORG-2026-001"
  }'

# 2. Create an event
curl -X POST http://localhost:5050/api/v1/events \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {accessToken}" \
  -d '{
    "title": "Urban Garden Setup",
    "description": "Help us create community gardens",
    "category": "ENVIRONMENT",
    "startDate": "2026-06-01T09:00:00",
    "endDate": "2026-06-01T14:00:00",
    "location": "Central Park",
    "maxCapacity": 30,
    "imageUrl": "https://example.com/garden.jpg"
  }'

# 3. View event registrations
curl -X GET http://localhost:5050/api/v1/events/{eventId} \
  -H "Authorization: Bearer {accessToken}"
```

---

## Troubleshooting

### Issue: Database Connection Failed

**Error**: `java.sql.SQLException: Cannot get a connection, pool error Timeout waiting for idle object`

**Solution**:
1. Verify MySQL is running: `mysql -u root -p`
2. Check database exists: `SHOW DATABASES;`
3. Verify credentials in `.env` file
4. Check MySQL port (default 3306)

```bash
# Restart MySQL
sudo systemctl restart mysql  # Linux
brew services restart mysql  # macOS
```

---

### Issue: JWT Token Expired

**Error**: `401 Unauthorized: Token expired`

**Solution**: Use the refresh token to get a new access token:

```bash
curl -X POST http://localhost:5050/api/v1/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "{refreshToken}"}'
```

---

### Issue: Email Verification Not Sending

**Error**: Verification email not received

**Solution**:
1. Verify email configuration in `.env`:
   - `MAIL_HOST=smtp.gmail.com`
   - `MAIL_PORT=587`
   - Use Gmail App Password (not regular password)

2. Check mail logs:
   ```bash
   # In application logs, search for "MailException"
   ```

3. Verify from address matches configuration

---

### Issue: CORS Errors

**Error**: `Access to XMLHttpRequest blocked by CORS policy`

**Solution**: Update `CORS_ORIGINS` environment variable:

```bash
CORS_ORIGINS=http://localhost:3000,http://localhost:5001,https://yourdomain.com
```

Then restart the application.

---

### Issue: High Memory Usage

**Error**: Application running slowly or `OutOfMemoryError`

**Solution**: Increase JVM heap size:

```bash
export JAVA_OPTS="-Xms512m -Xmx1024m"
./mvnw spring-boot:run
```

---

## 🔗 Navigation

- ⬆️ [Back to Top](#-ween-backend---technical-documentation)
- 📋 [Main README](README.md)
- 🏗️ [Architecture & Flow](ARCHITECTURE.md)

---

**Last Updated**: April 2026  
**Documentation Version**: 1.0  
**Maintained By**: Ween Development Team
