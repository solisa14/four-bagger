# syntax=docker/dockerfile:1.7

# API runtime image. Profile and secrets must be supplied at runtime
# (docker compose for local, ECS task definition for production).
# Do not bake SPRING_PROFILES_ACTIVE or secrets into this image.

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
    JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75"

EXPOSE 8080

ENTRYPOINT ["java","-jar","/app/app.jar"]
