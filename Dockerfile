# Build Stage: Build Backend JAR
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY mvnw mvnw.cmd ./
RUN chmod +x mvnw
COPY .mvn ./.mvn
COPY pom.xml ./
RUN ./mvnw dependency:go-offline -B
COPY src ./src
RUN ./mvnw -DskipTests package

# Production Stage: Run Standalone JAR
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S mechx4 && adduser -S mechx4 -G mechx4
WORKDIR /app
EXPOSE 8080
ENV SPRING_PROFILES_ACTIVE=prod
COPY --from=builder /app/target/*.jar app.jar
RUN chown -R mechx4:mechx4 /app
USER mechx4
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
