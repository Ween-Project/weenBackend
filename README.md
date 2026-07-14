# Ween Backend

Ween is a Youth Volunteering and Social Impact Platform built for connecting young volunteers with organizations, community events, certificates, social activity, and measurable impact. This repository contains the Spring Boot backend API for authentication, event discovery, event registration, organization management, posts, chat, QR check-in, certificates, coins, badges, notifications, leaderboards, AI helpers, and administration.

The project is developed by Team Enthuzone.

## Documentation Index

The detailed project documentation is stored in the `docs` folder:

- [Architecture](docs/architecture.md)
- [Entity Relationship Diagram](docs/erd.md)
- [User Flow](docs/user-flow.md)
- [API Reference](docs/api-reference.md)
- [Setup Guide](docs/setup.md)
- [Security Model](docs/security.md)
- [Testing Guide](docs/testing.md)
- [Grading Requirements Alignment](docs/grading-alignment.md)

## Project Summary

Ween solves the problem of fragmented volunteer discovery and impact tracking. Volunteers need a simple way to find relevant opportunities, register for events, prove participation, receive certificates, and build a visible social impact profile. Organizations need a controlled way to publish events, manage participants, communicate with users, verify attendance, and review platform activity.

The backend exposes a REST API under `/api/v1`, uses JWT authentication, applies role-based authorization, stores data through JPA/Hibernate, and uses Flyway migrations for database schema management.

## Core Features

- User registration, organization registration, login, refresh, logout, email verification, password reset, and password change.
- JWT-based stateless authentication with HTTP-only token cookies and Bearer token support.
- Role-based access control for `VOLUNTEER`, `ORGANIZER`, `ORGANIZATION_ADMIN`, and `ADMIN`.
- Event CRUD with categories, status transitions, filters, public reads, organizer writes, and participant statistics.
- Event registration and cancellation for volunteers.
- QR generation and check-in flow for participation verification.
- Organization profile management, organizer invitations, approval, rejection, and organizer removal.
- Social posts with media, likes, saves, reposts, and comments.
- User profile, public profile, followers, following, badges, certificates, events, and coin data.
- Direct and room-based chat with REST endpoints and WebSocket support.
- Certificates for completed participation.
- Ween Coins, transaction history, leaderboards, badges, and referrals.
- Notifications for user-facing platform activity.
- Admin moderation for users, organizations, events, posts, comments, certificates, coins, badges, referrals, audit logs, and AI statistics.
- AI endpoints for event content assistance and chat history.
- Global exception handling with consistent error responses.
- OpenAPI/Swagger documentation.
- Docker and Docker Compose support.

## Technology Stack

| Area | Technology |
| --- | --- |
| Language | Java 17 |
| Framework | Spring Boot 3.2.3 |
| Web API | Spring Web MVC |
| Security | Spring Security, JWT, BCrypt |
| Database | MySQL 8.0, PostgreSQL runtime driver available |
| ORM | Spring Data JPA, Hibernate |
| Migrations | Flyway |
| Validation | Jakarta Bean Validation |
| Mapping | MapStruct |
| File Uploads | Multipart API, Cloudinary SDK |
| Email | Spring Boot Mail |
| PDF Rendering | Thymeleaf, Flying Saucer OpenPDF, iText dependency |
| Realtime | Spring WebSocket, STOMP configuration |
| API Docs | SpringDoc OpenAPI, Swagger UI |
| Testing | JUnit 5, Mockito, Spring Security Test, Testcontainers |
| Build | Maven |
| Deployment | Docker, Docker Compose |

## Repository Structure

```text
weenBackend/
  .github/
  src/
    main/
      java/com/ween/
        config/
        controller/
        dto/
        entity/
        enums/
        exception/
        mapper/
        repository/
        security/
        seeder/
        service/
        WeenApplication.java
      resources/
        db.migration/
        templates/
    test/
      java/com/ween/
        controller/
        security/
        service/
      resources/
        application-test.yml
  docker-compose.yml
  Dockerfile
  pom.xml
  README.md
  docs/
```

## Main API Areas

| Area | Base Path | Purpose |
| --- | --- | --- |
| Authentication | `/api/v1/auth` | Register, login, refresh, logout, verification, password flows |
| Users | `/api/v1/users` | Current profile, public profiles, search, user events, badges, certificates |
| Follows | `/api/v1/users/{userId}` | Follow, unfollow, followers, following |
| Organizations | `/api/v1/organizations` | Organization details, update, current organization events |
| Invitations | `/api/v1/organizations/{orgId}/invitations` | Invite and manage organizers |
| Events | `/api/v1/events` | Event list, detail, create, update, delete, publish, start, complete, cancel |
| Registrations | `/api/v1/events/{id}/register` | Volunteer event registration and cancellation |
| Participations | `/api/v1/participations` | QR-based check-in join flow |
| QR | `/api/v1/qr` | Generate QR token |
| Posts | `/api/v1/posts` | Feed, CRUD, likes, saves, reposts, comments |
| Chat | `/api/v1/chat` | Conversations, requests, direct messages, rooms, group messages |
| Certificates | `/api/v1/certificates` | My certificates, download, delete |
| Coins | `/api/v1/coins` | Balance and transaction history |
| Leaderboard | `/api/v1/leaderboard` | Ranked volunteer impact data |
| Notifications | `/api/v1/notifications` | Notification list and read states |
| AI | `/api/v1/ai` | Event suggestions, chat, history |
| Admin | `/api/v1/admin` | Platform moderation and statistics |

See [API Reference](docs/api-reference.md) for a more complete endpoint map.

## Requirement Highlights

This backend directly supports the major backend grading requirements:

- REST API with proper HTTP method usage across 18 controllers.
- DTO-based request and response handling.
- Full CRUD support for events and posts, plus administrative CRUD-style management for users, organizations, badges, events, posts, comments, and certificates.
- Input validation through annotations such as `@NotBlank`, `@NotNull`, `@Size`, `@Email`, and `@Min`.
- Global exception handling through `GlobalExceptionHandler`.
- MySQL database support with JPA/Hibernate and Flyway migrations.
- JWT registration, login, refresh, logout, protected endpoints, and role-based authorization.
- Four roles: `VOLUNTEER`, `ORGANIZER`, `ORGANIZATION_ADMIN`, and `ADMIN`.
- Swagger/OpenAPI API documentation.
- Unit and controller test coverage across service, controller, and security layers.

See [Grading Requirements Alignment](docs/grading-alignment.md) for a grader-friendly checklist.

## Quick Start

### Prerequisites

- Java 17 or newer
- Maven 3.9 or newer, or the included Maven wrapper
- Docker and Docker Compose
- MySQL 8.0 if running without Docker

### Environment Variables

Copy `.env.example` to `.env` and fill in real values.

```env
DB_USERNAME=your-db-username
DB_PASSWORD=your-db-password
JWT_SECRET=your-secret-key-min-32-characters-required
AES_SECRET_KEY=16-character-key
ORGANIZER_API_KEY=your-api-key-here
GEMINI_API_KEY=your-gemini-api-key-here
MAIL_USERNAME=your-email@example.com
MAIL_PASSWORD=your-smtp-app-password
```

### Run With Docker Compose

```bash
docker compose up --build
```

Default service URLs:

- Backend API: `http://localhost:5050`
- Swagger UI: `http://localhost:5050/swagger-ui.html`
- OpenAPI JSON: `http://localhost:5050/v3/api-docs`
- MySQL: `localhost:3306`

### Run Locally With Maven

```bash
./mvnw clean test
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd clean test
.\mvnw.cmd spring-boot:run
```

## Authentication Flow

1. A volunteer registers with `POST /api/v1/auth/register`, or an organization registers with `POST /api/v1/auth/register/organization`.
2. The user logs in with `POST /api/v1/auth/login` or `POST /api/v1/auth/login/organization`.
3. The API returns authentication data and sets HTTP-only `accessToken` and `refreshToken` cookies.
4. Protected endpoints are called with an authenticated request.
5. The access token can be refreshed with `POST /api/v1/auth/refresh`.
6. Logout is handled through `POST /api/v1/auth/logout`.

## Development Workflow

The requested branch for README work is `update/readme`. Do not develop directly on `main`. For project tasks, use a task branch such as:

```text
feature/user-authentication
feature/event-crud
feature/post-feed
fix/login-validation
update/readme
```

Recommended commit discipline:

- Keep commits focused.
- Use descriptive messages such as `Document backend architecture` or `Add API reference documentation`.
- Avoid large unrelated changes in the same commit.
- Do not mix documentation updates with backend behavior changes unless they belong to the same task.

## Testing

Run the complete test suite:

```bash
./mvnw test
```

Run service tests only:

```bash
./mvnw test -Dtest=*ServiceTest
```

Run controller tests only:

```bash
./mvnw test -Dtest=*ControllerTest
```

The repository currently contains tests for services, controllers, and security helpers, including authentication, events, posts, organizations, participation, QR, certificates, coins, chat, notifications, admin behavior, and utility classes.

## Database

Flyway migrations are located in `src/main/resources/db.migration`.

Important tables include:

- `users`
- `organizations`
- `events`
- `event_registrations`
- `participations`
- `qr_tokens`
- `certificates`
- `coin_transactions`
- `leaderboard_entries`
- `notifications`
- `referrals`
- `chat_rooms`
- `chat_messages`

See [Entity Relationship Diagram](docs/erd.md) for the data model.

## OpenAPI

When the application is running, API documentation is available at:

```text
http://localhost:5050/swagger-ui.html
http://localhost:5050/v3/api-docs
```

## Team

Team Name: Enthuzone

Team Members:

- Kenan Gafarov
- Mansura Badalova
- Umut Alizade
- Lala Aliyeva

## License

This project is prepared for the Ween final project submission. Usage and distribution should follow the team and course rules.
