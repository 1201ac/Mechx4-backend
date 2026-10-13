# Mechx4 Backend (Java 21 + Spring Boot 3.3)

Logistics and Accessibility Management Platform — Backend REST API & Realtime SSE Service with TiDB Cloud.

## Tech Stack
- **Framework:** Java 21, Spring Boot 3.3.4, Spring Security (JWT + API Key), Spring Data JPA (Hibernate)
- **Database:** TiDB Cloud Serverless (MySQL 8.0 wire-compatible) with SSL/TLS identity verification / H2 (test profile)
- **Migrations:** Flyway SQL migrations (`src/main/resources/db/migration`, versions V1 through V5)
- **Realtime:** Server-Sent Events (`GET /api/realtime/stream`)
- **API Docs:** OpenAPI / Swagger UI at `/api/docs` and `/swagger-ui.html`

## Local Development

```bash
# Run unit & integration tests
./mvnw test

# Start local backend server (port 8080 by default)
./mvnw spring-boot:run

# Build executable JAR (target/mechx4.jar)
./mvnw clean package
```

## TiDB Cloud Configuration

- **Host:** `gateway01.ap-northeast-1.prod.aws.tidbcloud.com`
- **Port:** `4000`
- **Username:** `3KQW41Z7XRUm1xM.root`
- **Database:** `sys` (or `mechx4`)
- **Password:** Read dynamically via `DB_PASSWORD` or `SPRING_DATASOURCE_PASSWORD`
- **TLS/SSL URL:**
  `jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/sys?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3`

## Deployment to Render

This backend service is configured for deployment to **Render** as a Web Service.

### Option A: Render Blueprint (Recommended)
1. In Render Dashboard, click **New +** -> **Blueprint**.
2. Connect your repository.
3. Render will automatically detect `render.yaml` and configure the service.

### Option B: Manual Web Service Setup
1. Create a new **Web Service** on Render and select your repo.
2. Select **Docker** environment.
3. Set Dockerfile path to `Dockerfile` (or `backend/Dockerfile`).
4. Set Health Check Path: `/api/health`.
5. Configure Environment Variables:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `SPRING_DATASOURCE_URL`: `jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/sys?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3`
   - `SPRING_DATASOURCE_USERNAME`: `3KQW41Z7XRUm1xM.root`
   - `DB_PASSWORD`: `<your-db-password>`
   - `MECHX4_JWT_SECRET`: `<secure-random-32-char-string>`
   - `MECHX4_CORS_ALLOWED_ORIGINS`: `https://<your-vercel-app>.vercel.app`
