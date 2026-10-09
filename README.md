# ASCEND API

Spring Boot backend for the ASCEND personal progression platform.

## What This Service Does

The backend provides the core application logic for authentication, player progression, mission handling, and status retrieval. It is built to support the game-inspired routine system used by the ASCEND frontend.

## Stack

- Java 21
- Spring Boot 4
- Spring Web MVC
- Spring Data JPA
- PostgreSQL for local development and deploys
- Gradle Wrapper

## Requirements

- Java 21 or newer

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

## Supabase Database

Create a Supabase project and copy the PostgreSQL connection details from **Project Settings > Database**.

For Render, prefer the pooled connection string and set:

```text
ASCEND_DB_URL=jdbc:postgresql://YOUR-SUPABASE-POOLER-HOST:6543/postgres?sslmode=require
ASCEND_DB_USER=postgres.YOUR-PROJECT-REF
ASCEND_DB_PASSWORD=YOUR-SUPABASE-DATABASE-PASSWORD
```

Keep the `jdbc:postgresql://` prefix because this is a Spring Boot JDBC URL. Supabase provides PostgreSQL; the AI provider is configured separately with `OPENAI_API_KEY`.

If you use the direct Supabase database host instead of the pooler, keep `?sslmode=require` and use the port shown by Supabase.

## Render Deploy

This repo includes `render.yaml` for a Docker web service.

In Render:

1. Create a new Blueprint or Web Service from this repository.
2. Use the backend folder as the service root if Render asks for one.
3. Add these environment variables:

```text
ASCEND_DB_URL=jdbc:postgresql://YOUR-SUPABASE-POOLER-HOST:6543/postgres?sslmode=require
ASCEND_DB_USER=postgres.YOUR-PROJECT-REF
ASCEND_DB_PASSWORD=YOUR-SUPABASE-DATABASE-PASSWORD
ASCEND_CORS_ALLOWED_ORIGINS=https://YOUR-VERCEL-PROJECT.vercel.app,https://*.vercel.app
ASCEND_AI_PROVIDER=groq
ASCEND_AI_BASE_URL=https://api.groq.com/openai/v1
ASCEND_AI_API_KEY=YOUR-GROQ-API-KEY
GROQ_API_KEY=YOUR-GROQ-API-KEY
ASCEND_AI_MODEL=openai/gpt-oss-20b
ASCEND_AI_RESPONSE_MODE=chat-completions
```

Render provides `PORT` automatically, and the Dockerfile already starts the Spring Boot app with that port.

After deploy, check:

```text
https://YOUR-RENDER-SERVICE.onrender.com/api/health
```

It should return `status: UP`.

## Groq AI (plano gratuito)

The backend calls AI providers through an OpenAI-compatible chat completions request, so Groq works with the built-in `groq` provider.

1. Create a free key at https://console.groq.com/keys .
2. Configure the service (Render, Docker or a local `.env` copied from `.env.example`):

```text
ASCEND_AI_PROVIDER=groq
ASCEND_AI_BASE_URL=https://api.groq.com/openai/v1
ASCEND_AI_API_KEY=YOUR-GROQ-API-KEY
ASCEND_AI_MODEL=openai/gpt-oss-20b
ASCEND_AI_RESPONSE_MODE=chat-completions
```

With `ASCEND_AI_PROVIDER=groq` you can omit `ASCEND_AI_BASE_URL` and just export `GROQ_API_KEY`; the service falls back to `https://api.groq.com/openai/v1` and reads the key from that variable.

The default model is `openai/gpt-oss-20b`, verified with the configured Groq account. Availability and free quotas depend on the account.

If the model id is rejected, list the ids actually enabled for your key:

```bash
curl -H "Authorization: Bearer $GROQ_API_KEY" https://api.groq.com/openai/v1/models
```

The key stays in the backend only. Never put it in `VITE_*` or `EXPO_PUBLIC_*` variables. If no key is configured, the app keeps working in local fallback mode.

### Other OpenAI-compatible providers

Any provider exposing an OpenAI-compatible `/chat/completions` endpoint works by setting `ASCEND_AI_PROVIDER=openai-compatible` plus `ASCEND_AI_BASE_URL`, `ASCEND_AI_API_KEY` and `ASCEND_AI_MODEL`. `azure-openai` and `llama` providers are also supported.

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
