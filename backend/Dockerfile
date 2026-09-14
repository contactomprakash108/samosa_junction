
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
COPY src src
RUN --mount=type=cache,target=/root/.m2 mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl \
    && addgroup -S samosa \
    && adduser -S -G samosa samosa
WORKDIR /app
COPY --from=build --chown=samosa:samosa /app/target/samosa-junction-backend-0.1.0-SNAPSHOT.jar app.jar
USER samosa
EXPOSE 8080
HEALTHCHECK --interval=15s --timeout=5s --start-period=50s --retries=10 \
    CMD curl -fsS http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
