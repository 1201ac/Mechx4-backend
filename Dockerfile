# Build Stage: Build Backend JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY mvnw mvnw.cmd ./
COPY .mvn ./.mvn
COPY pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw -DskipTests package

# Production Stage: Run Standalone JAR
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S nerlogix && adduser -S nerlogix -G nerlogix
WORKDIR /app
EXPOSE 8080
COPY --from=builder /app/target/nerlogix.jar app.jar
RUN chown -R nerlogix:nerlogix /app
USER nerlogix
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
