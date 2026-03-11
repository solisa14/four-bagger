# Four Bagger API

`four-bagger-api` is a Spring Boot backend for user registration, login, JWT-based authentication, profile management,
and refresh-token session handling.

## Tech Stack

- Java 25
- Spring Boot 4
- Spring Web MVC
- Spring Security
- Spring Data JPA
- Flyway
- PostgreSQL
- Testcontainers

## Local Setup

### Java

This project targets Java 25. Maven must run with a Java 25 JDK or the build will fail before tests start.

Check what Maven is using:

```bash
java -version
mvn -version
```

If Maven is not using Java 25, point it to a Java 25 JDK before building:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
mvn -version
```

Why this matters:

- `pom.xml` targets Java 25.
- Java 25 produces class file version `69`.
- Running Maven with Java 24 or older can fail with a `class file version` mismatch even if the code itself is correct.

### Docker

The test profile uses Testcontainers PostgreSQL:

```yaml
spring:
  datasource:
    url: jdbc:tc:postgresql:14-alpine:///fourbagger
```

That means integration and JPA tests need a working Docker runtime. Before running the full test suite, start Docker
Desktop, Colima, OrbStack, or another compatible Docker daemon.

## Common Commands

Compile the project:

```bash
mvn clean compile
```

Run the full test suite:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
mvn clean test
```

Run the app locally:

```bash
export JAVA_HOME=$(/usr/libexec/java_home -v 25)
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

## Configuration Notes

- Main configuration lives in `src/main/resources/application.yml`.
- `application-dev.yml` is intended for local PostgreSQL development.
- `application-prod.yml` is intended for deployed environments.
- No default runtime profile is forced in `application.yml`; set `SPRING_PROFILES_ACTIVE` (or
  `-Dspring-boot.run.profiles=...`) explicitly.
- Secure auth cookies default to `true` in base config and are only relaxed in local/test profiles.
- H2 console access is disabled by default and only enabled when `spring.h2.console.enabled=true` (enabled in `dev`
  profile).
- Flyway owns schema changes. Hibernate is configured with `ddl-auto: validate`, so entity changes should be accompanied
  by migrations.

## Testing Strategy

The project uses a layered test approach:

- Unit tests for service logic
- `@WebMvcTest` for controller validation and HTTP behavior
- `@DataJpaTest` for repository behavior
- `@SpringBootTest` + `MockMvc` for end-to-end flows

If a test uses the `test` profile or extends the shared integration/JPA test base classes, expect Docker-backed
PostgreSQL to be involved.
