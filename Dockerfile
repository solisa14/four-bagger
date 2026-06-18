# syntax=docker/dockerfile:1.7

# Local development image for docker compose up --build.
# Compose supplies database and app env vars at runtime.

# ---- Build stage -------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

COPY src ./src
RUN ./mvnw -B -ntp clean package -DskipTests \
    && cp target/*.jar /workspace/app.jar

# ---- Runtime stage -----------------------------------------------------------
FROM eclipse-temurin:25-jre

RUN groupadd --system app && useradd --system --gid app --home /app app
WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar
RUN chown -R app:app /app
USER app

ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=dev \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
