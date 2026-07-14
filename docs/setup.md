# Setup Guide

This guide explains how to run the Ween backend locally.

## Prerequisites

- Java 17 or newer
- Maven 3.9 or newer, or the included Maven wrapper
- Docker and Docker Compose
- MySQL 8.0 if running outside Docker

## Environment

Create a `.env` file from `.env.example`.

```bash
cp .env.example .env
```

On Windows PowerShell:

```powershell
Copy-Item .env.example .env
```

Required values:

| Variable | Purpose |
| --- | --- |
| `DB_USERNAME` | Database user |
| `DB_PASSWORD` | Database password |
| `JWT_SECRET` | Secret for signing JWT tokens; use at least 32 characters |
| `AES_SECRET_KEY` | AES key used by QR/security helpers |
| `ORGANIZER_API_KEY` | API key for protected organizer/check-in flows |
| `GEMINI_API_KEY` | Gemini API key for AI endpoints |
| `MAIL_USERNAME` | SMTP username |
| `MAIL_PASSWORD` | SMTP password or app password |

## Docker Compose Run

```bash
docker compose up --build
```

Services:

| Service | Port | Description |
| --- | --- | --- |
| `backend` | `5050` | Spring Boot API |
| `mysql` | `3306` | MySQL database |

Useful URLs:

```text
http://localhost:5050/swagger-ui.html
http://localhost:5050/v3/api-docs
```

Stop containers:

```bash
docker compose down
```

Remove database volume when a clean database is needed:

```bash
docker compose down -v
```

## Local Maven Run

Run tests:

```bash
./mvnw test
```

Start application:

```bash
./mvnw spring-boot:run
```

On Windows PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

## Build Jar

```bash
./mvnw clean package
```

The jar is created under `target/`.

## Database Migrations

Flyway migration files are located in:

```text
src/main/resources/db.migration
```

Current migration files:

- `V1__CREATE_TABLES.sql`
- `V2__ADD_CONSTRAINTS.sql`
- `V3__SEED_DATA.sql`
- `V4__ADD_PARTICIPATIONS.sql`
- `V5__ADD_CHAT_MESSAGES.sql`

## Troubleshooting

### Port 5050 is already used

Change the backend port mapping in `docker-compose.yml`.

```yaml
ports:
  - "5051:5050"
```

### MySQL is not ready

Docker Compose uses a health check for MySQL. If the backend starts too early after manual changes, restart it:

```bash
docker compose restart backend
```

### Authentication fails after changing secrets

Tokens signed with an old `JWT_SECRET` become invalid after changing the secret. Log in again to receive new tokens.
