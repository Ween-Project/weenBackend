# Testing Guide

The Ween backend includes service, controller, and security tests.

## Test Structure

```text
src/test/java/com/ween/
  controller/
  security/
  service/
src/test/resources/application-test.yml
```

Current test coverage areas include:

- Authentication
- Users
- Events
- Event registration and participation
- Organizations and invitations
- Posts
- Chat
- QR
- Certificates
- Coins
- Badges
- Notifications
- Leaderboard
- Referrals
- AI and Gemini integration logic
- Admin behavior
- JWT and AES utilities
- User details security service

## Commands

Run all tests:

```bash
./mvnw test
```

Run all service tests:

```bash
./mvnw test -Dtest=*ServiceTest
```

Run all controller tests:

```bash
./mvnw test -Dtest=*ControllerTest
```

Run one test class:

```bash
./mvnw test -Dtest=AuthServiceTest
```

On Windows PowerShell, use:

```powershell
.\mvnw.cmd test
.\mvnw.cmd test -Dtest=*ServiceTest
.\mvnw.cmd test -Dtest=*ControllerTest
```

## Test Configuration

Test configuration is stored in:

```text
src/test/resources/application-test.yml
```

The Maven build includes:

- `spring-boot-starter-test`
- `spring-security-test`
- `testcontainers`
- `testcontainers-mysql`
- JaCoCo Maven plugin

## Coverage

The Maven configuration includes JaCoCo with a package-level line coverage rule. Entities, DTOs, enums, and configs are excluded from the package rule because they are either structural classes or framework configuration classes.

Generate coverage report:

```bash
./mvnw clean test jacoco:report
```

Open the generated report:

```text
target/site/jacoco/index.html
```

## Recommended Manual Checks

Before final submission, verify these flows through Swagger UI or the frontend:

1. Volunteer registration, login, token refresh, and logout.
2. Organization registration and login.
3. Organizer creates, updates, publishes, starts, completes, and cancels an event.
4. Volunteer lists events, opens event detail, registers, and cancels registration.
5. QR generation and check-in.
6. Post create, update, delete, like, save, repost, and comment flows.
7. User profile update and public profile lookup.
8. Admin lists users, verifies organizations, manages badges, and reviews statistics.
