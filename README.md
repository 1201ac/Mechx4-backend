# NER-Logix Backend (Java 21 + Spring Boot 3.3)

Logistics and Accessibility Management Platform for the North Eastern Region — Backend REST API & Realtime SSE Service.

## Tech Stack
- **Framework:** Java 21, Spring Boot 3.3+, Spring Security (JWT + API Key), Spring Data JPA (Hibernate)
- **Database:** H2 (dev/test file & in-memory) / MySQL (TiDB Cloud) / PostgreSQL 16 (prod profile)
- **Migrations:** Flyway SQL migrations (`src/main/resources/db/migration`)
- **Realtime:** Server-Sent Events (`GET /api/realtime/stream`)
- **API Docs:** OpenAPI / Swagger UI at `/api/docs` and `/swagger-ui.html`

## Local Development

```bash
# Run unit & integration tests
./mvnw test

# Start local backend server (port 8080 by default with H2 dev DB)
./mvnw spring-boot:run

# Build executable JAR
./mvnw clean package
```

## Deployment to Render

This backend service is configured for deployment to **Render** as a Web Service.

### Option A: Render Blueprint (Recommended)
1. In Render Dashboard, click **New +** -> **Blueprint**.
2. Connect your `NerLogixBackend` repository.
3. Render will automatically detect `render.yaml` and configure the service.

### Option B: Manual Web Service Setup
1. Create a new **Web Service** on Render and select the `NerLogixBackend` repo.
2. Select **Docker** environment (or **Java** runtime).
3. If using Docker, set Dockerfile path to `Dockerfile`.
4. Set Health Check Path: `/api/health`
5. Configure Environment Variables:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `SPRING_DATASOURCE_URL`: `jdbc:mysql://<tidb-host>:4000/<db>?sslMode=VERIFY_IDENTITY` (or PostgreSQL JDBC URL)
   - `SPRING_DATASOURCE_USERNAME`: `<db-user>`
   - `SPRING_DATASOURCE_PASSWORD`: `<db-password>`
   - `NERLOGIX_JWT_SECRET`: `<secure-random-32-char-string>`
   - `NERLOGIX_CORS_ALLOWED_ORIGINS`: `https://<your-vercel-app>.vercel.app` (or `*` to allow all origins)
