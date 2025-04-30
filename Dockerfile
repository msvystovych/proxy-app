# Use lightweight JDK image
FROM eclipse-temurin:17-jdk-jammy as builder

WORKDIR /app
COPY . .
RUN ./mvnw clean package -DskipTests

# Final stage
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app
COPY --from=builder /app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]