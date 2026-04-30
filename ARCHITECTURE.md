# 🏗️ Ween Backend - Architecture & Flow Documentation

## 📋 Table of Contents

1. **[System Architecture](#system-architecture)**
2. **[Data Flow Diagrams](#data-flow-diagrams)**
3. **[Core Workflows](#core-workflows)**
4. **[Database Schema Design](#database-schema-design)**
5. **[Security Architecture](#security-architecture)**
6. **[Scalability & Performance](#scalability--performance)**

---

## System Architecture

### Layered Architecture Pattern

Ween Backend follows a **4-layer architecture** with clear separation of concerns:

```
┌─────────────────────────────────────────────────────┐
│          Presentation Layer (REST API)               │
│  Controllers handle HTTP requests and responses      │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│          Service Layer (Business Logic)              │
│  Services contain core business rules and workflows  │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│       Repository Layer (Data Access Object)          │
│   Repositories manage database operations (CRUD)     │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│          Persistence Layer (Database)                │
│              MySQL 8.0 Database                      │
└─────────────────────────────────────────────────────┘
```

### Component Breakdown

| Layer | Components | Responsibility |
|-------|-----------|-----------------|
| **Presentation** | Controllers (9 total) | HTTP endpoint handling, request validation, response formatting |
| **Service** | Services (13 total) | Business logic, workflows, calculations, external service integration |
| **Data Access** | Repositories, EntityManagers | Query execution, ORM mapping, transaction management |
| **Persistence** | MySQL Database | Data storage, indexing, relationships |

### Cross-Cutting Concerns

```
┌─────────────────────────────────────────────────────┐
│  Security (Spring Security, JWT, RBAC)              │
│  Logging & Monitoring (SLF4J, JaCoCo)               │
│  Error Handling (Exception Handlers, Advice)        │
│  Validation (Bean Validation, Custom Validators)    │
│  Async Processing (Async Executors, Task Scheduling)│
│  Caching & Rate Limiting (Redis, Bucket4j)          │
└─────────────────────────────────────────────────────┘
          ↓
   [All Four Layers Above]
```

---

## Data Flow Diagrams

### 1. User Registration & Authentication Flow

```mermaid
sequenceDiagram
    participant User as 📱 User
    participant Controller as 🔗 Auth Controller
    participant Service as ⚙️ Auth Service
    participant Repo as 📦 User Repository
    participant DB as 🗄️ MySQL DB
    participant Email as 📧 Email Service

    User->>Controller: POST /auth/register
    activate Controller
    Controller->>Service: register(RegisterRequest)
    activate Service
    
    Service->>Service: Validate input
    Service->>Repo: findByEmail()
    activate Repo
    Repo->>DB: SELECT * FROM users WHERE email=?
    DB-->>Repo: null (not found)
    Repo-->>Service: Optional.empty()
    deactivate Repo
    
    Service->>Service: Hash password (BCrypt)
    Service->>Repo: save(User)
    activate Repo
    Repo->>DB: INSERT INTO users (...)
    DB-->>Repo: User (id, email, etc.)
    Repo-->>Service: Saved User
    deactivate Repo
    
    Service->>Service: Generate JWT tokens
    Service->>Email: sendVerificationEmail(email, token)
    activate Email
    Email->>Email: Generate verification link
    Email-->>User: 📨 Verification email sent
    deactivate Email
    
    Service-->>Controller: AuthResponse (accessToken, refreshToken)
    deactivate Service
    
    Controller-->>User: 201 Created + tokens
    deactivate Controller
```

### 2. Event Discovery & Filtering Flow

```mermaid
sequenceDiagram
    participant User as 👤 User
    participant Controller as 🔗 Event Controller
    participant Service as ⚙️ Event Service
    participant Repo as 📦 Event Repository
    participant Cache as 💾 Cache Layer
    participant DB as 🗄️ MySQL DB

    User->>Controller: GET /events?category=ENVIRONMENT&page=0
    activate Controller
    
    Controller->>Service: listEvents(filters, pageable)
    activate Service
    
    Service->>Cache: checkCache(filters, page)
    activate Cache
    Cache-->>Service: null (cache miss)
    deactivate Cache
    
    Service->>Repo: findByFilters(category, status, pageable)
    activate Repo
    
    Repo->>DB: SELECT * FROM events WHERE category=? AND status=?
    Note over DB: Uses index on (category, status, start_date)
    DB-->>Repo: List<Event> (50 results)
    Repo-->>Service: Page<EventResponse>
    deactivate Repo
    
    Service->>Cache: storeResults(filters, results, TTL)
    
    Service-->>Controller: EventPage
    deactivate Service
    
    Controller-->>User: 200 OK + paginated events
    deactivate Controller
```

### 3. Event Registration & Capacity Management Flow

```mermaid
sequenceDiagram
    participant User as 👤 User
    participant Controller as 🔗 Event Controller
    participant RegistrationSvc as ⚙️ Registration Service
    participant EventSvc as ⚙️ Event Service
    participant Repo as 📦 Repository
    participant DB as 🗄️ MySQL DB
    participant CoinSvc as 💰 Coin Service
    participant NotifSvc as 📬 Notification Service

    User->>Controller: POST /events/{id}/register
    activate Controller
    
    Controller->>RegistrationSvc: registerUserForEvent(eventId, userId)
    activate RegistrationSvc
    
    RegistrationSvc->>EventSvc: validateEventCapacity(eventId)
    activate EventSvc
    EventSvc->>Repo: findByIdWithLock(eventId)
    activate Repo
    Note over Repo: Pessimistic lock to prevent overbooking
    Repo->>DB: SELECT * FROM events WHERE id=? FOR UPDATE
    DB-->>Repo: Event object
    Repo-->>EventSvc: Event
    deactivate Repo
    
    EventSvc->>EventSvc: Check capacity<br/>registered < max
    EventSvc-->>RegistrationSvc: ✅ Capacity available
    deactivate EventSvc
    
    RegistrationSvc->>Repo: save(EventRegistration)
    activate Repo
    Repo->>DB: INSERT INTO event_registrations (...)
    DB-->>Repo: Saved registration
    Repo-->>RegistrationSvc: EventRegistration (status=PENDING)
    deactivate Repo
    
    par Async Operations
        RegistrationSvc->>CoinSvc: awardCoins(userId, REGISTRATION_BONUS, 10)
        RegistrationSvc->>NotifSvc: sendNotification(userId, "Event registered")
    end
    
    RegistrationSvc-->>Controller: EventRegistrationResponse
    deactivate RegistrationSvc
    
    Controller-->>User: 201 Created + registration details
    deactivate Controller
```

### 4. Certificate Generation & QR Code Flow

```mermaid
sequenceDiagram
    participant User as 👤 User
    participant Controller as 🔗 Certificate Controller
    participant CertSvc as ⚙️ Certificate Service
    participant QrSvc as 🔲 QR Service
    participant Repo as 📦 Repository
    participant DB as 🗄️ MySQL DB
    participant S3 as ☁️ Cloud Storage (Optional)

    User->>Controller: POST /certificates/{eventId}/generate
    activate Controller
    
    Controller->>CertSvc: generateCertificate(eventId, userId)
    activate CertSvc
    
    CertSvc->>Repo: findEventRegistration(eventId, userId)
    activate Repo
    Repo->>DB: SELECT * FROM event_registrations WHERE event_id=? AND user_id=?
    DB-->>Repo: EventRegistration
    Repo-->>CertSvc: Registration (status=COMPLETED)
    deactivate Repo
    
    CertSvc->>CertSvc: Validate completion status
    
    CertSvc->>QrSvc: generateQRCode(event, user, timestamp)
    activate QrSvc
    QrSvc->>QrSvc: Encode URL<br/>Generate PNG image
    QrSvc-->>CertSvc: QR code (base64)
    deactivate QrSvc
    
    CertSvc->>CertSvc: Generate certificate number<br/>(CERT-YYYY-XXXXX)
    
    CertSvc->>Repo: saveCertificate(Certificate)
    activate Repo
    Repo->>DB: INSERT INTO certificates (...)
    DB-->>Repo: Certificate
    Repo-->>CertSvc: Certificate object
    deactivate Repo
    
    CertSvc->>CertSvc: Generate PDF (template-based)<br/>with QR code
    
    opt Upload to S3
        CertSvc->>S3: putObject(certificate.pdf)
        S3-->>CertSvc: S3 URL
    end
    
    CertSvc-->>Controller: CertificateResponse (QR + PDF link)
    deactivate CertSvc
    
    Controller-->>User: 200 OK + certificate
    deactivate Controller
```

### 5. Gamification: Coin & Leaderboard Flow

```mermaid
sequenceDiagram
    participant EventCompletion as 📍 Event Completion
    participant CoinSvc as 💰 Coin Service
    participant LeaderSvc as 🏆 Leaderboard Service
    participant Repo as 📦 Repository
    participant Cache as 💾 Redis Cache
    participant DB as 🗄️ MySQL DB
    participant NotifSvc as 📬 Notification Service

    EventCompletion->>CoinSvc: awardCoins(userId, COMPLETION, amount)
    activate CoinSvc
    
    CoinSvc->>Repo: findUserCoinBalance(userId)
    activate Repo
    Repo->>Cache: get(user:{id}:coins)
    activate Cache
    Note over Cache: Check Redis cache first
    Cache-->>Repo: 250 (cached value)
    Repo-->>CoinSvc: CurrentBalance
    deactivate Cache
    deactivate Repo
    
    CoinSvc->>CoinSvc: Calculate new balance<br/>250 + 50 = 300 coins
    
    CoinSvc->>Repo: saveCoinTransaction(CoinTransaction)
    activate Repo
    Repo->>DB: BEGIN TRANSACTION
    Repo->>DB: INSERT INTO coin_transactions (...)
    Repo->>DB: UPDATE users SET coins=300 WHERE id=?
    Repo->>DB: COMMIT
    DB-->>Repo: Success
    Repo-->>CoinSvc: Transaction saved
    deactivate Repo
    
    CoinSvc->>Cache: invalidate(user:{id}:coins)
    activate Cache
    Cache-->>CoinSvc: ✅ Cache invalidated
    deactivate Cache
    
    CoinSvc->>LeaderSvc: updateLeaderboard(userId, DAILY)
    activate LeaderSvc
    
    LeaderSvc->>Repo: getRanking(userId, DAILY)
    activate Repo
    Repo->>DB: SELECT rank FROM leaderboard_daily WHERE user_id=? ORDER BY coins DESC
    DB-->>Repo: Rank (e.g., #5)
    Repo-->>LeaderSvc: CurrentRank
    deactivate Repo
    
    LeaderSvc->>Cache: updateLeaderboard(leaderboard_daily, data)
    LeaderSvc-->>CoinSvc: ✅ Leaderboard updated
    deactivate LeaderSvc
    
    CoinSvc->>NotifSvc: sendNotification(userId, "Gained 50 coins! Now ranked #5")
    NotifSvc-->>CoinSvc: ✅ Notification queued
    
    CoinSvc-->>EventCompletion: ✅ Coin award complete
    deactivate CoinSvc
```

### 6. Admin Analytics & Reporting Flow

```mermaid
sequenceDiagram
    participant Admin as 👨‍💼 Admin
    participant Controller as 🔗 Admin Controller
    participant AdminSvc as ⚙️ Admin Service
    participant Repo as 📦 Repository
    participant Cache as 💾 Aggregation Cache
    participant DB as 🗄️ MySQL DB

    Admin->>Controller: GET /admin/analytics?period=MONTHLY
    activate Controller
    
    Controller->>AdminSvc: getAnalytics(MONTHLY)
    activate AdminSvc
    
    AdminSvc->>Cache: checkAnalyticsCache(MONTHLY)
    activate Cache
    Cache-->>AdminSvc: null (cache miss, or expired)
    deactivate Cache
    
    AdminSvc->>Repo: getAnalytics(MONTHLY)
    activate Repo
    
    par Parallel Queries
        Repo->>DB: SELECT COUNT(*) FROM users WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        Repo->>DB: SELECT COUNT(*) FROM events WHERE status='COMPLETED' AND end_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        Repo->>DB: SELECT SUM(coins) FROM coin_transactions WHERE created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        Repo->>DB: SELECT AVG(registered_count/max_capacity) FROM events WHERE end_date >= DATE_SUB(NOW(), INTERVAL 30 DAY)
    end
    
    DB-->>Repo: Aggregated results
    Repo-->>AdminSvc: AnalyticsDTO
    deactivate Repo
    
    AdminSvc->>Cache: storeAnalytics(MONTHLY, data, TTL=1hour)
    
    AdminSvc-->>Controller: AnalyticsResponse
    deactivate AdminSvc
    
    Controller-->>Admin: 200 OK + analytics dashboard data
    deactivate Controller
```

---

## Core Workflows

### Workflow 1: User Registration to First Event

```mermaid
graph TD
    A["👤 User Signs Up"] -->|POST /auth/register| B["✅ Account Created"]
    B --> C["📧 Verification Email Sent"]
    C -->|User clicks link| D["✉️ Email Verified"]
    D --> E["🔐 Credentials Stored"]
    E --> F["📱 User Logs In"]
    F -->|JWT Token Generated| G["🎟️ Browse Events"]
    G -->|Filter by category| H["🔍 View Event Details"]
    H -->|Click Register| I["✋ Register for Event"]
    I -->|Capacity Check| J{Spots Available?}
    J -->|Yes| K["✅ Registration Confirmed"]
    J -->|No| L["❌ Event Full"]
    K --> M["🎁 Bonus Coins Awarded"]
    M --> N["📬 Confirmation Notification"]
    N --> O["✨ Event Added to Calendar"]
    O --> P["🏆 Leaderboard Updated"]
```

### Workflow 2: Event Completion to Certificate

```mermaid
graph TD
    A["📍 Volunteer Attends Event"] --> B["✅ Check-in at Event"]
    B -->|Scan QR Code| C["⏱️ Clock In Time"]
    C --> D["🔨 Participate in Event"]
    D --> E["⏱️ Clock Out"]
    E --> F["📊 Calculate Hours"]
    F --> G{Hours Valid?}
    G -->|Yes| H["✅ Mark as COMPLETED"]
    G -->|No| I["❌ Dispute Registration"]
    H --> J["🎁 Award Coins<br/>(Base + Bonus)"]
    J --> K["🏆 Update Leaderboard"]
    K --> L["📜 Generate Certificate"]
    L --> M["🔲 Embed QR Code"]
    M --> N["📄 Create PDF"]
    N --> O["📧 Email Certificate"]
    O --> P["🎉 Achievement Unlocked"]
```

### Workflow 3: Authorization & Access Control

```mermaid
graph TD
    A["HTTP Request"] -->|Include JWT| B["Extract Token"]
    B --> C["Validate Signature"]
    C -->|Valid| D["Extract User Claims"]
    C -->|Invalid| E["❌ 401 Unauthorized"]
    D --> F["Check Token Expiry"]
    F -->|Expired| G["❌ 401 Token Expired"]
    F -->|Valid| H["Load User Role"]
    H --> I["Check Endpoint Role"]
    I -->|Role Match| J["✅ Access Granted"]
    I -->|Role Mismatch| K["❌ 403 Forbidden"]
    J --> L["Continue to Controller"]
    L --> M["Execute Business Logic"]
```

---

## Database Schema Design

### Entity Relationship Diagram

```mermaid
erDiagram
    USERS ||--o{ EVENT_REGISTRATIONS : registers
    USERS ||--o{ CERTIFICATES : earns
    USERS ||--o{ COIN_TRANSACTIONS : "receives"
    USERS ||--o{ LEADERBOARD_ENTRY : "appears in"
    USERS ||--o{ REFERRAL : "creates"
    USERS ||--o{ NOTIFICATIONS : receives
    
    ORGANIZATIONS ||--o{ EVENTS : creates
    ORGANIZATIONS ||--o{ USERS : employs
    
    EVENTS ||--o{ EVENT_REGISTRATIONS : "has"
    EVENTS ||--o{ CERTIFICATES : "generates"
    EVENTS ||--o{ NOTIFICATIONS : "triggers"
    
    EMAIL_VERIFICATION_TOKEN ||--o{ USERS : verifies
    PASSWORD_RESET_TOKEN ||--o{ USERS : resets
    QR_TOKEN ||--o{ EVENT_REGISTRATIONS : generates
```

### Key Database Design Decisions

| Decision | Rationale | Implementation |
|----------|-----------|-----------------|
| **Binary UUID** | Efficient storage vs string UUIDs | BINARY(16) for all IDs |
| **Soft Deletes** | Maintain historical data | `deleted_at` timestamp (nullable) |
| **Temporal Tracking** | Audit trail for compliance | `created_at`, `updated_at` on all entities |
| **Indexing Strategy** | Query optimization | Multi-column indexes on frequently filtered columns |
| **Pessimistic Locking** | Prevent race conditions | `FOR UPDATE` on capacity checks |
| **Enum Storage** | Type safety + query efficiency | `ENUM` type for status fields |
| **Batch Operations** | Reduce database roundtrips | Hibernate batch size = 10 |
| **Connection Pooling** | Efficient resource management | HikariCP (20 max, 5 min) |

### Index Strategy

```sql
-- Users Table Indexes
CREATE INDEX idx_email ON users(email);
CREATE INDEX idx_username ON users(username);
CREATE INDEX idx_referral_code ON users(referral_code);

-- Events Table Indexes
CREATE INDEX idx_organization_id ON events(organization_id);
CREATE INDEX idx_status ON events(status);
CREATE INDEX idx_start_date ON events(start_date);
CREATE INDEX idx_category_status ON events(category, status, start_date);

-- Event Registrations Indexes
CREATE INDEX idx_event_id ON event_registrations(event_id);
CREATE INDEX idx_user_id ON event_registrations(user_id);
CREATE INDEX idx_status ON event_registrations(status);
CREATE UNIQUE INDEX idx_unique_registration ON event_registrations(event_id, user_id);

-- Leaderboard Indexes
CREATE INDEX idx_period_rank ON leaderboard_entry(period, rank);
CREATE INDEX idx_user_period ON leaderboard_entry(user_id, period);

-- Coin Transactions Indexes
CREATE INDEX idx_user_id ON coin_transactions(user_id);
CREATE INDEX idx_created_at ON coin_transactions(created_at);
```

---

## Security Architecture

### Authentication Flow

```mermaid
sequenceDiagram
    participant Client as 🖥️ Client/Frontend
    participant Filter as 🔐 JWT Filter
    participant Auth as 🔒 Auth Manager
    participant Provider as 👤 User Provider
    participant Controller as 🔗 Controller

    Client->>Filter: HTTP Request + Bearer Token
    activate Filter
    
    Filter->>Filter: Extract token from header
    Filter->>Auth: Validate JWT
    activate Auth
    
    Auth->>Auth: Verify signature
    Auth->>Auth: Check expiration
    Auth-->>Filter: Token valid + claims
    deactivate Auth
    
    Filter->>Provider: Load user details
    activate Provider
    Provider->>Provider: Query user from DB
    Provider-->>Filter: UserDetails
    deactivate Provider
    
    Filter->>Filter: Create Authentication
    Filter->>Filter: Set SecurityContext
    
    Filter-->>Controller: Request authorized
    deactivate Filter
    
    Controller->>Controller: Check @PreAuthorize
    Controller->>Controller: Execute endpoint
    Controller-->>Client: Response
```

### Security Layers

| Layer | Security Mechanism | Implementation |
|-------|-------------------|-----------------|
| **Transport** | HTTPS/TLS | Enforced in production |
| **Authentication** | JWT Tokens | HS256 signature, token expiry |
| **Authorization** | RBAC + @PreAuthorize | Role-based method security |
| **Input Validation** | Bean Validation + Custom | @NotNull, @Email, @Size, custom validators |
| **SQL Injection** | Parameterized queries | JPA/Hibernate ORM |
| **CSRF** | CSRF tokens | Disabled for stateless API |
| **Rate Limiting** | Bucket4j | Token bucket algorithm |
| **Encryption** | AES-256 | Sensitive field encryption |

### JWT Structure

```json
{
  "header": {
    "alg": "HS256",
    "typ": "JWT"
  },
  "payload": {
    "sub": "user-uuid",
    "username": "john_doe",
    "email": "john@example.com",
    "role": "USER",
    "iat": 1704067200,
    "exp": 1704153600
  }
}
```

---

## Scalability & Performance

### Caching Strategy

```mermaid
graph TD
    A["Request"] --> B{Check Cache}
    B -->|Hit| C["Return from Cache"]
    B -->|Miss| D["Query Database"]
    D --> E["Process Data"]
    E --> F["Store in Cache"]
    F --> G["Return to Client"]
    C --> G
    
    H["Cache Invalidation"] -->|TTL Expires| I["Cache Expired"]
    H -->|Manual Invalidate| I
    I --> J["Next request goes to DB"]
```

### Caching Layers

| Cache Type | Technology | Use Case | TTL |
|-----------|-----------|----------|-----|
| **Query Results** | Redis / Local | Event lists, leaderboards | 15-60 min |
| **User Sessions** | In-Memory | Active user data | Session lifetime |
| **Computed Data** | Redis | Analytics, aggregations | 1 hour |
| **Warm Data** | DB Query Cache | Frequently accessed events | DB-dependent |

### Performance Metrics

| Metric | Target | Implementation |
|--------|--------|-----------------|
| **Response Time** | < 200ms (p95) | Caching, indexing, pagination |
| **QPS** | 1000+ requests/sec | Connection pooling, async processing |
| **Database Queries** | < 2 per request | Query optimization, N+1 prevention |
| **Memory Usage** | < 1GB baseline | Lazy loading, pagination |

### Async Processing

```mermaid
graph TD
    A["Sync Request"] --> B["Return Response"]
    B --> C["Queue Async Task"]
    C --> D["Email Service<br/>(Background)"]
    C --> E["Notification Service<br/>(Background)"]
    C --> F["Analytics Update<br/>(Background)"]
    D --> G["✅ Tasks Complete"]
    E --> G
    F --> G
```

### Horizontal Scaling Considerations

```
┌──────────────────────────────────────────────────────┐
│              Load Balancer (Nginx)                    │
└──────┬──────────────────────────────────────────────┘
       │
   ┌───┴───┬───────────┬───────────┐
   │       │           │           │
┌──▼──┐ ┌──▼──┐    ┌──▼──┐    ┌──▼──┐
│App1 │ │App2 │    │App3 │    │App4 │
└──┬──┘ └──┬──┘    └──┬──┘    └──┬──┘
   │       │          │          │
   └───┬───┴──────────┴──────────┘
       │
   ┌───▼────────────────────────┐
   │ Centralized Database       │
   │ (MySQL + Replication)      │
   └────────────────────────────┘
       │
   ┌───▼────────────────────────┐
   │ Shared Cache               │
   │ (Redis Cluster)            │
   └────────────────────────────┘
```

---

## Event Status Transitions

### Valid State Transitions

```mermaid
graph TD
    PENDING -->|Event start time reached| ACTIVE
    ACTIVE -->|Event end time reached| COMPLETED
    PENDING -->|Admin cancels| CANCELLED
    ACTIVE -->|Admin cancels| CANCELLED
    COMPLETED -->|No further transitions| COMPLETED
    CANCELLED -->|No further transitions| CANCELLED
    
    style PENDING fill:#FFA500
    style ACTIVE fill:#00AA00
    style COMPLETED fill:#0000FF
    style CANCELLED fill:#FF0000
```

### Status Transition Rules

| From | To | Condition | Auto/Manual |
|-----|----|------------|------------|
| PENDING | ACTIVE | Event start time reached | Auto (scheduled job) |
| PENDING | CANCELLED | Admin action | Manual |
| ACTIVE | COMPLETED | Event end time passed | Auto (scheduled job) |
| ACTIVE | CANCELLED | Admin action (emergency) | Manual |
| COMPLETED | - | No transitions allowed | Immutable |
| CANCELLED | - | No transitions allowed | Immutable |

### Event Registration Status Transitions

```mermaid
graph TD
    PENDING -->|Event starts| ACTIVE
    ACTIVE -->|Event ends| COMPLETED
    PENDING -->|User unregisters| CANCELLED
    ACTIVE -->|User leaves early| CANCELLED
    COMPLETED -->|No transitions| COMPLETED
    CANCELLED -->|No transitions| CANCELLED
    
    style PENDING fill:#FFA500
    style ACTIVE fill:#00AA00
    style COMPLETED fill:#0000FF
    style CANCELLED fill:#FF0000
```

---

## Error Handling Architecture

### Exception Hierarchy

```
Exception
├── BusinessException (4xx errors)
│   ├── ValidationException
│   ├── ResourceNotFoundException
│   ├── DuplicateResourceException
│   ├── UnauthorizedException
│   └── ForbiddenException
├── SystemException (5xx errors)
│   ├── DatabaseException
│   ├── EmailException
│   ├── ExternalServiceException
│   └── InternalServerException
```

### Global Exception Handler

```mermaid
graph TD
    A["Exception Thrown"] --> B["ExceptionAdvice<br/>@RestControllerAdvice"]
    B --> C{Exception Type}
    C -->|BusinessException| D["400-403 Response"]
    C -->|SystemException| E["500 Response"]
    C -->|Unknown| F["500 Generic Error"]
    D --> G["Log & Response"]
    E --> G
    F --> G
    G --> H["Return to Client"]
```

---

## Deployment Architecture

### Development Environment

```
Developer Local Machine
├── Java 17 + Maven
├── MySQL (Docker or Local)
├── IDE (IntelliJ, VS Code)
└── Application (localhost:5050)
```

### Production Environment

```
┌─────────────────────────────────────────────┐
│         CDN (CloudFlare/AWS)                 │
│         (Static assets, images)              │
└────────────────┬────────────────────────────┘
                 │
┌────────────────▼────────────────────────────┐
│      Load Balancer (AWS ALB / Nginx)         │
│      (SSL/TLS termination, routing)          │
└────────────────┬────────────────────────────┘
                 │
    ┌────────────┼────────────┐
    │            │            │
┌───▼────┐  ┌───▼────┐  ┌───▼────┐
│  App   │  │  App   │  │  App   │
│Instance│  │Instance│  │Instance│
│ Pod 1  │  │ Pod 2  │  │ Pod 3  │
└───┬────┘  └───┬────┘  └───┬────┘
    │           │           │
    └───────────┼───────────┘
                │
        ┌───────▼────────────┐
        │  MySQL Database    │
        │  (Master-Slave)    │
        └─────────┬──────────┘
                  │
        ┌─────────▼──────────┐
        │  Redis Cache       │
        │  (Cluster)         │
        └────────────────────┘
```

---

## API Versioning Strategy

```
Current Version: v1 (/api/v1/*)

URI Versioning Pattern:
├── /api/v1/events    (Stable)
├── /api/v2/events    (Future improvements)
└── /api/v3/events    (Major redesign)

Backward Compatibility:
- v1 maintained for 12+ months
- Deprecation notices in response headers
- Migration guide provided before sunset
```

---

## 🔗 Navigation

- ⬆️ [Back to Top](#-ween-backend---architecture--flow-documentation)
- 📋 [Main README](README.md)
- 📖 [Technical Documentation](DOCS.md)

---

**Last Updated**: April 2026  
**Architecture Version**: 1.0  
**Diagrams**: Mermaid.js  
**Maintained By**: Ween Architecture Team
