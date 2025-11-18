# Multi-stage build for SQL SaaS Platform Server
FROM gradle:8.5-jdk17 AS builder

WORKDIR /app

# Copy gradle and build files
COPY build.gradle .
COPY settings.gradle .
COPY gradle gradle/

# Copy all module source code
COPY core core/
COPY connector connector/
COPY sqlparser sqlparser/
COPY persistence persistence/
COPY entitlement entitlement/
COPY server server/

# Build the application (skip tests for faster builds)
# Use gradle command directly since gradlew doesn't exist
RUN gradle :server:bootJar -x test --no-daemon

# Runtime stage
FROM eclipse-temurin:17-jre

WORKDIR /app

# Install curl for healthchecks
RUN apt-get update && apt-get install -y curl && rm -rf /var/lib/apt/lists/*

# Create logs directory
RUN mkdir -p /app/logs

# Copy the built jar from builder stage
COPY --from=builder /app/server/build/libs/*.jar /app/sql-saas-platform.jar

# Expose application port
EXPOSE 9090

# Environment variables with defaults
ENV JAVA_OPTS="-Xms512M -Xmx1024M"
ENV SPRING_PROFILES_ACTIVE="docker"

# Entrypoint
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar /app/sql-saas-platform.jar"]
