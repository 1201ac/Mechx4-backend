# Mechx4 — Logistics & Accessibility Management Platform

> Intelligent, real-time logistics and essential goods coordination platform featuring hazard-aware Dijkstra routing, GIS mapping, multi-role dispatch board, offline field reporting, and automated Server-Sent Events (SSE).

---

## 1. Project Overview & Architecture

**Mechx4** is an enterprise-grade full-stack platform engineered for resilient goods delivery and fleet coordination across complex terrain and severe weather conditions.

### Tech Stack
- **Frontend (`/frontend`):** React 18, TypeScript, Vite, Tailwind CSS, TanStack Query (v5), Framer Motion, Lenis Smooth Scroll, Lucide Icons, Google Maps JavaScript API & Google Directions, PWA (Service Worker + IndexedDB offline sync).
- **Backend (`/backend`):** Java 21, Spring Boot 3.3.4, Spring Security 6 (JWT + API Keys + Role-based access), Spring Data JPA (Hibernate), Flyway Migrations, Server-Sent Events (SSE).
- **Database:** TiDB Cloud Serverless (MySQL 8.0 wire-compatible distributed SQL) with mandatory TLS 1.2/1.3 identity verification; local fallback supported via H2 in-memory MySQL mode.
- **Deployments:**
  - Frontend: Independent SPA deployment to **Vercel** (`vercel.json` SPA routing configured).
  - Backend: Standalone containerized service deployable to **Render** (`render.yaml` blueprint + Dockerfile).

---

## 2. Directory Structure

```
nerlogix1/
├── frontend/                     # React 18 + Vite frontend application
│   ├── src/                      # Components, pages, contexts, hooks, i18n
│   ├── public/                   # Static assets & Mechx4 favicon (SVG)
│   ├── scripts/                  # Development verification scripts
│   ├── e2e/                      # Playwright end-to-end tests
│   ├── package.json              # Frontend dependencies and npm scripts
│   ├── vercel.json               # Vercel SPA rewrite configuration
│   └── .env.example              # Frontend environment template
│
├── backend/                      # Spring Boot 3.3 backend application
│   ├── src/main/java/            # Java controllers, services, repositories, entities
│   ├── src/main/resources/       # application.yml, application-prod.yml, Flyway SQL migrations
│   ├── docs/                     # Architecture specifications & matrix
│   ├── Dockerfile                # Render production container definition
│   ├── render.yaml               # Render Blueprint definition
│   ├── pom.xml                   # Maven build descriptor
│   └── .env.example              # Backend environment template
│
├── .gitignore                    # Central Git ignore rules (securing .env, keys, logs)
├── .gitattributes                # Line ending normalization
├── package.json                  # Root convenience scripts
└── README.md                     # Platform documentation
```

---

## 3. Database Configuration (TiDB Cloud)

Mechx4 connects to **TiDB Cloud Serverless** via the MySQL Connector/J driver with SSL certificate verification.

### TiDB Cloud Connection Details
- **Host:** `gateway01.ap-northeast-1.prod.aws.tidbcloud.com`
- **Port:** `4000`
- **Username:** `3KQW41Z7XRUm1xM.root`
- **Database:** `sys` (or dedicated `mechx4` application database)
- **SSL / TLS Mode:** `sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3`
- **Password:** Read dynamically from `DB_PASSWORD` (or `SPRING_DATASOURCE_PASSWORD`). **Never hardcoded.**

### JDBC Connection URL
```
jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/sys?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3
```

> [!NOTE]
> **Database Selection (`sys` vs `mechx4`):**
> The user account `3KQW41Z7XRUm1xM.root` holds global administrative privileges (`*.* WITH GRANT OPTION`). The `sys` database contains MySQL system views (e.g. `schema_unused_indexes`). A dedicated database `mechx4` has also been provisioned and fully migrated with all 24 application tables. You can target either database simply by setting `SPRING_DATASOURCE_URL` or changing the database name segment in your connection string.

---

## 4. Database Migrations (Flyway)

Database schema evolution is strictly versioned and automatically managed by **Flyway**:

| Version | Migration Script | Description |
|---|---|---|
| `V1` | `V1__schema.sql` | Core schema: users, districts, road nodes, road segments, vehicles, shipments, incidents, alerts, settings, audit logs. |
| `V2` | `V2__schema_updates_and_indexes.sql` | Composite indexes and performance optimizations for routing and GIS queries. |
| `V3` | `V3__add_heading_to_vehicle_positions.sql` | Compass heading tracking on GPS vehicle coordinates. |
| `V4` | `V4__vehicle_ownership.sql` | Driver-to-vehicle association and ownership tracking. |
| `V5` | `V5__dispatch.sql` | Multi-role dispatch assignments and history table (`shipment_assignments`). |

Flyway automatically validates and applies pending migrations on backend startup.

---

## 5. Environment Variables & Setup

Create a `.env` file in `backend/` and `frontend/` (or copy from `.env.example`).

### Backend (`backend/.env`)
```bash
# TiDB Cloud Connection
SPRING_DATASOURCE_URL=jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/sys?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3
SPRING_DATASOURCE_USERNAME=3KQW41Z7XRUm1xM.root
DB_PASSWORD=<your_tidb_password>
SPRING_DATASOURCE_PASSWORD=<your_tidb_password>

# Profile & Port
SPRING_PROFILES_ACTIVE=prod
PORT=8080

# Security (minimum 32 characters)
MECHX4_JWT_SECRET=your_secure_random_jwt_secret_at_least_32_characters_long_here

# CORS Allowed Origins
MECHX4_CORS_ALLOWED_ORIGINS=https://mechx4-frontend.vercel.app,http://localhost:5173,http://localhost:8080
```

### Frontend (`frontend/.env`)
```bash
# Backend REST API Base URL
VITE_API_BASE_URL=/api

# Optional Google Maps Configuration
VITE_GOOGLE_MAPS_API_KEY=
VITE_GOOGLE_MAP_ID=
```

---

## 6. Seeded Operational Accounts

All demo accounts share the password: `Demo@1234`

| Email | Role | Scope of Access |
|---|---|---|
| `admin@nerlogix.com` | **ADMIN** | System owner, user management, rule thresholds, API keys, audit log, simulation controls. |
| `moderator@nerlogix.com` | **MODERATOR** | Dispatch board, field report verification, incident lifecycle, alert resolution, fleet controls. |
| `user@nerlogix.com` | **USER / DRIVER** | Driver navigation mode, shipment delivery acceptance, offline field reporting, weather logging. |

---

## 7. Local Development Instructions

### Prerequisites
- **Node.js:** v18+ (Node 20 or 22 recommended)
- **Java:** JDK 21+
- **Maven:** Bundled via `./mvnw` (or `.\mvnw.cmd` on Windows)

### 1. Start Backend Server
```bash
cd backend
# Windows:
.\mvnw.cmd spring-boot:run
# Linux / macOS:
./mvnw spring-boot:run
```
The REST API will start on `http://localhost:8080`.
Interactive OpenAPI docs are available at `http://localhost:8080/swagger-ui.html`.

### 2. Start Frontend Server
```bash
cd frontend
npm install
npm run dev
```
The frontend Vite server will be available at `http://localhost:5173` with automatic API proxying to port 8080.

---

## 8. Verification & Testing

Run all quality checks across frontend and backend:

```bash
# 1. Backend test suite (72 unit & integration tests)
cd backend
.\mvnw.cmd test

# 2. Frontend TypeScript compilation
cd ../frontend
npm run typecheck

# 3. Frontend ESLint verification
npm run lint

# 4. Frontend Vitest unit tests
npm test -- --run

# 5. Frontend Production Build
npm run build
```

---

## 9. Deployment Instructions

### Frontend (Vercel)
1. Push the repository to GitHub.
2. In Vercel, click **New Project** and select the repository.
3. Set the Root Directory to `frontend`.
4. Add Environment Variable:
   - `VITE_API_BASE_URL`: `https://<your-render-backend-name>.onrender.com/api`
5. Click **Deploy**. Vercel will build the SPA and apply rewrite rules from `vercel.json`.

### Backend (Render)
1. In Render Dashboard, click **New +** -> **Web Service** (or **Blueprint** with `backend/render.yaml`).
2. Set Root Directory to `backend` (if using separate repo, root `/`).
3. Environment: **Docker** (using `Dockerfile`).
4. Set Health Check Path: `/api/health`.
5. Configure Environment Variables:
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `SPRING_DATASOURCE_URL`: `jdbc:mysql://gateway01.ap-northeast-1.prod.aws.tidbcloud.com:4000/sys?sslMode=VERIFY_IDENTITY&enabledTLSProtocols=TLSv1.2,TLSv1.3`
   - `SPRING_DATASOURCE_USERNAME`: `3KQW41Z7XRUm1xM.root`
   - `DB_PASSWORD`: `<your-tidb-password>`
   - `MECHX4_JWT_SECRET`: `<secure-random-32-char-key>`
   - `MECHX4_CORS_ALLOWED_ORIGINS`: `https://<your-vercel-app>.vercel.app`
