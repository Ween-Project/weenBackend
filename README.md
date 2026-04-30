# 🌱 Ween Backend - Student Volunteering Platform

> **Empower students to make a difference. A comprehensive backend platform for discovering, organizing, and rewarding volunteering opportunities.**

---

## 📋 Table of Contents

- **[Project Overview](#project-overview)**
- **[Technical Flow & Architecture](#technical-flow--architecture)**
- **[Getting Started](#getting-started)**

---

## Project Overview

### About Ween

**Ween** is a full-featured student volunteering platform designed to bridge the gap between passionate volunteers and meaningful opportunities. The platform enables:

- **Discovery**: Browse and filter volunteering opportunities across multiple categories
- **Organization Management**: Create, manage, and track volunteering events
- **Reservation & Registration**: Reserve spots with real-time capacity tracking
- **Gamification**: Earn coins and climb leaderboards for volunteering contributions
- **Certification**: Generate verifiable certificates for completed volunteering work
- **Community Engagement**: Build referral networks and foster community growth

### Project Objectives

✅ Simplify volunteering opportunity discovery  
✅ Provide secure user authentication and authorization  
✅ Automate event management and registration workflows  
✅ Implement gamification mechanics (coins, leaderboards, referrals)  
✅ Support organizational management capabilities  
✅ Enable digital certification of volunteer work  
✅ Maintain high code quality with 70%+ test coverage  

---

## 🏗️ Tech Stack

| Category | Technology | Version |
|----------|-----------|---------|
| **Language** | Java | 17 |
| **Framework** | Spring Boot | 3.2.3 |
| **API** | Spring Web MVC | 3.2.3 |
| **Security** | Spring Security + JWT | 0.12.3 |
| **Database** | MySQL | 8.2.0 |
| **ORM** | Spring Data JPA | 3.2.3 |
| **Mapping** | MapStruct | 1.5.5 |
| **Documentation** | SpringDoc OpenAPI | 2.3.0 |
| **Email** | Spring Mail | 3.2.3 |
| **QR Codes** | ZXing | 3.5.3 |
| **Rate Limiting** | Bucket4j | 7.6.0 |
| **Build Tool** | Maven | 3.x |
| **Testing** | JUnit 5 + Testcontainers | Latest |
| **Validation** | Jakarta Bean Validation | Latest |

---

## 📦 Architecture Overview

Ween follows a **layered architecture pattern** with clear separation of concerns:

```
Controller Layer (REST Endpoints)
        ↓
Service Layer (Business Logic)
        ↓
Repository Layer (Data Access)
        ↓
Database (MySQL)
```

### Core Modules

| Module | Purpose | Key Components |
|--------|---------|-----------------|
| **Authentication** | User identity & access management | JWT tokens, session management, email verification |
| **Events** | Event lifecycle management | Create, update, list, register, filter events |
| **Users** | User profile & account management | Registration, profile updates, role management |
| **Organizations** | Org admin capabilities | Event creation, member management, analytics |
| **Gamification** | Reward system | Coins, leaderboards (daily/monthly), referrals |
| **Certificates** | Digital credentials | QR code generation, template-based certificates |
| **Notifications** | Alert system | Email notifications, in-app alerts |
| **Admin** | Platform administration | User management, analytics, system settings |

---

## 🚀 Key Features

### 1. **User Management**
- Student & Organization registration with email verification
- Role-based access control (ADMIN, ORGANIZER, USER)
- Secure JWT-based authentication
- Password reset and account recovery

### 2. **Event Management**
- Create and manage volunteer events across 7+ categories
  - 🌍 Environment
  - 🏥 Health
  - 📚 Education
  - 🤝 Human Rights
  - 🔬 Technology
  - 🌐 International
  - 🎭 Culture
- Advanced filtering (category, date range, status)
- Real-time capacity tracking
- Event status transitions (PENDING → ACTIVE → COMPLETED)

### 3. **Registration & Reservation**
- Seamless event registration system
- Automatic capacity validation
- QR code generation for check-in
- Registration status tracking

### 4. **Gamification**
- **Coin System**: Earn coins for volunteering activities
- **Leaderboards**: Compete on daily and monthly basis
- **Referral Program**: Invite friends and earn rewards
- **Badges & Recognition**: Community-driven engagement

### 5. **Certification**
- Automatic certificate generation
- QR code integration for verification
- Template-based design per event category
- PDF export capabilities

### 6. **Admin Dashboard**
- User and organization management
- Event oversight and analytics
- System configuration
- Rate limiting and security controls

---

## 📊 API Structure

All endpoints follow RESTful conventions with OpenAPI/Swagger documentation.

**Base URL**: `http://localhost:5050/api/v1`

### Available Controllers

| Controller | Endpoint | Purpose |
|-----------|----------|---------|
| **AuthController** | `/auth` | Login, register, token refresh |
| **UserController** | `/users` | User profile management |
| **EventController** | `/events` | Event CRUD and discovery |
| **OrganizationController** | `/organizations` | Org management |
| **CertificateController** | `/certificates` | Certificate operations |
| **CoinController** | `/coins` | Coin management |
| **LeaderboardController** | `/leaderboard` | Rankings and statistics |
| **NotificationController** | `/notifications` | Alert management |
| **QrController** | `/qr` | QR code generation |
| **AdminController** | `/admin` | Admin operations |

---

## 🔒 Security Features

- ✅ Spring Security integration
- ✅ JWT token-based authentication (access + refresh tokens)
- ✅ Role-based authorization (RBAC)
- ✅ Email verification for account creation
- ✅ Password hashing and secure reset flow
- ✅ Rate limiting (Bucket4j)
- ✅ CORS configuration for frontend integration
- ✅ SQL injection prevention (JPA parameterized queries)

---

## 📈 Performance & Quality

- **Database Optimization**: Indexed queries on frequently searched columns
- **Connection Pooling**: HikariCP with optimized pool size (20 max, 5 min)
- **Batch Processing**: Hibernate batch inserts/updates (batch size: 10)
- **Code Coverage**: Target 70%+ coverage with JaCoCo
- **Asynchronous Processing**: Async email sending and notifications
- **Swagger Documentation**: Auto-generated API docs at `/swagger-ui.html`

---

## 📚 Documentation

For detailed information, refer to:

| Document | Content |
|----------|---------|
| **[DOCS.md](DOCS.md)** | Installation, environment setup, API endpoints, usage examples |
| **[ARCHITECTURE.md](ARCHITECTURE.md)** | System architecture, data flow diagrams, technical decision rationale |

---

## 🛠️ Quick Commands

```bash
# Build the project
./mvnw clean package

# Run locally
./mvnw spring-boot:run

# Run tests
./mvnw test

# Generate code coverage report
./mvnw clean test jacoco:report

# Access Swagger UI
# Navigate to: http://localhost:5050/swagger-ui.html
```

---

## 📝 Project Structure

```
ween-backend/
├── src/
│   ├── main/
│   │   ├── java/com/ween/
│   │   │   ├── controller/          # REST endpoints
│   │   │   ├── service/             # Business logic
│   │   │   ├── repository/          # Data access
│   │   │   ├── entity/              # JPA entities
│   │   │   ├── dto/                 # Request/Response DTOs
│   │   │   ├── enums/               # Enumeration types
│   │   │   ├── mapper/              # MapStruct mappers
│   │   │   ├── config/              # Spring configuration
│   │   │   ├── security/            # Security components
│   │   │   └── exception/           # Custom exceptions
│   │   └── resources/
│   │       ├── application.yml      # Main configuration
│   │       ├── templates/           # Email & certificate templates
│   │       └── db/migration/        # Flyway migrations
│   └── test/
│       ├── java/com/ween/           # Unit & integration tests
│       └── resources/
│           └── application-test.yml # Test configuration
├── pom.xml                          # Maven configuration
└── mvnw/mvnw.cmd                   # Maven wrapper
```

---

## 🤝 Contributing

Guidelines for contributing to Ween:

1. **Code Style**: Follow Spring/Java conventions
2. **Testing**: Maintain 70%+ code coverage
3. **Documentation**: Update README & API docs for new features
4. **Commits**: Use conventional commit messages
5. **Branches**: Feature branches prefixed with `feature/`, `bugfix/`, etc.

---

## 📞 Support & Contact

For issues, questions, or feedback:

- 📧 Email: weenorganization@gmail.com
- 🌐 Website: https://ween.az
- 📱 API: https://api.ween.az

---

## 📄 License

Proprietary - Ween Platform

---

## 🔗 Navigation

- ⬆️ [Back to Top](#-ween-backend---student-volunteering-platform)
- 📖 [Technical Documentation](DOCS.md)
- 🏗️ [Architecture & Flow Diagrams](ARCHITECTURE.md)

---

**Last Updated**: April 2026  
**Version**: 1.0.0  
**Status**: ✅ Production Ready
