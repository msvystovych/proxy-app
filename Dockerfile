# ────────────────────────────────────────────────
# Stage 1: Build with Maven
# ────────────────────────────────────────────────
FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy project files
COPY pom.xml .
COPY src ./src

# Build the application (skip tests for Docker build)
RUN mvn clean package -DskipTests

# ────────────────────────────────────────────────
# Stage 2: Runtime with lightweight JRE
# ────────────────────────────────────────────────
FROM eclipse-temurin:17-jre

WORKDIR /app

# Copy the fat JAR from build stage
COPY --from=builder /app/target/*.jar app.jar

# Expose app port (if Spring Boot default)
EXPOSE 8080

# Run Spring Boot app
ENTRYPOINT ["java", "-jar", "app.jar"]