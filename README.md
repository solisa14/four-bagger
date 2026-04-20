![CI](https://github.com/solisa14/four-bagger-api/actions/workflows/ci.yml/badge.svg)
# Four Bagger

Four Bagger is a Spring Boot backend for organizing cornhole games and single-elimination tournaments. It supports
user authentication, standalone singles and doubles games, tournament registration via join codes, bracket generation,
round-level rule configuration, and automatic match progression as games are completed.

I built this project because I wanted something my family could actually use during cornhole tournaments at family
functions, and I wanted a backend project that pushed me beyond basic CRUD. The goal was to practice backend engineering
in a project with real domain rules, real state transitions, and a real deployment.

The API is deployed and live at **https://four-bagger-api.azurewebsites.net**.

## Highlights

- JWT-based authentication with refresh-token rotation and HttpOnly cookies
- Standalone singles and doubles game support
- Tournament lifecycle support from registration to bracket generation to live match progression
- Configurable round rules with `bestOf` series support and multiple scoring modes
- Event-driven tournament advancement when a game completes
- Flyway-managed schema changes with Hibernate validation and PostgreSQL persistence
- Containerized deployment on Azure App Service with a serverless Neon PostgreSQL backend

## Tech Stack

- Java 25
- Spring Boot 4.0.1
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL (Neon in production)
- Testcontainers
- JUnit 5 and Mockito
- Docker (multi-stage `linux/amd64` build)
- Azure App Service for Containers + Azure Container Registry

## Architecture

The project uses a layered, package-by-feature structure:

- `auth` handles registration, login, logout, refresh-token rotation, and persisted refresh tokens
- `user` handles profile reads and account updates
- `game` handles standalone game creation, frame recording, scoring, and game state transitions
- `tournament` handles tournament lifecycle, bracket generation, round configuration, and match progression
- `security` contains JWT parsing, authentication filters, and Spring Security configuration
- `common` contains shared exception handling and validation utilities

Some of the main engineering decisions in the codebase:

- Scoring rules use a strategy-style design through `GameScoringPolicy`, allowing different scoring modes to be selected
  from `Game.scoringMode`
- Controllers use mapper classes to translate request DTOs into domain commands or responses (for example `GameMapper`,
  `TournamentMapper`), which keeps service boundaries explicit
- Tournament progression is event-driven: when a tournament-linked game reaches `COMPLETED`, `GameService` publishes
  `GameCompletedEvent`; `TournamentMatchService` listens and runs match/tournament progression (advancing winners,
  best-of series, etc.)
- Database changes are managed through Flyway migrations, while Hibernate runs in `validate` mode to prevent the schema
  from drifting away from the entity model

## Testing Approach

The project uses a layered test strategy instead of relying on only one kind of test:

- Unit tests for service-level business logic
- `@WebMvcTest` tests for controller behavior, validation, and HTTP responses
- `@DataJpaTest` tests for repository behavior against PostgreSQL via Testcontainers
- `@SpringBootTest` integration tests for full application flows

Integration tests under `src/test/java/.../auth`, `game`, and `tournament` exercise the same HTTP flows as
the [API examples](#api-snapshot) (for example `AuthFlowIntegrationTest`, `GameFlowIntegrationTest`,
`TournamentLifecycleIntegrationTest`, and `TournamentGameProgressionIntegrationTest`).

## Local Development

The backend can be run locally against a PostgreSQL database of your choice, or against the deployed instance.

Run the application locally (defaults to port `8080` unless overridden in configuration), then use the examples below
against `http://localhost:8080`. Swap in `https://four-bagger-api.azurewebsites.net` to run them against the deployed
instance instead.

Running the full integration test suite requires **Docker** (Testcontainers starts PostgreSQL for tests). See
`./mvnw clean test` in CI or locally with Docker running.

## Deployment

The production instance runs as a Docker container on Azure App Service for Containers (Linux), backed by a Neon
serverless PostgreSQL database.

Deployment topology:

- **Image build**: multi-stage `Dockerfile` — Maven + Temurin 25 JDK build stage, slim `eclipse-temurin:25-jre` runtime
  stage, non-root user, targeted at `linux/amd64`
- **Registry**: Azure Container Registry (`fourbaggeracr.azurecr.io/four-bagger-api:latest`)
- **Runtime**: Azure App Service (Linux Containers) in the `four-bagger-rg` resource group
- **Database**: Neon PostgreSQL (`us-east-1` region, free tier), accessed via standard JDBC connection string
- **Migrations**: Flyway runs from `main()` via a `SpringApplicationBuilder` initializer before any Spring beans are
  created, so Hibernate schema validation always sees the migrated schema
- **Secrets**: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `JWT_SECRET`, and `ALLOWED_ORIGINS` are supplied as App Service
  application settings (environment variables) and consumed by `application-prod.yml`
- **Profile**: the container sets `SPRING_PROFILES_ACTIVE=prod` so `application-prod.yml` takes over

The manual deploy cycle (until CI/CD is wired up) is:

```bash
docker build --no-cache --platform linux/amd64 \
  -t fourbaggeracr.azurecr.io/four-bagger-api:latest .

az acr login --name fourbaggeracr
docker push fourbaggeracr.azurecr.io/four-bagger-api:latest

az webapp restart --resource-group four-bagger-rg --name four-bagger-api
```

## API Snapshot

All endpoints are served under `/api/v1`. Authenticated routes expect a JWT in the `accessToken` HttpOnly cookie (set by
`POST /auth/register` or `POST /auth/login`).

### Representative Endpoints

| Area               | Endpoints                                                                                                                                                                                                                                                                     |
|--------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Auth               | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh-token`, `POST /auth/logout`                                                                                                                                                                                    |
| User               | `GET /user/me`, `PATCH /user/me`, `PUT /user/me/password`                                                                                                                                                                                                                     |
| Games              | `POST /games`, `POST /games/{gameId}/start`, `POST /games/{gameId}/frames`, `GET /games/{gameId}`, `GET /games/me`, `POST /games/{gameId}/cancel`                                                                                                                             |
| Tournaments        | `POST /tournaments`, `GET /tournaments/{id}`, `POST /tournaments/join`, `POST /tournaments/{id}/bracket`, `PATCH /tournaments/{id}/rounds/{roundNumber}`, `POST /tournaments/{id}/start`, `DELETE /tournaments/{id}/participants/{participantId}`, `DELETE /tournaments/{id}` |
| Tournament Matches | `POST /tournaments/{tournamentId}/matches/{matchId}/start`, `GET /tournaments/{tournamentId}/matches/{matchId}`                                                                                                                                                               |

### Validation and rules (quick reference)

- **Register**: username must be 5–30 alphanumeric characters or underscores; password must meet the strength rules
  enforced by the API (e.g. mixed case, digit, special character).
- **Create tournament**: `title` is required; `gameType` may be `SINGLES` or `DOUBLES` (omit for default singles).
- **Bracket generation**: singles requires **more than two** registered participants; doubles requires an **even** count
  of **at least six** participants.
- **Game mutations** (`start`, `frames`, `cancel`): the caller must be a **participant** on the game or the user who *
  *created** the game (tournament games are typically created with the organizer as `createdBy`, so the organizer can
  run the demo without being a player).

### Register and use cookies

Auth responses set `accessToken` and `refreshToken` as HttpOnly cookies. Use `-c` / `-b` with `curl` so authenticated
calls work:

```bash
curl -i -c cookies.txt \
  -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "username": "player_one",
    "email": "player1@example.com",
    "password": "StrongPass1!",
    "firstName": "Player",
    "lastName": "One"
  }'
```

### Standalone game (two players)

Player two must exist so you can pass their user id as `playerTwoId`. Register two players, then obtain player two’s id
from `GET /user/me` while logged in as player two.

```bash
# Player 1 — register and keep cookies in p1.txt
curl -i -c p1.txt -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_p1","email":"demo_p1@example.com","password":"StrongPass1!","firstName":"P","lastName":"One"}'

# Player 2 — register and keep cookies in p2.txt
curl -i -c p2.txt -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_p2","email":"demo_p2@example.com","password":"StrongPass1!","firstName":"P","lastName":"Two"}'

# Player 2 — get their user id (copy from JSON "id" field)
curl -s -b p2.txt http://localhost:8080/api/v1/user/me
```

Replace `PLAYER_TWO_UUID` below with that `id`, then as **player 1** create a game, start it, and record a frame:

```bash
curl -i -b p1.txt -X POST http://localhost:8080/api/v1/games \
  -H "Content-Type: application/json" \
  -d "{\"playerTwoId\":\"PLAYER_TWO_UUID\"}"

# Use the created game id from the response as GAME_ID
curl -i -b p1.txt -X POST http://localhost:8080/api/v1/games/GAME_ID/start

curl -i -b p1.txt -X POST http://localhost:8080/api/v1/games/GAME_ID/frames \
  -H "Content-Type: application/json" \
  -d '{"p1BagsIn":1,"p1BagsOn":0,"p2BagsIn":0,"p2BagsOn":0}'
```

### Tournament flow (singles, bracket, match, game)

Singles brackets need **at least three** participants besides the organizer. Register an organizer and three players (
four cookie jars), create a tournament, join with the join code from the create response, generate the bracket, then
read the tournament JSON to get a `matchId` (see `rounds[].matches[].id`; non-bye matches have `"isBye": false`).

```bash
# Organizer
curl -i -c org.txt -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_org","email":"demo_org@example.com","password":"StrongPass1!","firstName":"O","lastName":"Org"}'

# Three players (repeat pattern with unique usernames / emails)
curl -i -c p1.txt -X POST http://localhost:8080/api/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"demo_t1","email":"demo_t1@example.com","password":"StrongPass1!","firstName":"T","lastName":"One"}'
# ... p2.txt, p3.txt for demo_t2 / demo_t3 similarly
```

Create tournament (organizer cookie):

```bash
curl -i -b org.txt -X POST http://localhost:8080/api/v1/tournaments \
  -H "Content-Type: application/json" \
  -d '{"title":"Family Cornhole Classic"}'
```

Save `id` as `TOURNAMENT_ID` and `joinCode` from the response. Each player joins:

```bash
curl -i -b p1.txt -X POST http://localhost:8080/api/v1/tournaments/join \
  -H "Content-Type: application/json" \
  -d '{"joinCode":"JOIN_CODE_FROM_CREATE"}'
```

Generate bracket (organizer only):

```bash
curl -i -b org.txt -X POST http://localhost:8080/api/v1/tournaments/TOURNAMENT_ID/bracket
```

Move the tournament to live play (required before starting a match):

```bash
curl -i -b org.txt -X POST http://localhost:8080/api/v1/tournaments/TOURNAMENT_ID/start
```

Fetch the tournament and pick a non-bye match id (optional `jq`). You can do this after the bracket step; match ids are
stable once the bracket exists:

```bash
curl -s -b org.txt http://localhost:8080/api/v1/tournaments/TOURNAMENT_ID
# Or: jq -r '.rounds[0].matches[] | select(.isBye==false) | .id' | head -1
```

Start that match (creates or returns the backing game), then drive the game the same way as in the standalone
example—using the **organizer** cookie if they are `createdBy` on the tournament game:

```bash
curl -i -b org.txt -X POST \
  http://localhost:8080/api/v1/tournaments/TOURNAMENT_ID/matches/MATCH_ID/start

curl -i -b org.txt -X POST http://localhost:8080/api/v1/games/GAME_ID/start

curl -i -b org.txt -X POST http://localhost:8080/api/v1/games/GAME_ID/frames \
  -H "Content-Type: application/json" \
  -d '{"p1BagsIn":1,"p1BagsOn":0,"p2BagsIn":0,"p2BagsOn":0}'
```

Inspect the match:

```bash
curl -s -b org.txt http://localhost:8080/api/v1/tournaments/TOURNAMENT_ID/matches/MATCH_ID
```

**Doubles tournaments** require at least six players (even count); set `"gameType":"DOUBLES"` on create and register
enough accounts before `POST .../bracket`.

## Project Status

The backend implements the features described above and is deployed publicly on Azure App Service against a Neon
PostgreSQL database. A companion frontend is planned so the project can be demonstrated through a complete user flow
instead of API calls alone.

## Next Steps

Current follow-up work is focused on:

- GitHub Actions CI/CD so deploys happen on push to `main` instead of via the manual build/push/restart cycle
- adding a lightweight frontend for demos and real usage
- continuing to refine the tournament experience as new use cases come up
