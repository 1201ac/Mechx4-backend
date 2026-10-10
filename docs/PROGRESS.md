# NER-Logix Progress

## Decisions
- Using Java 21 with Spring Boot 3.3.4, Maven Wrapper.
- Using H2 in PostgreSQL compatibility mode for dev/test, PostgreSQL for prod.
- Frontend: Vite + React 18 + TS + Tailwind CSS + TanStack Query + React Router + Lenis + Lucide Icons + Leaflet.
- Baseline scripts check (`check.mjs`) and verification (`verify.mjs`) ran cleanly before audit fixes.
- Created `docs/FUNCTION_MATRIX.md` tracking all backend endpoints and UI routes/components against role authorization and test coverage.
- WP1 completed: replaced Maven Wrapper, added flyway postgres dependency, configured docker-compose with postgres 16 and env vars, cleaned tracked static files and temp DBs, created .env.example, added code splitting (manualChunks + React.lazy), added clean script.
- WP2 completed: fixed shipment route plan auto-generation (C1), simulator driver fix & ETA recovery (C2, C16), authenticated upload controller & auth image hook (C3), file upload magic byte validation (C4), dynamic cached RuleConfigService (C5), spec-compliant risk engine & 10min auto-recompute (C6, C7), status history restoration & 409 duplicate resolve guards (C8), field report status updates (C9), field report idempotent duplicate handling (C10), AFTER_COMMIT transactional SSE event stream (C11), driver-only shipment filter (C12), admin self-demotion/deactivation conflict guards (C15), UTF-8 token hashing (C18).
- WP3 completed: ApiKeyAuthenticationFilter for X-API-Key (S1), query-param token support removed from JwtAuthenticationFilter (S2), LoginRateLimiter enforcing 5 max failed attempts per 15 min per IP returning 429 (S3), refresh token family rotation & reuse detection returning 401 (S4), CORS policy, CSP header & security headers with 401 entry point (S5), password policy enforcing 8+ chars with upper/lower/digit/special (S6), deactivated user check in JwtAuthenticationFilter returning 401 (S7), 500 error sanitization and ResponseStatusException handling (S8).
- WP4 completed: search endpoint GET /api/search?q= (M1), weather log listing GET /api/weather/logs (M2), movement & emergency dashboards GET /api/dashboard/movement & /api/dashboard/emergency (M3), emergency mode settings GET|POST /api/settings/emergency (M4), notification broadcast POST /api/notifications/broadcast (M5), audit log service & paged GET /api/admin/audit (M6), road snap geometry POST /api/admin/roads/snap-geometry (M7).
- WP5 completed: backend public config endpoint GET /api/public/config for Google Maps API key (G1), installed @googlemaps/js-api-loader, created GoogleDirectionsMap component and googleMapsLoader utility (G2), updated RoutePlannerPage to offer native Google Maps directions with fallback and step-by-step route cards, updated CSP headers for google maps domains.
- WP6 completed: audited role access controls (F1), connected SSE realtime stream via useRealtimeStream with live status indicator (F2), created SettingsPage (/settings) and OfflineBanner component (F3), handled multilingual JSON titles/messages in alerts & notifications (F4), integrated Lenis smooth scrolling (F5).
- WP7 completed: created MethodSecurityTest testing authorization matrix for USER, MODERATOR, ADMIN roles (T1), enhanced e2e/pipeline.spec.ts driving all 6 operational pipeline steps with explicit assertions (T2), audited every row of docs/FUNCTION_MATRIX.md to reflect Working status across all endpoints and UI routes (T3).
- WP8 completed: updated Dockerfile and docker-compose.yml for production execution (D1, D2), updated README.md with complete env vars table, security notes, production docker setup, and ingest API examples (D3), ran full final verification pass with check.mjs and verify.mjs (16/16 E2E tests passing) (D4).
- Task 1 completed: role-based sidebar menu filtering & route protection, Driver Mode empty states, stock update modal & backend API (`PUT /api/districts/{id}/stock`), Admin Network Data page (`/admin/network`), unread notification badge & SSE handler, language switcher i18n support, role-specific dashboard views, resolved 35 missing Tailwind color tokens, added `GET /api/public/config`, updated matrix in `docs/FUNCTION_MATRIX.md`, and created `e2e/roles.spec.ts`.
- Task 2 completed: purged Leaflet/CARTO maps from `RoutePlannerPage.tsx`, updated `GoogleDirectionsMap.tsx` with risk polyline overlay, step-by-step driving directions, embed fallback for missing keys, Google Maps loader singleton fix (`googleMapsLoader.ts`), updated `e2e/planner.spec.ts` asserting 0 `.leaflet-container` elements.
- Task 3 completed: created `AuthShell.tsx` (split-screen visual shell, dark sidebar, terrain polyline SVG, branded motto), `Login.tsx` (role selector tiles, `zod` form validation, show/hide password, splash screen animation), `Register.tsx` (role selection radio tiles, matching visual shell), updated `App.tsx` routing (`/` renders `Login`, authenticated users redirect to role home, public marketing page moved to `/about`), updated `e2e/auth.spec.ts`, updated `docs/FUNCTION_MATRIX.md`, verified 100% green on `check.mjs` and `verify.mjs` (20/20 E2E tests passing).

## Blockers
- None.

## Work Packages
- [x] Step A: Baseline & Function Matrix
- [x] WP1: Build, tooling and repository hygiene
- [x] WP2: Core correctness bugs
- [x] WP3: Security
- [x] WP4: Missing features
- [x] WP5: Route planning directly in Google Maps
- [x] WP6: Frontend per-role, realtime, offline, i18n, design
- [x] WP7: Proof: tests and audits
- [x] WP8: Docker, docs, final verification
- [x] Task 1: Role audit, navigation controls, stock update & admin network page
- [x] Task 2: Google-Maps-Only Route Planner
- [x] Task 3: Designer login experience, split-screen auth shell & role home routing

## Notes
- All tasks fully completed and verified with check.mjs and verify.mjs (20/20 Playwright E2E tests passing 100% green).
