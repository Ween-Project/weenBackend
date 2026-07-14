# API Reference

Base URL for local development:

```text
http://localhost:5050
```

All main REST endpoints use the `/api/v1` prefix. Most successful responses are wrapped in `ApiResponse<T>`.

## Authentication

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/v1/auth/register` | Public | Register a volunteer account with multipart request data |
| `POST` | `/api/v1/auth/register/organization` | Public | Register an organization account with multipart request data |
| `POST` | `/api/v1/auth/login` | Public | Log in as a volunteer or regular user |
| `POST` | `/api/v1/auth/login/organization` | Public | Log in as an organization |
| `POST` | `/api/v1/auth/refresh` | Public | Refresh access token using cookie or request body |
| `POST` | `/api/v1/auth/logout` | Authenticated | Log out and clear auth cookies |
| `GET` | `/api/v1/auth/verify-token` | Authenticated | Send verification email to current user |
| `POST` | `/api/v1/auth/verify-token` | Public | Verify email token |
| `POST` | `/api/v1/auth/forgot-password` | Public | Request password reset link |
| `POST` | `/api/v1/auth/reset-password` | Public | Reset password with token |
| `POST` | `/api/v1/auth/change-password` | Authenticated | Change current password |

## Users and Follows

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/v1/users/me` | Authenticated | Get current profile |
| `PUT` | `/api/v1/users/me` | Authenticated | Update current profile |
| `GET` | `/api/v1/users/@{username}` | Public | Get public profile |
| `GET` | `/api/v1/users/search` | Authenticated | Search users |
| `GET` | `/api/v1/users/me/events` | Authenticated | Current user's events |
| `GET` | `/api/v1/users/me/certificates` | Authenticated | Current user's certificates |
| `GET` | `/api/v1/users/{userId}/events` | Authenticated | User event history |
| `GET` | `/api/v1/users/{userId}/certificates` | Authenticated | User certificates |
| `GET` | `/api/v1/users/{userId}/badges` | Authenticated | User badges |
| `GET` | `/api/v1/users/me/badges` | Authenticated | Current user's badges |
| `GET` | `/api/v1/users/me/coins` | Authenticated | Current user's coins |
| `POST` | `/api/v1/users/{userId}/follow` | Authenticated | Follow a user |
| `DELETE` | `/api/v1/users/{userId}/follow` | Authenticated | Unfollow a user |
| `GET` | `/api/v1/users/{userId}/followers` | Authenticated | User followers |
| `GET` | `/api/v1/users/{userId}/following` | Authenticated | User following list |

## Organizations

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/v1/organizations/{id}` | Public | Get organization detail |
| `PUT` | `/api/v1/organizations/{id}` | `ORGANIZER`, `ORGANIZATION_ADMIN` | Update organization |
| `GET` | `/api/v1/organizations/current-organization-events` | Public by URL rule | Get current organization events |
| `POST` | `/api/v1/organizations/{orgId}/invitations` | `ORGANIZER`, `ORGANIZATION_ADMIN` | Invite organizer |
| `GET` | `/api/v1/invitations/approve` | Public token flow | Approve invitation |
| `GET` | `/api/v1/invitations/reject` | Public token flow | Reject invitation |
| `DELETE` | `/api/v1/organizations/{orgId}/organizers/{organizerId}` | `ORGANIZER`, `ORGANIZATION_ADMIN` | Remove organizer |

## Events and Registrations

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `GET` | `/api/v1/events` | Public | List events with filtering and pagination |
| `GET` | `/api/v1/events/{id}` | Public | Get event detail |
| `POST` | `/api/v1/events` | `ORGANIZER`, `ORGANIZATION_ADMIN` | Create event |
| `PUT` | `/api/v1/events/{id}` | `ORGANIZER`, `ORGANIZATION_ADMIN` | Update event |
| `DELETE` | `/api/v1/events/{id}` | `ORGANIZER`, `ORGANIZATION_ADMIN`, `ADMIN` | Delete event |
| `POST` | `/api/v1/events/{id}/publish` | `ADMIN`, `ORGANIZER`, `ORGANIZATION_ADMIN` | Publish event |
| `POST` | `/api/v1/events/{id}/start` | `ADMIN`, `ORGANIZER`, `ORGANIZATION_ADMIN` | Start event |
| `POST` | `/api/v1/events/{id}/complete` | `ADMIN`, `ORGANIZER`, `ORGANIZATION_ADMIN` | Complete event |
| `POST` | `/api/v1/events/{id}/cancel` | `ADMIN`, `ORGANIZER`, `ORGANIZATION_ADMIN` | Cancel event |
| `GET` | `/api/v1/events/{id}/stats` | `ORGANIZER`, `ORGANIZATION_ADMIN` | Event statistics |
| `POST` | `/api/v1/events/{id}/register` | `VOLUNTEER` | Register for event |
| `DELETE` | `/api/v1/events/{id}/register` | `VOLUNTEER` | Cancel registration |
| `GET` | `/api/v1/events/{id}/participants` | `ORGANIZER`, `ORGANIZATION_ADMIN` | List participants |

## Posts

| Method | Path | Access | Description |
| --- | --- | --- | --- |
| `POST` | `/api/v1/posts` | Authenticated | Create post |
| `GET` | `/api/v1/posts` | Authenticated | List feed posts |
| `GET` | `/api/v1/posts/following` | Authenticated | List followed users' posts |
| `GET` | `/api/v1/posts/{postId}` | Authenticated | Get post detail |
| `GET` | `/api/v1/posts/user/{userId}` | Authenticated | List user's posts |
| `GET` | `/api/v1/posts/organization/{organizationId}` | Authenticated | List organization's posts |
| `GET` | `/api/v1/posts/user/{userId}/reposts` | Authenticated | List user's reposts |
| `GET` | `/api/v1/posts/saved` | Authenticated | List saved posts |
| `GET` | `/api/v1/posts/liked` | Authenticated | List liked posts |
| `PUT` | `/api/v1/posts/{postId}` | Authenticated owner | Update post |
| `DELETE` | `/api/v1/posts/{postId}` | Authenticated owner or admin logic | Delete post |
| `POST` | `/api/v1/posts/{postId}/like` | Authenticated | Like post |
| `DELETE` | `/api/v1/posts/{postId}/like` | Authenticated | Unlike post |
| `POST` | `/api/v1/posts/{postId}/save` | Authenticated | Save post |
| `DELETE` | `/api/v1/posts/{postId}/save` | Authenticated | Unsave post |
| `POST` | `/api/v1/posts/{postId}/repost` | Authenticated | Repost |
| `DELETE` | `/api/v1/posts/{postId}/repost` | Authenticated | Remove repost |
| `POST` | `/api/v1/posts/{postId}/comments` | Authenticated | Add comment |
| `GET` | `/api/v1/posts/{postId}/comments` | Authenticated | List comments |
| `DELETE` | `/api/v1/posts/{postId}/comments/{commentId}` | Authenticated | Delete comment |

## Chat, Impact, AI, and Admin

Chat endpoints under `/api/v1/chat` provide conversations, requests, direct messages, group rooms, room messages, members, role changes, removals, and leave operations.

Impact endpoints include:

- `GET /api/v1/qr/generate`
- `POST /api/v1/participations/checkin-join`
- `GET /api/v1/certificates/my`
- `GET /api/v1/certificates/download/{id}`
- `DELETE /api/v1/certificates/{certificateId}`
- `GET /api/v1/coins/balance`
- `GET /api/v1/coins/transactions`
- `GET /api/v1/leaderboard`
- `GET /api/v1/notifications`
- `PUT /api/v1/notifications/{id}/read`
- `PUT /api/v1/notifications/read-all`

AI endpoints under `/api/v1/ai` support event suggestions, AI chat, history retrieval, and history deletion.

All `/api/v1/admin/**` endpoints require the `ADMIN` role. Admin operations include users, organizations, events, posts, comments, certificates, badges, coins, referrals, audit logs, and AI statistics.

## Response Shape

Successful responses commonly use:

```json
{
  "success": true,
  "data": {},
  "message": "Operation completed successfully",
  "timestamp": "2026-07-14T00:00:00"
}
```

Validation and runtime errors are normalized by `GlobalExceptionHandler` with fields such as status, error, message, path, traceId, and fieldErrors when validation fails.
