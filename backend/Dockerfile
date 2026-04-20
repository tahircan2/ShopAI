# Build Stage
FROM maven:3.9-eclipse-temurin-21-alpine AS build
WORKDIR /app

# Copy pom.xml and download dependencies (cache layer)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn package -DskipTests

# Runtime Stage
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user for security
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy jar from build stage
COPY --from=build /app/target/shopai-backend-*.jar app.jar

EXPOSE 8080

# Performance and memory tuning for container
ENTRYPOINT ["java", \
            "-XX:+UseParallelGC", \
            "-XX:MaxRAMPercentage=75.0", \
            "-jar", "app.jar"]
