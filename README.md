# ASCEND API

Spring Boot backend for the ASCEND personal progression platform.

## What This Service Does

The backend provides the core application logic for authentication, player progression, mission handling, and status retrieval. It is built to support the game-inspired routine system used by the ASCEND frontend.

## Stack

- Java 20
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- PostgreSQL for local development and deploys
- Gradle Wrapper

## Requirements

- Java 20 or newer

## Local Database

By default, the API uses PostgreSQL. The easiest local setup is to run the provided Docker Compose stack, which starts both the database and the backend together.

Default settings:

```text
URL: jdbc:postgresql://db:5432/ascend
Driver: org.postgresql.Driver
User: ascend
Password: ascend
```

This setup already includes:

- a dedicated PostgreSQL container
- a persistent Docker volume for the database files

If you want to override the database, set:

```powershell
$env:ASCEND_DB_URL="jdbc:postgresql://localhost:5432/ascend"
$env:ASCEND_DB_DRIVER="org.postgresql.Driver"
$env:ASCEND_DB_USER="ascend"
$env:ASCEND_DB_PASSWORD="ascend"
$env:ASCEND_DB_DIALECT="org.hibernate.dialect.PostgreSQLDialect"
```

If you want to use a managed PostgreSQL instance in Render or Supabase, just point `ASCEND_DB_URL` and the credentials to that service.

Tables are created automatically by Hibernate on startup.

## Docker Compose

From the project root, start the stack with:

```powershell
docker compose up -d --build
```

This starts the backend and a PostgreSQL database on port `8080` and `5432`.

To watch the backend logs:

```powershell
docker compose logs -f backend
```

To stop everything:

```powershell
docker compose down
```

## Running the API

```bash
./gradlew bootRun
```

On Windows:

```powershell
..\start-backend.ps1
```

Default URL:

```text
http://localhost:8080
```

If port `8080` is unavailable, `start-backend.ps1` tries `8081` and then `8082`.

## Running Tests

```bash
./gradlew test
```

On Windows:

```powershell
.\gradlew.bat test
```

Tests use H2 in PostgreSQL compatibility mode, so they stay fully self-contained.

## Available Endpoints

- `POST /api/auth/register` creates a user and opens a session
- `POST /api/auth/login` authenticates a user
- `GET /api/auth/me` returns the authenticated user
- `POST /api/auth/logout` closes the current session
- `GET /api/health` returns API status information
- `GET /api/player/status` returns player progression and routine data

Protected routes require:

```text
Authorization: Bearer <token>
```

## CORS for Separate Frontend Deploys

If you deploy the frontend on GitHub Pages, Netlify, Vercel, or another public domain, set the allowed frontend origin(s) with:

```powershell
$env:ASCEND_CORS_ALLOWED_ORIGINS="https://your-frontend-domain.com"
```

You can provide more than one origin or pattern by separating them with commas.

Examples:

```powershell
$env:ASCEND_CORS_ALLOWED_ORIGINS="https://nate-allen21.github.io,https://*.vercel.app"
```

Localhost and private-network addresses are still allowed by default for development.

## Package Layout

- `config` infrastructure configuration such as CORS
- `controller` HTTP entry points
- `domain` JPA entities and enums
- `dto` request and response contracts
- `exception` centralized error handling
- `repository` persistence layer
- `service` application rules and orchestration
