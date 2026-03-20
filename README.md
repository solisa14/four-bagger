# Four Bagger API

Four Bagger API is a Spring Boot backend for organizing cornhole games and single-elimination tournaments. It supports user authentication, standalone singles and doubles games, tournament registration via join codes, bracket generation, round-level rule configuration, and automatic match progression as games are completed.

I built this project because I wanted something my family could actually use during cornhole tournaments at family functions, and I wanted a backend project that pushed me beyond basic CRUD. The goal was to practice backend engineering in a project with real domain rules, real state transitions, and a deployment path I can keep building toward.

## Highlights

- JWT-based authentication with refresh-token rotation and HttpOnly cookies
- Standalone singles and doubles game support
- Tournament lifecycle support from registration to bracket generation to live match progression
- Configurable round rules with `bestOf` series support and multiple scoring modes
- Event-driven tournament advancement when a game completes
- Flyway-managed schema changes with Hibernate validation and PostgreSQL persistence

## Tech Stack

- Java 25
- Spring Boot 4.0.1
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Testcontainers
- JUnit 5 and Mockito

## Architecture

The project uses a layered, package-by-feature structure:

- `auth` handles registration, login, logout, refresh-token rotation, and session persistence
- `user` handles profile reads and account updates
- `game` handles standalone game creation, frame recording, scoring, and game state transitions
- `tournament` handles tournament lifecycle, bracket generation, round configuration, and match progression
- `security` contains JWT parsing, authentication filters, and Spring Security configuration
- `common` contains shared exception handling and validation utilities

Some of the main engineering decisions in the codebase:

- Scoring rules use a strategy-style design through `GameScoringPolicy`, allowing different scoring modes to be selected from `Game.scoringMode`
- Controllers map request DTOs into command records before business logic runs, which keeps service boundaries explicit and validation closer to the domain
- Tournament progression is event-driven: completed games publish an event that the tournament match service listens to for advancing winners and managing best-of series
- Database changes are managed through Flyway migrations, while Hibernate runs in `validate` mode to prevent the schema from drifting away from the entity model

## Testing Approach

The project uses a layered test strategy instead of relying on only one kind of test:

- Unit tests for service-level business logic
- `@WebMvcTest` tests for controller behavior, validation, and HTTP responses
- `@DataJpaTest` tests for repository behavior against PostgreSQL via Testcontainers
- `@SpringBootTest` integration tests for full application flows

This lets the project validate domain rules quickly at the unit layer while still exercising the real persistence and API flow where it matters.

## Run Locally

### Prerequisites

- Java 25
- PostgreSQL running locally for the `dev` profile
- Docker running for the test suite because repository and integration tests use Testcontainers

### Environment Variables

For local development, the app expects:

- `JWT_SECRET`
- `DEV_DB_URL`
- `DEV_DB_USERNAME`
- `DEV_DB_PASSWORD`
- `SPRING_PROFILES_ACTIVE=dev`

This repository includes a `.env.dev` file with local development defaults and a `.env.prod` file showing the variables expected for a production deployment. Review the values and replace them with your own where appropriate.

### Start the App

Set Java 25 for Maven:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
```

Load local environment variables:

```bash
set -a
source .env.dev
set +a
```

Compile the project:

```bash
mvn clean compile
```

Run the API with the `dev` profile:

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Run the test suite:

```bash
mvn clean test
```

## API Snapshot

All endpoints are served under `/api/v1`.

### Representative Endpoints

| Area | Endpoints |
| --- | --- |
| Auth | `POST /auth/register`, `POST /auth/login`, `POST /auth/refresh-token`, `POST /auth/logout` |
| User | `GET /user/me`, `PATCH /user/me`, `PUT /user/me/password` |
| Games | `POST /games`, `POST /games/{gameId}/start`, `POST /games/{gameId}/frames`, `GET /games/{gameId}`, `GET /games/me`, `POST /games/{gameId}/cancel` |
| Tournaments | `POST /tournaments`, `GET /tournaments/{id}`, `POST /tournaments/join`, `POST /tournaments/{id}/bracket`, `PATCH /tournaments/{id}/rounds/{roundNumber}`, `POST /tournaments/{id}/start`, `DELETE /tournaments/{id}` |
| Tournament Matches | `POST /tournaments/{tournamentId}/matches/{matchId}/start`, `GET /tournaments/{tournamentId}/matches/{matchId}` |

### Register a User

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

The auth endpoints issue `accessToken` and `refreshToken` as HttpOnly cookies, so using a cookie jar makes it easy to call authenticated routes from the command line.

### Create a Tournament

```bash
curl -i -b cookies.txt \
  -X POST http://localhost:8080/api/v1/tournaments \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Family Cornhole Classic",
    "gameType": "DOUBLES"
  }'
```

### Generate a Bracket

```bash
curl -i -b cookies.txt \
  -X POST http://localhost:8080/api/v1/tournaments/{tournamentId}/bracket
```

### Start or Inspect a Tournament Match

Start the first game for a match:

```bash
curl -i -b cookies.txt \
  -X POST http://localhost:8080/api/v1/tournaments/{tournamentId}/matches/{matchId}/start
```

Inspect a match after progression begins:

```bash
curl -s -b cookies.txt \
  http://localhost:8080/api/v1/tournaments/{tournamentId}/matches/{matchId}
```

## Project Status

The backend is implemented and locally runnable today. Deployment is the next step, and a companion frontend is planned so the project can be demonstrated through a complete user flow instead of API calls alone.

## Next Steps

Current follow-up work is focused on:

- deploying the backend
- adding a lightweight frontend for demos and real usage
- continuing to refine the tournament experience as new use cases come up
