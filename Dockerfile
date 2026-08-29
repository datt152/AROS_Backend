# =============================================================================
# Stage 1: Build JAR with Maven
# =============================================================================
FROM maven:3.9-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

RUN chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src ./src

RUN ./mvnw clean package -DskipTests -B

# =============================================================================
# Stage 2: Run Spring Boot app
# =============================================================================
FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

RUN addgroup -S aros && adduser -S aros -G aros \
    && mkdir -p /tmp/uploads/omr \
    && chown -R aros:aros /app /tmp/uploads/omr

USER aros

COPY --from=build /app/target/aros-core-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
