# Multi-stage Dockerfile
# Build stage uses Maven image with Temurin JDK 21
FROM maven:3.9.4-eclipse-temurin-21-alpine AS build

WORKDIR /workspace

# copy maven wrapper and pom first to take advantage of layer caching
COPY mvnw pom.xml ./.mvn/ ./
COPY .mvn .mvn
RUN chmod +x ./mvnw || true

# download dependencies
RUN ./mvnw -B -DskipTests dependency:go-offline

# copy sources and build
COPY src ./src
RUN ./mvnw -B -DskipTests package

# Runtime stage: small JRE image
FROM eclipse-temurin:21.0.11_10-jre-alpine

WORKDIR /app

# copy assembled jar from the build stage
COPY --from=build /workspace/target/*.jar /app/app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]

