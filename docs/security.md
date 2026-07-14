# Security Model

Ween uses Spring Security with stateless JWT authentication, role-based authorization, password hashing, API key filtering, and global exception handling.

## Authentication

Main classes:

- `SecurityConfig`
- `JwtAuthenticationFilter`
- `JwtUtil`
- `UserDetailsServiceImpl`
- `AuthController`
- `AuthService`

Authentication endpoints:

- `POST /api/v1/auth/register`
- `POST /api/v1/auth/register/organization`
- `POST /api/v1/auth/login`
- `POST /api/v1/auth/login/organization`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/auth/logout`

The API supports JWT Bearer authentication and also sets HTTP-only cookies named `accessToken` and `refreshToken` during login and registration.

## Roles

| Role | Purpose |
| --- | --- |
| `VOLUNTEER` | Volunteer user who can register for events, check in, post, chat, follow, and track impact |
| `ORGANIZER` | Organization-side user who can create and manage events and participants |
| `ORGANIZATION_ADMIN` | Higher organization role with organization and organizer management rights |
| `ADMIN` | Platform administrator with moderation and management permissions |

## Authorization Rules

Examples from `SecurityConfig`:

- Public routes include registration, login, event reads, Swagger, OpenAPI, WebSocket entry, and health checks.
- Volunteer routes include event registration and registration cancellation.
- Organizer routes include event creation, event updates, participant lists, event stats, organization updates, and invitations.
- Admin routes include all `/api/v1/admin/**` endpoints.

Some endpoints also use method-level checks through `@PreAuthorize`.

## Password Security

Passwords are hashed using BCrypt with strength 12:

```java
new BCryptPasswordEncoder(12)
```

Plain-text passwords are never stored in the database.

## CORS

CORS origins are configurable through:

```text
ween.cors.allowed-origins
```

The default development origins include common local frontend ports such as `3000`, `3001`, `5000`, `5001`, `5173`, and `8080`.

## QR and API Key Security

QR and check-in related functionality uses `QrService`, `QrController`, `ParticipationController`, `ApiKeyFilter`, and `AesUtil`.

The QR token validity defaults to a short lifetime through:

```text
ween.qr.token-validity-seconds
```

The organizer/check-in API key is configured through:

```text
ORGANIZER_API_KEY
```

## Error Handling

`GlobalExceptionHandler` converts common errors into consistent HTTP responses:

| Exception Type | HTTP Status |
| --- | --- |
| Validation errors | `400 Bad Request` |
| Invalid token | `400 Bad Request` |
| Unauthorized | `401 Unauthorized` |
| Access denied | `403 Forbidden` |
| Resource not found | `404 Not Found` |
| Data conflict or already exists | `409 Conflict` |
| QR token expired | `410 Gone` |
| QR token invalid | `422 Unprocessable Entity` |
| Event capacity exceeded | `422 Unprocessable Entity` |
| Service unavailable | `503 Service Unavailable` |
| Unexpected error | `500 Internal Server Error` |

## Security Notes for Frontend Integration

- Send authenticated requests with credentials enabled if using HTTP-only cookies.
- For Bearer token integration, send `Authorization: Bearer <token>`.
- Store tokens carefully on the frontend. HTTP-only cookies are preferred for browser-based clients.
- Handle `401` by refreshing or redirecting to login.
- Handle `403` by showing an authorization error instead of retrying.
