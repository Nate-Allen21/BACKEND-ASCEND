# ASCEND API

Spring Boot backend for the ASCEND personal progression platform.

## What This Service Does

The backend provides the core application logic for authentication, player progression, mission handling, and status retrieval. It is built to support the game-inspired routine system used by the ASCEND frontend.

## Stack

- Java 20
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- H2 for local development
- Gradle Wrapper

## Requirements

- Java 20 or newer

## Local Database

By default, the API uses an H2 file database path that works well in containers and in local development.

Default settings:

```text
Path base: /tmp/ascend-local
Driver: org.h2.Driver
User: sa
Password: empty
```

This local setup already includes:

- `AUTO_SERVER=FALSE` to avoid container-specific file locking issues
- `LOCK_TIMEOUT=10000` to tolerate short access disputes

If you want to override the database, set:

```powershell
$env:ASCEND_DB_URL="jdbc:h2:file:./data/ascend-local;MODE=MySQL;AUTO_SERVER=TRUE;LOCK_TIMEOUT=10000"
$env:ASCEND_DB_DRIVER="org.h2.Driver"
$env:ASCEND_DB_USER="sa"
$env:ASCEND_DB_PASSWORD=""
$env:ASCEND_DB_DIALECT="org.hibernate.dialect.H2Dialect"
```

If you want persistent local storage inside Docker, you can point `ASCEND_DB_URL` to a writable folder of your choice and mount that folder into the container.

Tables are created automatically by Hibernate on startup.

## Docker Compose

From the project root, start the stack with:

```powershell
docker compose up -d --build
```

This starts the backend on port `8080` with the embedded H2 database.

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

Tests use H2 in memory, so they stay fully self-contained.

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
