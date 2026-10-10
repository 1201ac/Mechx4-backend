# Stage 1: Build Frontend
FROM node:20-alpine AS frontend-builder
WORKDIR /app/frontend
ARG VITE_GOOGLE_MAPS_API_KEY
ARG VITE_GOOGLE_MAP_ID
ENV VITE_GOOGLE_MAPS_API_KEY=$VITE_GOOGLE_MAPS_API_KEY
ENV VITE_GOOGLE_MAP_ID=$VITE_GOOGLE_MAP_ID
COPY frontend/package*.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Stage 2: Build Backend JAR
FROM eclipse-temurin:21-jdk-alpine AS backend-builder
WORKDIR /app
COPY backend/mvnw backend/mvnw.cmd ./backend/
COPY backend/.mvn ./backend/.mvn
COPY backend/pom.xml ./backend/
WORKDIR /app/backend
RUN ./mvnw dependency:go-offline -B
COPY src ./src
COPY --from=frontend-builder /app/frontend/dist ./src/main/resources/static
RUN ./mvnw -DskipTests package

# Stage 3: Production Runner
FROM eclipse-temurin:21-jre-alpine
RUN addgroup -S nerlogix && adduser -S nerlogix -G nerlogix
WORKDIR /app
EXPOSE 8080
COPY --from=backend-builder /app/backend/target/nerlogix.jar app.jar
RUN chown -R nerlogix:nerlogix /app
USER nerlogix
HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8080/api/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
