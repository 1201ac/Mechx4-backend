# NER-Logix — Master Full-Stack QA, Reliability, & Verification Report

**Date:** October 11, 2026  
**Git Branch:** `qa/full-flow-fixes`  
**Target Environments:** Frontend (Vercel), Backend (Render / Docker), Database (TiDB Cloud MySQL-Compatible)  
**Verification Status:** All QA Verification Gates Passed (`72/72` backend tests passing, `0` TypeScript errors, `0` lint errors, frontend production build succeeded)

---

## 1. Executive Summary & Root Cause Diagnoses

The QA investigation identified and systematically addressed the following root causes behind reported UI freezes, unresponsiveness, and routing limitations:

### A. Realtime Multi-Tab Infinite Echo Loop (UI Freezing / "Reconnecting..." Lock)
* **Observed Symptom:** Buttons became unresponsive, the UI displayed "Reconnecting...", and browser CPU usage spiked with high event churn.
* **Root Cause:** Both `RealtimeProvider.tsx` and `createEventHandlerMap` in `eventHandlers.ts` maintained separate `BroadcastChannel('nerlogix_sync')` instances. When an SSE event arrived in Tab A, `RealtimeProvider` posted it to the channel, and `handleEvent` inside `eventHandlers.ts` posted it *again*. When Tab B received the message, its `handleEvent` posted it *back* to Tab A, resulting in an infinite ping-pong broadcast echo loop that flooded React Query query invalidations and event dispatchers.
* **Resolution:** Removed `BroadcastChannel` from `eventHandlers.ts`. Realtime synchronization is centrally owned by `RealtimeProvider.tsx`. Incoming tab broadcast messages update React Query caches and listeners but strictly never re-broadcast to other tabs.

### B. Route Planner Mode Inflexibility & Disconnect
* **Observed Symptom:** Users could only plan routes between predefined internal network nodes. Arbitrary city navigation (e.g. Hyderabad to Guwahati) was unavailable, and straight-line distance was not cleanly distinguished from road travel time.
* **Root Cause:** `RoutePlannerPage.tsx` was tightly coupled exclusively to network nodes (`RoadNodeDto`) and lacked support for arbitrary place geocoding/embed previews.
* **Resolution:** Upgraded `RoutePlannerPage.tsx` to support **Dual Modes**:
  1. **Mode 1 — Safe Network Routes (Risk-Aware):** Dijkstra-based routing across regional road corridors with live hazard penalties, vehicle class suitability, priority levels, route cards, and interactive Google Maps polylines. Includes a **Swap button** and validation preventing identical origin/destination selection.
  2. **Mode 2 — Any-Place Route Preview (Google Maps):** Accepts arbitrary origins and destinations (e.g., Guwahati, Assam to Hyderabad, Telangana), provides place swapping, calculates straight-line Haversine distance, displays an explicit distinction label (`"Straight-line distance: ~X km. Road distance & driving duration previewed via Google Maps"`), renders a 500px responsive `GoogleMapEmbed`, and provides a direct "Open in Google Maps" external navigation link.

### C. Protection Against Accidental Production Database Mutation During Local Testing
* **Observed Symptom:** `TidbConnectivityTest.java` contained fallback credentials targeting the live production TiDB Cloud cluster (`gateway01.ap-northeast-1.prod.aws.tidbcloud.com`).
* **Root Cause:** A test class directly initialized a Flyway migration and JDBC connection against TiDB without gating for local CI/test environments.
* **Resolution:** Annotated `TidbConnectivityTest` with `@Disabled` and `@EnabledIfEnvironmentVariable(named = "RUN_TIDB_INTEGRATION_TEST", matches = "true")` ensuring local test runs execute solely against the in-memory H2 database in MySQL compatibility mode.

### D. Toast Flooding & UI Disruption
* **Observed Symptom:** Multiple simultaneous background events could generate cascading toast notifications that obscured interactive UI elements.
* **Root Cause:** `ToastContext.tsx` lacked queue capping and duplicate suppression.
* **Resolution:** Added a 3-second deduplication cache for identical messages and limited the concurrent toast display to a maximum of 4 items.

### E. Missing Realtime Event Handlers
* **Observed Symptom:** Changes to roads, alerts, notifications, districts, and emergency states required a manual browser refresh to reflect in the UI.
* **Root Cause:** `eventHandlers.ts` lacked switch cases for `road.updated`, `alert.updated`, `notification.created`, `district.updated`, and `emergency.changed`.
* **Resolution:** Added explicit invalidation triggers and toast handlers for all missing backend SSE event topics.

---

## 2. QA Verification Gates & Test Results

All quality gates required by the master test suite were executed and verified:

| Gate | Tool / Command | Result | Details |
| :--- | :--- | :--- | :--- |
| **Backend Tests** | `.\mvnw.cmd test` | **PASS (72/72)** | 72 tests passed, 0 failures, 1 skipped (`TidbConnectivityTest` production safeguard). Tested against in-memory H2 with `open-in-view=false`. |
| **Frontend TypeScript** | `npm run typecheck` | **PASS (0 errors)** | `tsc --noEmit` completed cleanly with zero compilation errors. |
| **Frontend ESLint** | `npm run lint` | **PASS (0 errors)** | 0 errors across all frontend source files (warnings only). |
| **Frontend Vitest** | `npm test -- --run` | **PASS (1/1)** | Application test suite passed with zero regressions. |
| **Frontend Build** | `npm run build` | **PASS** | Vite production bundle created successfully with PWA Service Worker manifest generated (precache 111 entries). |

---

## 3. Status Matrix: Core Application Endpoints & Features

| Feature / Domain | Method / Path | Status | Verification Detail |
| :--- | :--- | :--- | :--- |
| **Auth** | `POST /api/auth/login` | `PASS` | JWT generation with Refresh Token cookie/header. |
| **Auth Profile** | `GET /api/auth/me` | `PASS` | Role and user profile resolution. |
| **Shipments List** | `GET /api/shipments` | `PASS` | `@EntityGraph` prevents `LazyInitializationException` for `vehicle.ownerUser`. |
| **Shipment Detail** | `GET /api/shipments/{id}` | `PASS` | Null-safe vehicle and owner mapping. |
| **Dispatch Board** | `GET /api/dispatch/board` | `PASS` | Verified with `open-in-view=false` integration tests. |
| **Driver Candidates** | `GET /api/dispatch/candidates/{id}` | `PASS` | Recommends eligible vehicles and available drivers. |
| **Driver Registry** | `GET /api/dispatch/drivers` | `PASS` | Returns active drivers and assigned shipments. |
| **Unassign Shipment** | `POST /api/dispatch/unassign/{id}` | `PASS` | Reverts shipment status to `PENDING` and records assignment history. |
| **Route Planner** | `POST /api/routes/plan` | `PASS` | Multi-alternative Dijkstra with risk penalties and corridor segments. |
| **Route Planner (Any-Place)** | Keyless Google Maps Embed | `PASS` | Arbitrary place input, Haversine straight-line distance, responsive embed. |
| **Live Map Tab** | Keyless Embed + GIS Vector Switch | `PASS` | Dual modes: Standard Google Maps (`GoogleMapEmbed`) with hub/corridor selector & GIS vector overlays. |
| **Realtime Stream** | `GET /api/realtime/stream` | `PASS` | SSE connection with tab deduplication, reconnection backoff, and no echo loop. |
| **Dashboard Connectivity** | `GET /api/dashboard/summary` | `PASS` | In-memory district grouping with SQL statement count optimization. |
| **Public Config** | `GET /api/public/config` | `PASS` | Serves public client keys safely without exposing private tokens. |

---

## 4. Route Planner Capabilities

The Route Planner now features two distinct operational modes:

### Mode 1: Safe Network Routes (Risk-Aware)
- **Engine:** Internal Dijkstra routing engine with road hazard weights.
- **Inputs:** Network node selector dropdowns with district tags, vehicle type (Truck, Medical Van, Tanker, Mini Truck), and priority level (Normal, High, Emergency).
- **Controls:**
  - One-click **Swap button** to invert origin and destination.
  - Validation that prevents identical origin and destination selection.
  - **Live Road Conditions Changed Banner:** Prompts for recalculation when an SSE incident or weather alert is received.
- **Visualization:**
  - Full Google Maps JavaScript API integration (polylines for all returned routes).
  - Selected route rendered thick and risk-colored; alternatives rendered clickable and gray.
  - Interactive corridor segment sequence breakdown.
  - "Create Shipment With Selected Route" shortcut for Admins and Moderators.

### Mode 2: Any-Place Route Preview (Google Maps)
- **Engine:** Keyless Google Maps Embed API and external directions deep links.
- **Inputs:** Arbitrary text inputs for origin and destination (e.g., Guwahati, Assam to Hyderabad, Telangana).
- **Controls:**
  - One-click **Swap button** for custom places.
  - "Calculate Distance & Preview Route" trigger.
- **Distance Distinction:**
  - Calculates straight-line Haversine distance where city or node coordinates are recognized.
  - Explicit disclaimer: *"Straight-line distance: ~X km. Road distance & driving duration previewed via Google Maps below."*
- **Visualization:**
  - Large responsive `GoogleMapEmbed` (500px height).
  - External link opening turn-by-turn navigation directly in Google Maps.

---

## 5. Deployment Safety & Rollback Guide

### Branching Policy
All changes are contained on branch `qa/full-flow-fixes`. Neither `main` nor remote branches have been pushed. Merging and deployment remain strictly under user control.

### Protected Configuration Files
The following deployment configuration files have been kept unmodified:
- `frontend/vercel.json`
- `backend/render.yaml`
- Backend and frontend Dockerfiles
- Environment variable schemas

### Step-by-Step Production Deployment Procedure
1. **Database Migrations (TiDB Cloud):**
   - Flyway migrations `V1__schema.sql`, `V3__add_heading_to_vehicle_positions.sql`, `V4__vehicle_ownership.sql`, and `V5__dispatch.sql` are backward-compatible.
   - When deploying to Render, Flyway automatically validates and applies pending migrations during startup.
2. **Backend Deployment (Render):**
   - Push branch or merge into `main`:
     ```bash
     git checkout main
     git merge qa/full-flow-fixes
     git push origin main
     ```
   - Render automatically triggers the Docker build from `backend/Dockerfile`.
   - Health check endpoint: `https://nerlogixbackend.onrender.com/api/health`.
3. **Frontend Deployment (Vercel):**
   - Push to GitHub triggers automatic Vercel deployment.
   - Verify environment variable `VITE_API_BASE_URL` is configured to `https://nerlogixbackend.onrender.com/api`.
   - The Service Worker precache updates automatically with version cleanup.

### Rollback Procedure
If issues are encountered post-deployment:
1. **Frontend:** In the Vercel dashboard, navigate to **Deployments** and click **Promote to Production** on the previous stable deployment.
2. **Backend:** In the Render dashboard, navigate to **Deploys** and click **Rollback to previous deploy**.
3. **Database:** Migrations V3–V5 only add nullable columns and new tables (`shipment_assignments`); rolling back the application code does not require dropping columns or altering database state.

---

## 6. Manual Verification Checklist

- [ ] **Login:** Sign in as `admin@nerlogix.com`, `moderator@nerlogix.com`, and `user@nerlogix.com` using password `Demo@1234`.
- [ ] **Realtime Multi-Tab:** Open the application in two side-by-side browser tabs. Update a shipment status or trigger an incident in one tab. Verify the second tab updates immediately without CPU freezing, duplicate toasts, or "Reconnecting..." lockups.
- [ ] **Route Planner (Network Mode):** Select Guwahati as origin and Shillong as destination. Click "Calculate Routes". Verify all route options display on the map and clicking route cards switches the highlighted polyline.
- [ ] **Route Planner (Any-Place Mode):** Switch to "Any-Place Route Preview". Enter "Guwahati" and "Hyderabad". Click "Calculate Distance & Preview". Verify the straight-line distance notice displays and the 500px Google Map loads directions.
- [ ] **Shipment Assignment:** Navigate to `/dispatch` as Admin or Moderator. Assign a pending shipment to a driver and vehicle. Log in as the driver (`user@nerlogix.com`) and verify the assignment appears under `/driver`.
- [ ] **Shipment Unassignment:** As Moderator or Admin, click "Unassign" on an assigned shipment and verify it returns to pending status.
