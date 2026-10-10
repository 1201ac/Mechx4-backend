# NER-Logix: Build Prompt (React frontend + Java Spring Boot backend)

Save this file as `AGENTS.md` in an empty project folder. It is the only specification. Read all of it before writing code. Do not invent features outside it.

---

## PART 1. WHAT YOU ARE BUILDING

### 1.1 Problem
The North Eastern Region (NER) of India faces severe logistics and accessibility problems from landslides, floods, heavy rainfall, road damage and poor connectivity. Essential goods (medicines, food, construction material, agricultural produce) reach remote districts late, causing shortages and fragmented information.

### 1.2 Product
**NER-Logix: Logistics and Accessibility Management Platform for the North Eastern Region.**
A **conventional, rule-based software platform** (not a machine-learning product; do not use the words "AI-powered" or "machine learning" anywhere in the UI or README). It combines standard GPS vehicle tracking, GIS mapping, manual field reports and weather conditions into one central database, and gives authorities and logistics operators real-time operational visibility, rule-based alerts for blocked routes and critical incidents, and decisive route action. Offline field reporting stores data locally and syncs when connectivity returns.

Tagline: "Fast, structured and transparent essential goods coordination."
It is a demo with fictional operational data. No government emblems; do not claim to be an official system.

### 1.3 Objectives (each must be demonstrably working)
1. **Monitor accessibility:** track road, bridge and route conditions across terrain; district connectivity status.
2. **Track essential goods:** GPS locations of vehicles carrying critical supplies; delivery status.
3. **Field incident reporting:** landslides, floods and road damage reported from the field with geo-tagged photos.
4. **Information transparency:** accurate route and accessibility data for authorities and operators.
5. **Event notification:** immediate alerts for blocked roads, delays and important logistical incidents, in English, Hindi, Assamese and Bengali.
6. **Alternate routes and delay estimates:** when a road is blocked or risky, suggest safer alternative routes with estimated delay (rule-based).
7. **Dashboards:** district-wise connectivity, logistics bottlenecks and supply gaps, emergency/disaster-time routes, real-time movement and delivery status.
8. **Offline support:** installable PWA, local queue, sync on reconnect, exactly once.

### 1.4 System modules (map to the architecture diagram in the abstract)
GIS Dashboard · Vehicle Tracking · Field Reporting · Alert System · Offline Reporting · Central Management (admin).
**System outputs, each a real screen or feature:** Live Map Overlay · Instant Alert Notifications · Blocked Route Status · Incident History Logs · Vehicle Delivery Status.

### 1.5 The operational pipeline (the heart of the system; implement and test it as one chain)
```
1 Field event / GPS created
2 Data upload / offline sync
3 Central DB processing   (validate, store, apply status rules)
4 GIS map & dashboard update   (pushed live to every open screen)
5 Alerts sent to stakeholders   (in-app notifications in each user's language)
6 Route action / response   (diversion proposed, accepted, vehicle follows new route)
```
A Playwright spec `pipeline.spec.ts` must drive all six steps in one run (see Phase 9).

### 1.6 Accounts: three roles only
| Role | Maps to | Can do |
|---|---|---|
| **USER** | Field officials, drivers, local staff | View map, dashboards, weather, planner. Submit field reports (also offline). Log local weather conditions. Receive notifications. If assigned a shipment as driver: Driver mode (trip, GPS sharing, accept safer route, mark delivered, report emergency). |
| **MODERATOR** | Logistics operators and authorities | Everything USER can, plus: verify/reject reports, manage incidents, set road/bridge status, acknowledge/resolve alerts, create and manage shipments and vehicles, update district stock, broadcast notifications, toggle emergency mode. |
| **ADMIN** | System owner (Central Management) | Everything MODERATOR can, plus: manage users and roles, edit districts and road network data, edit rule thresholds, create integration API keys, view audit log, demo tools. |

Seeded demo accounts, shared password `Demo@1234`: `admin@nerlogix.com`, `moderator@nerlogix.com`, `user@nerlogix.com` (this user is also the driver of one IN_TRANSIT shipment). Self-registration creates USER accounts. No other demo roles.

---

## PART 2. HOW YOU MUST WORK (strict)

### 2.1 The verify-and-fix loop (most important rule)
For **every task** (one small end-to-end slice: database → API → UI):
1. **Build** it.
2. **Run** the checks: `node scripts/check.mjs` (backend compile + unit/integration tests, frontend typecheck + lint + tests) and the task's own test; then open the app in a real browser with Playwright, perform the user action, and read the page and the browser console.
3. **Read the first error only.** Find the root cause. Fix it. Re-run.
4. If the same error survives 3 attempts, change approach or simplify the feature. If still failing, record it in `PROGRESS.md > Blockers` and ship the simplest version that works. Never leave a feature broken.
5. Only when everything is green, tick the task and continue.
6. At the end of each phase run `node scripts/verify.mjs` (2.3). It must be fully green before the next phase. If a later change breaks an earlier feature, fix it immediately.

Never move on with a known error. Never delete or weaken a failing test. Never hide a console error.

### 2.2 Definition of "working"
- The backend endpoint validates input, enforces the role, returns the standard error shape, and has a test.
- The UI calls the real endpoint. **No mock data, no hardcoded KPIs, no fixed arrays of rows or markers in the frontend, no fake loading timers.**
- Loading, empty, error (with Retry) and success states exist.
- Every button works or does not exist. No `TODO`, "coming soon", `alert()` stubs, dead links.
- You have seen it work in the browser and the console is clean.

### 2.3 Scripts to create in Phase 1 (cross-platform Node scripts; the developer uses Windows)
- `scripts/check.mjs`: runs `mvnw -q test` in `backend/`, and `npm run typecheck && npm run lint && npm test -- --run` in `frontend/`. Prints one line per step, and only the first error on failure.
- `scripts/verify.mjs`: runs `check`, builds everything (`scripts/build.mjs`), starts the built jar on a free port with a temporary H2 database, waits for `/api/health`, runs the Playwright specs in `e2e/`, then stops the server. One pass/fail line per step.
- `scripts/dev.mjs`: starts the Spring Boot backend (port 8080) and Vite dev server (port 5173, proxies `/api`) together.
- `scripts/build.mjs`: builds the frontend and copies `frontend/dist` into `backend/src/main/resources/static`, then `mvnw -q -DskipTests package`. Result: one runnable jar `backend/target/nerlogix.jar` serving API and UI.

### 2.4 Working discipline
- At session start read this file and `PROGRESS.md`; resume at the first unchecked task.
- View a file before editing it. Targeted edits only.
- Keep command output short (`-q`, `--silent`, `| tail -n 30`).
- Do not ask the user questions. Decide and record it in `PROGRESS.md > Decisions`.
- No dependency outside Part 3 unless you record why.
- Commit after each phase: `git commit -m "phase N: ..."`.
- `PROGRESS.md` stays under 120 lines: decisions, blockers, phase checklist, last 10 notes.

---

## PART 3. TECH STACK (fixed)

### Backend (Java)
Java 21, Spring Boot 3.3+, Maven with the Maven Wrapper (`mvnw`), packages under `com.nerlogix`.
Spring Web, Spring Security, Spring Data JPA (Hibernate), Bean Validation, Flyway, springdoc-openapi, jjwt (JWT), BCrypt, Spring `@Scheduled`, Spring events, `SseEmitter` for realtime. Tests: JUnit 5, Spring Boot Test, MockMvc, AssertJ.
**Do not use Lombok.** Use plain entities (getters/setters) and Java `record`s for DTOs.
**Database:** `dev` and `test` profiles use H2 in PostgreSQL compatibility mode (file DB `./data/nerlogix` for dev, in-memory for tests) so nothing needs installing. `prod` profile uses PostgreSQL 16. Flyway migrations (`V1__schema.sql`, later `V2__...`) use only portable SQL: `BIGINT GENERATED BY DEFAULT AS IDENTITY`, `VARCHAR`, `TEXT`, `DOUBLE PRECISION`, `BOOLEAN`, `TIMESTAMP`. Set `spring.jpa.hibernate.ddl-auto=none` and `spring.jpa.open-in-view=false`; add a test that loads every repository and runs a `count()` so mapping errors are caught early.
**Spatial data:** coordinates are `double lat/lng`; polylines and GeoJSON are stored as `TEXT`; spatial queries use bounding box + haversine in `GeoUtils`. The schema is "spatial-ready" for PostGIS later; do not use PostGIS functions.
**Seed data:** a `SeedService` (Java, `Random(42)`, idempotent, runs only if the users table is empty) creates all demo data. Do not hand-write large SQL inserts.
**Realtime:** `GET /api/realtime/stream` (SSE) authenticated by `Authorization: Bearer`. Heartbeat comment every 20 s; header `Cache-Control: no-cache`, `X-Accel-Buffering: no`. Publish events with `@TransactionalEventListener(phase = AFTER_COMMIT)` so clients never see uncommitted data. Events: `vehicle.update`, `alert.created`, `alert.updated`, `shipment.updated`, `incident.created`, `incident.updated`, `road.updated`, `notification.created`, `emergency.changed`, `weather.updated`.
**Auth:** access token 15 min, refresh token 7 days (stored hashed, rotated on every use). Rate-limit login (simple in-memory limiter).
**Uploads:** `FileStorage` interface, local folder `./uploads`; ≤ 5 MB; jpeg/png/webp only; magic-byte check; random filenames; served through an authenticated endpoint.
**Weather:** two sources. (1) **Manual condition logs** by USER/MODERATOR/ADMIN per district (condition NONE/LIGHT/MODERATE/HEAVY/EXTREME, optional rainfall mm, note); works offline. (2) Optional automatic fetch from Open-Meteo (setting `weather.autoFetch`, default on in dev); any failure falls back silently to the latest manual or seeded data and the UI shows a small "Last updated" tag with the source.

### Frontend (React)
React 18, Vite, TypeScript, React Router, TanStack Query, one Axios instance, Tailwind CSS, react-leaflet + leaflet, Recharts, Framer Motion (`motion`), **Lenis** (smooth scroll), lucide-react, i18next, `idb`, react-hook-form + zod, `@microsoft/fetch-event-source` (SSE with auth header), `vite-plugin-pwa`, Vitest + Testing Library, Playwright (in `e2e/` at repo root). Fonts self-hosted via `@fontsource`. Map tiles: CARTO `light_all`, fallback to standard OSM tiles.

### Languages
`en`, `hi`, `as`, `bn`.

---

## PART 4. DOMAIN, RULES AND DATA

### 4.1 Tables (Flyway V1)
`users`, `refresh_tokens`, `districts` (state, name, lat, lng, population_band), `road_nodes` (name, lat, lng, district_id, kind CITY|JUNCTION|DEPOT|HOSPITAL), `road_segments` (from_node, to_node, name, highway_code, asset_type ROAD|BRIDGE|PASS, length_km, base_speed_kmph, terrain_class 1–5, landslide_prone, flood_prone, status OPEN|RESTRICTED|BLOCKED, risk_score, risk_factors TEXT json, status_reason, polyline TEXT json, updated_at), `road_status_history`, `weather_logs` (district_id, condition, rain_mm, note, source MANUAL|OPEN_METEO|SEED, logged_by, logged_at), `vehicles` (reg_no, type TRUCK|MEDICAL_VAN|TANKER|MINI_TRUCK, capacity_kg, status IDLE|ON_TRIP|MAINTENANCE, lat, lng, heading, speed_kmph, last_seen), `vehicle_positions`, `shipments` (code, cargo_category MEDICINE|FOOD|CONSTRUCTION|AGRI|OTHER, cargo_description, weight_kg, priority NORMAL|HIGH|EMERGENCY, origin_node, destination_node, vehicle_id, driver_user_id, status DRAFT|ASSIGNED|IN_TRANSIT|DELAYED|DELIVERED|CANCELLED, planned_eta, current_eta, progress_pct, delay_min, started_at, delivered_at), `route_plans` (shipment_id, segment_ids TEXT json, geometry TEXT json, distance_km, eta_min, risk_score, status ACTIVE|PROPOSED|REJECTED|SUPERSEDED, reason), `incidents` (type LANDSLIDE|FLOOD|ROAD_DAMAGE|BRIDGE_DAMAGE|CONGESTION|ACCIDENT|OTHER, severity 1–4, lat, lng, segment_id, district_id, description, status REPORTED|VERIFIED|RESOLVED|REJECTED, source FIELD|SYSTEM|INTEGRATION|DRIVER, report_id, created_by, created_at, resolved_at), `incident_events` (incident_id, action, by_user, note, at: this is the Incident History Log), `field_reports` (client_id UNIQUE, user_id, lat, lng, accuracy, captured_at, type, severity, note, status PENDING|VERIFIED|REJECTED, moderator_note, incident_id), `report_images`, `alerts` (type ROAD_BLOCKED|DISTRICT_INACCESSIBLE|DELIVERY_DELAYED|HIGH_RISK_CORRIDOR|INCIDENT|EMERGENCY, severity INFO|WARNING|CRITICAL, entity_type, entity_id, **open_key UNIQUE NULLABLE** (holds the dedupe key while OPEN/ACKNOWLEDGED, set to NULL when resolved; unique indexes allow many NULLs on both H2 and PostgreSQL), status OPEN|ACKNOWLEDGED|RESOLVED, title/message TEXT json `{en,hi,as,bn}`, created_at, resolved_at), `notifications` (user_id, alert_id nullable, title/message json, read_at, created_at), `district_stock` (district_id, category, stock_units, daily_demand_units, updated_at), `rule_config` (key, value, description: admin-editable thresholds), `api_keys`, `audit_log`, `settings` (key, value: emergency_mode, weather.autoFetch, simulation.enabled).

### 4.2 Seed content
Real approximate coordinates for road nodes (do not change):
Guwahati 26.1445,91.7362 · Shillong 25.5788,91.8933 · Tura 25.5142,90.2024 · Jowai 25.4333,92.2000 · Nagaon 26.3500,92.6833 · Bongaigaon 26.4780,90.5580 · Tezpur 26.6528,92.7926 · Jorhat 26.7509,94.2037 · Sivasagar 26.9833,94.6333 · Dibrugarh 27.4728,94.9120 · Tinsukia 27.4922,95.3558 · Lumding 25.7500,93.1667 · Haflong 25.1667,93.0167 · Silchar 24.8333,92.7789 · Kailashahar 24.3333,92.0000 · Agartala 23.8315,91.2868 · Udaipur-Tripura 23.5333,91.4833 · Aizawl 23.7271,92.7176 · Lunglei 22.8870,92.7330 · Imphal 24.8170,93.9368 · Churachandpur 24.3333,93.6833 · Dimapur 25.9063,93.7276 · Kohima 25.6751,94.1086 · Mokokchung 26.3220,94.5150 · Mon 26.7333,95.0333 · Itanagar 27.0844,93.6053 · Ziro 27.5450,93.8300 · Pasighat 28.0660,95.3260 · Along 28.1667,94.8000 · Bomdila 27.2645,92.4157 · Tawang 27.5860,91.8594 · Siliguri 26.7271,88.3953 · Gangtok 27.3314,88.6138 · Namchi 27.1667,88.3500 · Mangan 27.5167,88.5333.

- **≥ 60 road segments** (≥ 6 BRIDGE, ≥ 6 PASS) along real corridors: Siliguri–Bongaigaon–Guwahati; Guwahati–Nagaon–Jorhat–Sivasagar–Dibrugarh–Tinsukia; Guwahati–Shillong–Jowai–Haflong–Silchar; Silchar–Aizawl; Silchar–Kailashahar–Agartala–Udaipur; Aizawl–Lunglei; Guwahati–Nagaon–Lumding–Dimapur; Haflong–Lumding; Dimapur–Kohima–Imphal; Dimapur–Mokokchung–Mon; Silchar–Imphal (via Churachandpur); Guwahati–Tezpur–Bomdila–Tawang; Tezpur–Itanagar–Ziro; Jorhat–Pasighat–Along; Siliguri–Gangtok–Mangan; Siliguri–Namchi; Guwahati–Tura. Polylines: 4–8 points interpolated between node coordinates with a small deterministic offset (label as "illustrative geometry" on the About page).
- The graph must be **connected**, with **two clearly different paths Guwahati→Imphal** (via Dimapur–Kohima, and via Shillong–Jowai–Haflong–Silchar) and alternatives for Guwahati→Aizawl and Guwahati→Agartala.
- ≥ 4 real districts per state (≥ 32) with approximate centroid and nearest node; stock for 4 categories per district (about 20% of cells below 3 days of cover); 3–5 hospital/depot nodes per state.
- 30 vehicles, 12 shipments (4 IN_TRANSIT with routes, 2 DELAYED, 3 ASSIGNED, 3 DELIVERED), 15 incidents with history events, 10 field reports (small generated sample PNGs), 10 alerts, weather logs for every district, 1 BLOCKED segment and 3 RESTRICTED.

### 4.3 Rule engine (transparent, configurable via `rule_config`; interface `RiskEngine` so it can be replaced later)
**Segment risk score 0–100** = sum, clamped:
- rainfall points: latest district condition/rain: LIGHT 5, MODERATE 12, HEAVY 22, EXTREME 30 (rain_mm ≥ 100 in 24 h counts as EXTREME); +5 if the 72 h total is also HEAVY or worse
- terrain: terrain_class × 3 (max 15) + landslide_prone +8 + flood_prone +8 (flood adds only when rainfall ≥ MODERATE; landslide only when rainfall ≥ LIGHT)
- open verified incidents within 15 km (haversine): severity × 4 each, max 25
- status: RESTRICTED +15; BLOCKED sets 100
- manual congestion note or CONGESTION incident on the segment: +10
- disruption history (RESOLVED incidents on the segment in the last 90 days): +2 each, max 8
Store `risk_factors` as a list of `{label, points}` so the UI can show "Why is this risky?". Categories: LOW < 35, MEDIUM 35–59, HIGH 60–79, CRITICAL ≥ 80.
Recompute for affected segments on: weather log added, incident created/verified/resolved, road status change, and every 10 minutes for all.

**Status rules:** a VERIFIED incident of severity 3 sets its segment RESTRICTED; severity 4 of type LANDSLIDE/FLOOD/BRIDGE_DAMAGE/ROAD_DAMAGE sets BLOCKED; resolving the incident restores the previous status (kept in `road_status_history`). Moderators/admins can set status manually with a reason. Every change is written to history and audit log and publishes `road.updated`.

**District connectivity score 0–100:** weighted share of the district's access segments (segments touching its nodes) that are usable: OPEN full weight, RESTRICTED 0.5, BLOCKED 0, then reduced by average risk × 0.3. Categories GOOD ≥ 75, DEGRADED 40–74, POOR < 40, INACCESSIBLE when all access segments are BLOCKED.

**Supply gap:** days_of_cover = stock_units / daily_demand_units; gap when < 3. **Bottleneck:** a district with a gap and connectivity POOR/INACCESSIBLE, or a segment used by ≥ 3 active routes with risk ≥ 60.

**Routing:** Dijkstra over OPEN and RESTRICTED segments (BLOCKED never used). Edge cost = travel_time_min × (1 + risk/100 × 1.5). Alternatives: re-run with the previous result's edges penalised ×3, dedupe identical paths, return up to 3 options each with `distanceKm, etaMinutes, riskScore, riskCategory, expectedDelayMin, reason, geometry, segmentIds`. **Delay estimate** = base_time × (risk/100 × 0.8) + 20 min if any RESTRICTED edge + 15 min if any HIGH+ edge. Reason is a plain sentence such as "Avoids NH-27 near Nagaon (blocked), adds 42 min."
**Emergency mode:** setting toggled by moderator/admin; planner uses only OPEN segments and weights risk ×4; map and dashboard prioritise hospitals and depots; banner for everyone; EMERGENCY shipments listed first.

**Shipment state machine:** DRAFT → ASSIGNED → IN_TRANSIT ⇄ DELAYED → DELIVERED; CANCELLED from any non-terminal state. Invalid transition → HTTP 409. Starting requires vehicle and driver. Delivering sets vehicle IDLE, delivered_at, adds weight to the destination district's stock for that cargo category, publishes `shipment.updated`.

### 4.4 Automation (idempotent; all alert creation goes through one `AlertService`)
- **Simulator** (`@Scheduled`, tick from `simulation.tick-ms`, default 3000, disabled when `simulation.enabled=false`): moves each IN_TRANSIT vehicle along its route geometry, stores position (history every 5th tick), updates progress and ETA, sets DELAYED when ETA exceeds plan by > 20 min, publishes `vehicle.update`. A vehicle with a real driver GPS fix newer than 30 s is not simulated. Reaching the end only marks the vehicle as arrived; the driver must press "Mark delivered".
- **Alert engine** (every 30 s and on relevant events), rules: segment BLOCKED → ROAD_BLOCKED (CRITICAL); district INACCESSIBLE → DISTRICT_INACCESSIBLE (CRITICAL); shipment DELAYED → DELIVERY_DELAYED (WARNING); segment risk ≥ 60 on an active route or serving a district → HIGH_RISK_CORRIDOR (WARNING); verified incident severity ≥ 3 → INCIDENT; emergency button → EMERGENCY (CRITICAL). De-duplicate with `open_key`; auto-resolve when the condition clears. Alert text templates exist in all four languages with parameters (road, district, minutes).
- **Notification fan-out:** for each alert, create notifications for relevant users in **their saved language**: all MODERATOR/ADMIN; plus drivers whose active route uses the segment; plus USERs in the affected state if they enabled alerts.
- **Reroute on road change:** when a segment becomes BLOCKED/RESTRICTED, find ASSIGNED/IN_TRANSIT shipments whose ACTIVE route uses it, compute alternatives, store the best as a PROPOSED route plan, create an alert, notify the driver and moderators. Driver accepts → plan becomes ACTIVE, old one SUPERSEDED, simulator follows new geometry.
- **Field report verified:** create or merge an incident (same type within 2 km and 6 h merges), write `incident_events`, apply status rules, recompute risk.

---

## PART 5. API (REST, JSON, under `/api`)
Error shape: `{timestamp,status,error,message,path,fieldErrors}` for 400/401/403/404/409/500. Lists: `?page&size&sort&q` + filters → `{content,page,size,totalElements,totalPages}`. Swagger UI at `/api/docs`.

- **Public:** `GET /health` (includes DB check) · `GET /public/overview` (counts only) · `GET /public/network` (nodes + segments with status + polyline; used by the landing page).
- **Auth:** `POST /auth/register|login|refresh|logout` · `GET|PATCH /auth/me` (name, language, alert prefs) · `POST /auth/change-password`.
- **Map:** `GET /map/segments|vehicles|incidents|locations|districts|weather` (bbox optional, lightweight DTOs).
- **Roads / blocked route status:** `GET /roads` (filters: status, state, asset type, risk) · `GET /roads/{id}` (history, risk factors) · `PATCH /roads/{id}/status` (MODERATOR+).
- **Routes:** `POST /routes/plan` `{originNodeId,destinationNodeId,vehicleType,priority}` · `GET /shipments/{id}/route-proposal` · `POST /shipments/{id}/route-proposal/accept|reject`.
- **Shipments (vehicle delivery status):** CRUD + `POST /shipments/{id}/assign|start|pause|deliver|cancel` (USER only on own shipment for start/deliver).
- **Vehicles:** CRUD (MODERATOR+) · `GET /vehicles/{id}/trail` · `POST /vehicles/me/position` (driver GPS; accepts a batch for offline catch-up).
- **Incidents + history logs:** list/detail/create/update/resolve · `GET /incidents/{id}/events` · `GET /incidents/history` (filterable timeline across incidents).
- **Field reports:** `POST /field-reports` (multipart `payload` JSON + `images[]`, idempotent by `clientId`; duplicate → 200 with `duplicate:true`) · `GET /field-reports` (USER own, MODERATOR+ all) · `POST /field-reports/{id}/verify|reject`.
- **Weather:** `GET /weather/districts` · `POST /weather/logs` (any authenticated) · `GET /weather/logs`.
- **Alerts / notifications:** `GET /alerts` · `POST /alerts/{id}/ack|resolve` (MODERATOR+) · `GET /notifications` · `POST /notifications/{id}/read` · `POST /notifications/read-all` · `POST /notifications/broadcast` (MODERATOR+, message with language variants; missing languages fall back to English).
- **Dashboards:** `GET /dashboard/summary` (8 KPIs) · `/dashboard/districts` · `/dashboard/supply` · `/dashboard/movement` · `/dashboard/emergency` · `GET /analytics/*` (SQL aggregations for charts).
- **Settings:** `GET|POST /settings/emergency` (MODERATOR+).
- **Search:** `GET /search?q=`.
- **Admin:** users (list/create/update role/activate/reset password) · `GET /admin/audit` · `GET|PATCH /admin/rules` · `GET|POST|DELETE /admin/api-keys` · `POST /admin/demo/rain` `{districtId, condition}` (adds a SEED-source weather log so risk and alerts visibly change) · `POST /admin/demo/reset`.
- **Integration ingest (header `X-API-Key`):** `POST /integrations/road-status`, `/integrations/weather`, `/integrations/assets` for external transport or government monitoring systems; examples in README.
- **Realtime:** `GET /realtime/stream`.

### 5.1 Authorization matrix (method security, tested)
| Area | USER | MODERATOR | ADMIN |
|---|---|---|---|
| Read map, dashboards, weather, planner | ✔ | ✔ | ✔ |
| Submit field reports, incidents, weather logs | ✔ | ✔ | ✔ |
| Driver actions on own shipment | ✔ | ✔ | ✔ |
| Verify/reject reports, resolve incidents | – | ✔ | ✔ |
| Road status, alerts ack/resolve, emergency mode | – | ✔ | ✔ |
| Shipments, vehicles, stock write, broadcast | – | ✔ | ✔ |
| Users, rules, API keys, audit, reference data, demo tools | – | – | ✔ |

---

## PART 6. DESIGN LANGUAGE: "Monsoon Light"

### 6.1 Intent
Bright, calm, trustworthy, a little regional: morning mist over hills and river valleys. Light surfaces, clear data, one confident brand colour. **No dark theme and no dark mode toggle.** No neon, glow, gradient cards, glassmorphism, purple, or emoji icons.

### 6.2 Tokens (`styles/tokens.css`, read by Tailwind)
```
--mist-50:#F7F9F8 page   --mist-100:#EEF3F1 sunken/table header   --mist-200:#DCE6E2 borders
--white:#FFFFFF surfaces
--ink-900:#13211D text   --ink-600:#4A5B56 secondary (≥4.5:1)   --ink-400:#7B8A85 hints
--river-600:#14597A BRAND (actions, links, selection, safe routes)   --river-700:#0F4660   --river-100:#DDEBF3
--tea-500:#2F8F5B open/safe   --tea-100:#E0F2E8
--marigold-500:#D98A00 restricted/high risk   --marigold-100:#FBEFD2
--rust-600:#C6403A blocked/critical   --rust-100:#FBE3E1
--sky-500:#2F7FD0 live/info
```
Brand colour = actions and selection only. Green/amber/red = status only, never decoration. White surfaces on `--mist-50`, 1 px `--mist-200` borders, soft shadow only on floating layers (`0 8px 24px rgb(19 33 29 / .10)`). Radius 10 px panels, 8 px controls. A very faint contour-line SVG (3% opacity) may sit behind the landing hero and empty states only.

### 6.3 Type
Headings and big numbers: **Bricolage Grotesque** 600/700. UI/body: **Manrope** 400–700. IDs, coordinates, timestamps in tables: **JetBrains Mono**. Hindi/Assamese/Bengali: Noto Sans Devanagari / Noto Sans Bengali. Scale 12/14/16/20/28/40/64; body 15 px desktop, 16 px mobile and Driver mode.
Forbidden: all-caps eyebrow labels, tracked-out caps, mono for ordinary labels, "A · B · C" meta strings, "→" on buttons, one highlighted word in a headline, numbered 01/02/03 markers unless the content is a real sequence.

### 6.4 Writing
Plain, specific, sentence case. Buttons name the action ("Block this road", "Send report", "Accept safer route"); confirmations repeat it ("Road blocked"). Errors say what happened and what to do. Operational lines read like "NH-27 near Nagaon is blocked: landslide, reported 14:32." No marketing filler.

### 6.5 Layout
- **App shell:** collapsible left sidebar (Overview, Operations, Field, Admin groups), top bar with search (Ctrl/⌘-K), language switcher, emergency indicator, notification bell with unread count, user menu. <1024 px: sidebar becomes a drawer; <768 px: bottom tab bar (Map, Shipments, Alerts, Report, Me).
- **Dashboard:** one KPI bar of 8 numbers (blocked roads, restricted roads, districts at risk, active shipments, delayed shipments, open alerts, supply gaps, on-time rate) with 24 h change; large map beside a live alert list; below, tabs: District connectivity · Bottlenecks and supply gaps · Emergency routes · Live movement. Not a grid of identical cards.
- **Live map (Live Map Overlay):** full-bleed map, floating collapsible layer panel, right detail sheet, bottom-left legend. Layers: road status, bridges, passes, risk (colour by category), vehicles, incidents, districts (circle by population band, colour by connectivity), hospitals/depots, weather conditions, active routes, proposed alternatives, verified reports.
- **Other screens:** Alerts (Instant Alert Notifications), Roads (Blocked Route Status), Incident History Logs (timeline with filters), Shipments (Vehicle Delivery Status), Vehicles, Route planner, Weather logs, Field report, Pending queue, Notifications, Settings, Admin pages.
- **Tables:** sticky header, 44 px rows, status as coloured dot + word, sortable, filter bar, pagination; stacked cards under 768 px.
- **Route styles (legend):** safe = solid river-blue 4 px; risky = marigold dashed; blocked = rust with ✕ at midpoint; alternative = tea-green with dash flow only while selected.
- **Vehicle marker:** rotated arrow by heading, colour by shipment status; emergency = pulsing red ring.

### 6.6 Landing page (public `/`)
1. **Hero:** headline "Know which roads are open before your convoy leaves." with one supporting sentence and buttons "Sign in" and "View the live map". Right side: an SVG of the **real NER road network** from `/api/public/network`, drawing itself in; then a looping scene: a vehicle dot travels a route, one segment turns red, the route redraws in green around it. Four live count-ups below (districts monitored, segments tracked, shipments moving, disruptions open) from `/api/public/overview`.
2. **How it works (scroll-driven, sticky SVG):** the six pipeline steps of 1.5 in plain words; each highlights part of the same SVG as you scroll.
3. **What you can do:** the objectives of 1.3 as a simple two-column list with line icons (not identical cards).
4. **Three roles:** User, Moderator, Admin.
5. **Final call:** sign-in button, demo accounts note, footer with language switcher.
Fully responsive; with reduced motion it shows the final static state.

### 6.7 Motion (smooth and purposeful)
- **Smooth scroll:** Lenis on the landing page and long in-app pages. Add `data-lenis-prevent` to maps and scrollable panels so zoom and inner scroll work. Anchor links scroll smoothly. Disable under `prefers-reduced-motion`.
- **Route transitions:** 200 ms fade + 8 px rise (`AnimatePresence`).
- **Scroll-linked:** hero SVG parallax layers, "How it works" driven by `useScroll`, thin progress line at the top of the landing page.
- **Orchestrated load:** hero headline lines reveal once, network draws via stroke-dashoffset, counters count up.
- **In-app:** list `layout` animations for arriving alerts and reordering rows, panel slide 160 ms, button press scale .98, toast spring, skeleton shimmer, KPI number tick on change, vehicle markers glide between positions, selected-route dash flow, one-time highlight fade on a new alert.
- **Do not** fade-slide every section, hover-lift every card, or add looping decoration other than the hero scene and live-status pulse. Animate only `transform` and `opacity`; throttle map updates to ≤ 4/s.

### 6.8 Driver mode (phone, one decision per screen)
For a USER with an active shipment. Type 16–20 px, buttons ≥ 56 px high at the bottom. Screens: Trip overview → Navigation (map, next road, ETA, risk badge) → Risk ahead (marigold banner: what happened, extra delay, buttons "Take safer route" / "Keep current route") → New route → Trip completed ("Mark delivered", optional photo). "Report emergency" is a hold-for-1.5-seconds red button with a confirmation sheet; it creates a DRIVER incident at the current GPS point and an EMERGENCY alert. Explain GPS permission in plain language and degrade gracefully when denied.

### 6.9 Accessibility
Contrast ≥ 4.5:1 text, ≥ 3:1 UI. Visible focus ring (2 px river-600, 2 px offset). Keyboard reachable; dialogs trap focus; `aria-live` for toasts and alerts. Status never by colour alone. Test at 390, 768, 1280, 1440 px: no horizontal scroll; tap targets ≥ 44 px.

---

## PART 7. OFFLINE, PWA, LANGUAGES
- **PWA:** `vite-plugin-pwa` (app shell precache, manifest, icons, install prompt). Never cache authenticated `/api` responses; runtime-cache only viewed map tiles (size-limited) and static assets.
- **Offline field reports:** the form works with no network. On submit save `{clientId (uuid), payload, images as Blobs, gps, capturedAt}` to IndexedDB, status "Waiting to sync". `syncEngine` flushes on the `online` event, every 15 s and on app start, with exponential backoff and per-item status (Waiting / Syncing / Synced / Failed with reason). If the refresh token has expired, keep the queue, prompt sign-in, then continue. The server's `clientId` uniqueness guarantees one record. A "Pending reports" panel offers Retry and Discard. Offline weather logs use the same queue.
- **Offline driver GPS:** positions are buffered and posted as a batch when the connection returns.
- **Offline reading:** keep the last dashboard summary and last 20 alerts in IndexedDB and show them under an "Offline. Last updated 14:32" banner.
- **Languages:** every user-visible string through `t()`. English complete; Hindi, Assamese and Bengali fully cover navigation, dashboard labels, alerts, notifications, report form, Driver mode and sign-in. A user's saved language decides the language of notifications generated for them. Check scripts render correctly in a screenshot.

---

## PART 8. SECURITY AND CLOUD READINESS
Bean Validation on every request DTO; JPA/parameterised queries only; CORS limited to configured origins; security headers; rate limits on login and ingest; upload checks (4 above); BCrypt passwords; API keys stored hashed and shown once; audit log for status changes, role changes, key creation, rule edits; all config via environment variables (`.env.example` complete, no secrets in git); JSON structured logs in `prod`; `/api/health` with DB check; multi-stage `Dockerfile` (build frontend, build jar, run on JRE 21) and `docker-compose.yml` (app + postgres:16 with healthchecks and volumes for data and uploads); README section on deploying to a VM or container service.

---

## PART 9. PHASES AND GATES
Each task ends with the verify-and-fix loop. A phase ends only after its **Gate** was executed and passed and `node scripts/verify.mjs` is green.

1. **Foundation.** Maven project with wrapper, Spring Boot app, profiles (dev/test/prod), Flyway baseline, `/api/health`; Vite + React + TS + Tailwind + tokens + fonts + Lenis + Framer Motion; scripts of 2.3; Playwright set up; `PROGRESS.md`.
   Gate: `node scripts/verify.mjs` passes on the empty app; page shows "Backend is up"; console clean; the built single jar serves the UI.
2. **Schema, seed, auth, first slice.** V1 schema, entities and repositories, `SeedService` (4.2), JWT auth with refresh rotation, role security, login/register pages, auth context, Axios refresh interceptor, `RoleGuard`, per-role sidebar, `/public/network` and `/public/overview`.
   Gate: tests for auth, refresh rotation and the authorization matrix · repository smoke test · graph-connected test · two distinct Guwahati→Imphal paths test · sign in as all three demo accounts in the browser.
3. **Landing page.** Section 6.6 with the real network animation, count-ups, Lenis scroll, scroll-linked sequence, responsive, reduced-motion fallback.
   Gate: screenshots at 1440 and 390 px reviewed; animation runs; numbers equal `/public/overview`; no layout shift; console clean.
4. **GIS dashboard, roads, blocked route status.** Live map with all layers, legend, detail sheet; roads page; moderator status change with reason; history and audit; SSE `road.updated`.
   Gate: every layer toggles without console errors · blocking a segment as moderator updates map colour, roads table and the blocked KPI without reload · USER receives 403 and sees no button.
5. **Weather and rule engine.** Manual weather logs, optional Open-Meteo with silent fallback, `RiskEngine` with factors, rules editor (admin), recompute triggers, risk layer, "Why is this risky?" panel, demo rain tool.
   Gate: `RiskEngineTest` (each factor, clamping, monotonic rainfall) · demo rain raises risk of nearby segments and creates a HIGH_RISK_CORRIDOR alert · works with the network disabled.
6. **Route planner.** Dijkstra + alternatives + delay estimate + reasons, planner page with option cards and styled polylines, emergency mode behaviour.
   Gate: planner test (block a segment ⇒ different path, no blocked edge used) · UI shows ≥ 2 options for Guwahati→Imphal · emergency mode excludes RESTRICTED edges.
7. **Vehicles, shipments, GPS tracking, delivery status.** CRUD, state machine, assignment, start/deliver, simulator, driver GPS endpoint and browser hook, vehicle drawer with trail, live markers.
   Gate: shipment workflow test (valid and invalid transitions) · Playwright dispatch flow (create → assign → start → vehicle moves → deliver → stock increases) · markers move within 10 s without refresh · `simulation.enabled=false` stops movement · posting a driver position moves the marker.
8. **Field reporting, offline sync, incident history.** Mobile-first report form (GPS, `capture="environment"` camera input, preview), IndexedDB queue and sync engine, moderator verification queue with photo viewer, incident merge, Incident History Logs page.
   Gate: idempotency test (same clientId twice = one row) · Playwright: offline → submit → "Waiting to sync" → online → "Synced" once → moderator verifies → incident, history events and map marker appear.
9. **Alert system, notifications, languages, full pipeline.** `AlertService`, rules, open_key dedupe and auto-resolve, fan-out in each user's language, bell and notifications page, broadcast, language switcher with hi/as/bn.
   Gate: alert tests (all rules, dedupe, auto-resolve) · switching language changes UI and the language of a new notification · **`pipeline.spec.ts` passes all six steps of 1.5 in one run** · alert reaches a second browser in under 3 s.
10. **Reroute and Driver mode.** Proposals on road change, risk screen, accept/reject, vehicle follows the new geometry, trip completion, emergency hold button.
   Gate: Playwright: block a road → driver sees proposal → accepts → vehicle follows the new route; emergency → critical alert → moderator acknowledges → resolves.
11. **Dashboards and analytics.** KPI bar, four dashboard views, supply page with moderator stock updates, emergency routes view, 8 SQL-aggregated charts with date/state/cargo filters.
   Gate: test proves KPIs equal direct SQL counts · delivering a shipment changes stock, movement stats and charts after refresh · grep finds no hardcoded chart data.
12. **Central management and integrations.** Users, rules, API keys, ingest endpoints, audit log page, edit districts and nodes, OpenAPI docs.
   Gate: non-admin gets 403 and a UI redirect · ingest with a valid key changes a segment status shown on the map; invalid key gets 401 · audit entries exist.
13. **Polish and hardening.** Search palette, PWA install and offline banner, skeleton/empty/error states everywhere, accessibility and responsive pass, code-splitting of map/analytics/admin, rate limits and headers.
   Gate: `console-clean.spec.ts` (every route × every role, fails on console errors, unexpected failed requests or horizontal overflow at 390 px) · keyboard-only run through sign-in → map → report.
14. **Docker, README, final verification.** Dockerfile, compose with PostgreSQL (run the whole test suite once against PostgreSQL via the `prod`-like profile if Docker is available; otherwise record that in PROGRESS.md), README (overview, features mapped to objectives 1–8, architecture and pipeline, rule engine explanation, run locally, run with Docker, env vars, demo accounts, API summary, offline sync, languages, testing, deployment, limitations).
   Gate: Part 10 fully ticked.

---

## PART 10. FINAL CHECKLIST (run, do not assume)
- [ ] Fresh clone: `node scripts/dev.mjs` works; `node scripts/build.mjs` then `java -jar backend/target/nerlogix.jar` serves API and UI; `docker compose up --build` works
- [ ] `node scripts/verify.mjs` fully green
- [ ] Each objective 1–8 is covered by at least one Playwright assertion
- [ ] The six-step pipeline test passes
- [ ] Each of the 3 roles: correct sidebar, forbidden pages redirect, forbidden APIs return 403
- [ ] Every route as every role: zero console errors, zero failed requests
- [ ] Blocking a road updates map, roads table, dashboard KPI, planner result, alert list and driver proposal live
- [ ] Offline report queued, synced exactly once; app opens offline with the last dashboard snapshot
- [ ] UI and a fresh notification switch between en/hi/as/bn
- [ ] Smooth scroll works on the landing page; map zoom and panels are not hijacked; reduced motion respected
- [ ] 390, 768, 1280, 1440 px: no horizontal overflow, no unreachable controls
- [ ] Design self-review: no dark surfaces, no all-caps labels, no fake numbers, colour used only for brand and status, no "AI" or "machine learning" wording
- [ ] No `TODO`, `alert(`, `System.out.println`, `console.log` leftovers, or large literal data arrays in `frontend/src`
- [ ] No secrets in the repo; `.env.example` complete

---

## PART 11. KNOWN PITFALLS
- **Spring:** broadcast SSE only after commit; `SseEmitter` timeout 0 or very long, remove emitters on completion/error/timeout; permit `/api/realtime/**` through the JWT filter with header auth; static-resource SPA fallback must forward non-`/api` paths to `index.html` without swallowing API 404s; enable `@EnableScheduling`; set Jackson to ISO-8601 dates; set `spring.servlet.multipart.max-file-size=5MB`; keep H2 console off.
- **Flyway/H2:** if a migration works on H2 but not PostgreSQL (or the reverse), simplify the SQL; never use vendor-specific types.
- **Without Lombok:** write explicit constructors/getters; records for DTOs; map entities to DTOs in services, never return entities from controllers.
- **Leaflet:** import `leaflet/dist/leaflet.css`; use SVG `divIcon` markers; give the container a height; call `invalidateSize` after panels open; update marker positions in place; React StrictMode must not create two maps.
- **Lenis** hijacks wheel events: mark maps and scroll panels `data-lenis-prevent`; destroy on unmount.
- **Vite dev proxy** must pass SSE through without buffering.
- **IndexedDB:** store photos as Blob plus metadata.
- **Camera and GPS** need HTTPS or `localhost`; explain phone testing in the README.
- **Time:** store UTC, display in `Asia/Kolkata`.
- **Windows:** use Node scripts and `mvnw.cmd` aware commands; avoid bash-only syntax.
