# Build stage
FROM maven:3.9-eclipse-temurin-21 AS builder

WORKDIR /app

COPY pom.xml .

RUN mvn dependency:go-offline -B

COPY src ./src

RUN mvn clean package -DskipTests -B

# Runtime stage
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -g 1001 -S appgroup && \
    adduser -u 1001 -S appuser -G appgroup

COPY --from=builder /app/target/travel_management_system-0.0.1-SNAPSHOT.jar app.jar
COPY entrypoint.sh ./entrypoint.sh

RUN chmod +x entrypoint.sh && chown appuser:appgroup app.jar entrypoint.sh

USER appuser

EXPOSE 8080

ENV SPRING_PROFILES_ACTIVE=docker \
    JAVA_OPTS="-XX:MaxRAMPercentage=75.0 -Djava.security.egd=file:/dev/./urandom"

ENTRYPOINT ["./entrypoint.sh"]
