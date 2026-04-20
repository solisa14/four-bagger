# syntax=docker/dockerfile:1.7

# ---- Build stage -------------------------------------------------------------
# Use a Java 25 JDK + Maven image so we don't need Java 25 installed locally.
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /workspace

# Copy Maven wrapper + pom first so dependency resolution can be cached.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN ./mvnw -B -ntp dependency:go-offline

# Copy sources and build the executable jar (skip tests; CI should run them).
COPY src ./src
RUN ./mvnw -B -ntp clean package -DskipTests \
    && cp target/*.jar /workspace/app.jar

# ---- Runtime stage -----------------------------------------------------------
# Slim JRE-only image keeps the final image small.
FROM eclipse-temurin:25-jre

# Run as a non-root user.
RUN groupadd --system app && useradd --system --gid app --home /app app
WORKDIR /app

COPY --from=build /workspace/app.jar /app/app.jar
RUN chown -R app:app /app
USER app

# Azure App Service for Containers sends traffic to the port set by WEBSITES_PORT
# (defaults to 80). Spring Boot honors SERVER_PORT, which we set to match.
ENV SERVER_PORT=8080 \
    SPRING_PROFILES_ACTIVE=prod \
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
