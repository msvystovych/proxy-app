FROM maven:3.9.5-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy only pom.xml and download dependencies first (leverages Docker cache)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source code and build the app
COPY src ./src
RUN mvn clean package -DskipTests -B


FROM eclipse-temurin:17-jre

# Use non-root user for security
RUN useradd -ms /bin/bash spring
USER spring

WORKDIR /app

# Copy only the final fat JAR (avoid *.jar pattern)
COPY --from=builder /app/target/*-SNAPSHOT.jar app.jar

# Expose default Spring Boot port
EXPOSE 8080

# Run Spring Boot
ENTRYPOINT ["java", "-jar", "app.jar"]