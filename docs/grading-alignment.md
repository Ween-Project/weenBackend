# Grading Requirements Alignment

This document maps the Ween backend to the final project grading requirements. It focuses only on documentation and backend evidence available in this repository.

## Project Planning and Documentation

| Requirement | Evidence |
| --- | --- |
| Project title | Ween - Youth Volunteering and Social Impact Platform |
| Project description | README and `docs/architecture.md` describe the platform purpose and backend scope |
| Problem statement | README explains fragmented volunteering discovery and impact tracking |
| Target users | Volunteers, organizations, organization admins, platform admins |
| Main features | README Core Features section |
| Technical architecture overview | `docs/architecture.md` |
| User flow | `docs/user-flow.md` |
| ERD | `docs/erd.md` |
| Setup and maintenance documentation | `docs/setup.md`, `docs/testing.md`, `docs/security.md` |

## Spring Boot Backend Requirements

| Requirement | Evidence in Repository |
| --- | --- |
| REST API following best practices | Controllers under `src/main/java/com/ween/controller` expose resource-oriented endpoints |
| Proper HTTP methods | Uses `GET`, `POST`, `PUT`, and `DELETE` mappings across controllers |
| Proper HTTP status codes | Controllers return `201 Created`, `200 OK`; exception handler returns `400`, `401`, `403`, `404`, `409`, `410`, `422`, `500`, `503` |
| DTOs for request and response handling | Request DTOs in `dto/request`, response DTOs in `dto/response` |
| Full CRUD on at least two resources | Events and posts provide create, read, update, delete flows |
| Input validation annotations | DTOs use `@NotBlank`, `@NotNull`, `@Size`, `@Email`, `@Min` |
| Global exception handling | `GlobalExceptionHandler` |
| MySQL or PostgreSQL database | Docker Compose uses MySQL 8.0; PostgreSQL driver is also available at runtime |
| JPA/Hibernate integration | Entities and repositories under `entity` and `repository`; Spring Data JPA dependency |
| Database migrations | Flyway migrations in `src/main/resources/db.migration` |

## JWT Authentication and Authorization

| Requirement | Evidence |
| --- | --- |
| User registration endpoint | `POST /api/v1/auth/register` |
| Organization registration endpoint | `POST /api/v1/auth/register/organization` |
| Login endpoint | `POST /api/v1/auth/login`, `POST /api/v1/auth/login/organization` |
| JWT-based authentication | `JwtAuthenticationFilter`, `JwtUtil`, `SecurityConfig` |
| Role-based authorization | `UserRole` enum and `SecurityConfig` role rules |
| At least two roles | Four roles are implemented: `VOLUNTEER`, `ORGANIZER`, `ORGANIZATION_ADMIN`, `ADMIN` |
| Protected endpoints | Events write endpoints, registrations, coins, notifications, chat, admin, profile endpoints |

## CRUD Resources

### Events

| Operation | Endpoint |
| --- | --- |
| Create | `POST /api/v1/events` |
| Read list | `GET /api/v1/events` |
| Read detail | `GET /api/v1/events/{id}` |
| Update | `PUT /api/v1/events/{id}` |
| Delete | `DELETE /api/v1/events/{id}` |

### Posts

| Operation | Endpoint |
| --- | --- |
| Create | `POST /api/v1/posts` |
| Read list | `GET /api/v1/posts` |
| Read detail | `GET /api/v1/posts/{postId}` |
| Update | `PUT /api/v1/posts/{postId}` |
| Delete | `DELETE /api/v1/posts/{postId}` |

Additional CRUD-style management exists for badges, admin event management, users, organizations, comments, and certificates.

## Full-Stack Integration Readiness

Although this repository is the backend, it provides the integration surface required by a Next.js frontend:

| Frontend Requirement | Backend Support |
| --- | --- |
| Frontend consumes REST APIs | REST endpoints under `/api/v1` |
| Functional login flow | Auth endpoints return tokens and set auth cookies |
| Functional logout flow | `POST /api/v1/auth/logout` clears auth cookies |
| Protected frontend routes | Backend returns `401` or `403` for invalid access |
| Token storage and usage | HTTP-only cookies and Bearer tokens are supported |
| Loading and error states | Backend returns consistent success and error response shapes |
| CRUD UI for at least two resources | Event and post CRUD endpoints are available |
| Dashboard/detail pages | Admin stats, event detail, profile, organization, leaderboard, and feed endpoints support frontend views |

## Git Workflow Reminder

For final submission, keep these workflow rules visible to the team:

- Work on a task branch, not directly on `main`.
- Keep each task branch focused.
- Use at least five meaningful commits per task branch if required by the course rules.
- Use descriptive commit messages.
- Keep commits small and reviewable.
- Do not mix unrelated feature work into documentation branches.

## Submission Checklist

| Item | Status |
| --- | --- |
| README written in English | Complete |
| No emoji in README or docs | Complete |
| Documentation folder exists | Complete |
| Architecture document exists | Complete |
| ERD document exists | Complete |
| User flow document exists | Complete |
| API reference exists | Complete |
| Setup instructions exist | Complete |
| Security documentation exists | Complete |
| Testing documentation exists | Complete |
| Requirements alignment document exists | Complete |
