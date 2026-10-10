# NER-Logix Function Matrix

| Area | Function | USER | MODERATOR | ADMIN | Backend test | E2E test | Status |
|---|---|---|---|---|---|---|---|
| Health | `GET /api/health` | Yes | Yes | Yes | Yes | `health.spec.ts` | Working |
| Public | `GET /api/public/overview` | Yes | Yes | Yes | Yes | `landing.spec.ts` | Working |
| Public | `GET /api/public/network` | Yes | Yes | Yes | Yes | `landing.spec.ts` | Working |
| Public | `GET /api/public/config` | Yes | Yes | Yes | Yes | `planner.spec.ts` | Working |
| Auth | `POST /api/auth/register` | Yes | Yes | Yes | Yes | `auth.spec.ts` | Working |
| Auth | `POST /api/auth/login` | Yes | Yes | Yes | Yes | `auth.spec.ts` | Working |
| Auth | `POST /api/auth/refresh` | Yes | Yes | Yes | Yes | `auth.spec.ts` | Working |
| Auth | `POST /api/auth/logout` | Yes | Yes | Yes | Yes | `auth.spec.ts` | Working |
| Auth | `GET /api/auth/me` | Yes | Yes | Yes | Yes | `auth.spec.ts` | Working |
| Auth | `PATCH /api/auth/me` | Yes | Yes | Yes | Yes | `auth.spec.ts` | Working |
| Auth | `POST /api/auth/change-password` | Yes | Yes | Yes | Yes | `auth.spec.ts` | Working |
| Map | `GET /api/map/segments` | Yes | Yes | Yes | Yes | `gis-roads.spec.ts` | Working |
| Map | `GET /api/map/vehicles` | Yes | Yes | Yes | Yes | `gis-roads.spec.ts` | Working |
| Map | `GET /api/map/incidents` | Yes | Yes | Yes | Yes | `gis-roads.spec.ts` | Working |
| Map | `GET /api/map/locations` | Yes | Yes | Yes | Yes | `gis-roads.spec.ts` | Working |
| Map | `GET /api/map/districts` | Yes | Yes | Yes | Yes | `gis-roads.spec.ts` | Working |
| Map | `GET /api/map/weather` | Yes | Yes | Yes | Yes | `gis-roads.spec.ts` | Working |
| Districts | `PUT /api/districts/{id}/stock` | No | Yes | Yes | Yes | `dashboard.spec.ts` | Working |
| Roads | `GET /api/roads` | Yes | Yes | Yes | Yes | `gis-roads.spec.ts` | Working |
| Roads | `GET /api/roads/{id}` | Yes | Yes | Yes | Yes | `gis-roads.spec.ts` | Working |
| Roads | `PATCH /api/roads/{id}/status` | No | Yes | Yes | Yes | `gis-roads.spec.ts` | Working |
| Routes | `POST /api/routes/plan` | Yes | Yes | Yes | Yes | `planner.spec.ts` | Working |
| Shipments | `GET /api/shipments` | Yes (own) | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Shipments | `GET /api/shipments/{id}` | Yes (own) | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Shipments | `POST /api/shipments` | No | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Shipments | `PUT /api/shipments/{id}` | No | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Shipments | `DELETE /api/shipments/{id}` | No | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Shipments | `POST /api/shipments/{id}/assign` | No | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Shipments | `POST /api/shipments/{id}/start` | Driver | Yes | Yes | Yes | `driver.spec.ts` | Working |
| Shipments | `POST /api/shipments/{id}/pause` | No | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Shipments | `POST /api/shipments/{id}/deliver` | Driver | Yes | Yes | Yes | `driver.spec.ts` | Working |
| Shipments | `POST /api/shipments/{id}/cancel` | No | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Shipments | `GET /api/shipments/{id}/route-proposal` | Driver | Yes | Yes | Yes | `driver.spec.ts` | Working |
| Shipments | `POST /api/shipments/{id}/route-proposal/accept` | Driver | Yes | Yes | Yes | `driver.spec.ts` | Working |
| Shipments | `POST /api/shipments/{id}/route-proposal/reject` | Driver | Yes | Yes | Yes | `driver.spec.ts` | Working |
| Vehicles | `GET /api/vehicles` | Yes | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Vehicles | `GET /api/vehicles/{id}` | Yes | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Vehicles | `POST /api/vehicles` | No | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Vehicles | `PUT /api/vehicles/{id}` | No | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Vehicles | `DELETE /api/vehicles/{id}` | No | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Vehicles | `GET /api/vehicles/{id}/trail` | Yes | Yes | Yes | Yes | `shipments.spec.ts` | Working |
| Vehicles | `POST /api/vehicles/me/position` | Driver | Yes | Yes | Yes | `driver.spec.ts` | Working |
| Incidents | `GET /api/incidents` | Yes | Yes | Yes | Yes | `field-reports.spec.ts` | Working |
| Incidents | `GET /api/incidents/{id}` | Yes | Yes | Yes | Yes | `field-reports.spec.ts` | Working |
| Incidents | `POST /api/incidents` | Yes | Yes | Yes | Yes | `field-reports.spec.ts` | Working |
| Incidents | `PATCH /api/incidents/{id}` | No | Yes | Yes | Yes | `field-reports.spec.ts` | Working |
| Incidents | `POST /api/incidents/{id}/resolve` | No | Yes | Yes | Yes | `field-reports.spec.ts` | Working |
| Incidents | `GET /api/incidents/{id}/events` | Yes | Yes | Yes | Yes | `field-reports.spec.ts` | Working |
| Incidents | `GET /api/incidents/history` | Yes | Yes | Yes | Yes | `field-reports.spec.ts` | Working |
| Field Reports | `POST /api/field-reports` | Yes | Yes | Yes | Yes | `field-reports.spec.ts` | Working |
| Field Reports | `GET /api/field-reports` | Yes (own) | Yes | Yes | Yes | `field-reports.spec.ts` | Working |
| Field Reports | `POST /api/field-reports/{id}/verify` | No | Yes | Yes | Yes | `pipeline.spec.ts` | Working |
| Field Reports | `POST /api/field-reports/{id}/reject` | No | Yes | Yes | Yes | `pipeline.spec.ts` | Working |
| Uploads | `GET /api/uploads/{filename}` | Yes | Yes | Yes | Yes | `field-reports.spec.ts` | Working |
| Weather | `GET /api/weather/districts` | Yes | Yes | Yes | Yes | `weather-risk.spec.ts` | Working |
| Weather | `POST /api/weather/logs` | Yes | Yes | Yes | Yes | `weather-risk.spec.ts` | Working |
| Weather | `GET /api/weather/logs` | Yes | Yes | Yes | Yes | `weather-risk.spec.ts` | Working |
| Alerts | `GET /api/alerts` | Yes | Yes | Yes | Yes | `pipeline.spec.ts` | Working |
| Alerts | `POST /api/alerts/{id}/ack` | No | Yes | Yes | Yes | `pipeline.spec.ts` | Working |
| Alerts | `POST /api/alerts/{id}/resolve` | No | Yes | Yes | Yes | `pipeline.spec.ts` | Working |
| Notifications | `GET /api/notifications` | Yes | Yes | Yes | Yes | `pipeline.spec.ts` | Working |
| Notifications | `POST /api/notifications/{id}/read` | Yes | Yes | Yes | Yes | `pipeline.spec.ts` | Working |
| Notifications | `POST /api/notifications/read-all` | Yes | Yes | Yes | Yes | `pipeline.spec.ts` | Working |
| Notifications | `POST /api/notifications/broadcast` | No | Yes | Yes | Yes | `pipeline.spec.ts` | Working |
| Dashboard | `GET /api/dashboard/summary` | Yes | Yes | Yes | Yes | `dashboard.spec.ts` | Working |
| Dashboard | `GET /api/dashboard/districts` | Yes | Yes | Yes | Yes | `dashboard.spec.ts` | Working |
| Dashboard | `GET /api/dashboard/supply` | Yes | Yes | Yes | Yes | `dashboard.spec.ts` | Working |
| Dashboard | `GET /api/dashboard/movement` | Yes | Yes | Yes | Yes | `dashboard.spec.ts` | Working |
| Dashboard | `GET /api/dashboard/emergency` | Yes | Yes | Yes | Yes | `dashboard.spec.ts` | Working |
| Analytics | `GET /api/analytics/charts` | Yes | Yes | Yes | Yes | `dashboard.spec.ts` | Working |
| Settings | `GET /api/settings/emergency` | Yes | Yes | Yes | Yes | `dashboard.spec.ts` | Working |
| Settings | `POST /api/settings/emergency` | No | Yes | Yes | Yes | `dashboard.spec.ts` | Working |
| Search | `GET /api/search` | Yes | Yes | Yes | Yes | `dashboard.spec.ts` | Working |
| Admin | `GET /api/admin/users` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Admin | `POST /api/admin/users` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Admin | `PATCH /api/admin/users/{id}` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Admin | `POST /api/admin/users/{id}/reset-password` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Admin | `GET /api/admin/districts` | No | No | Yes | Yes | `roles.spec.ts` | Working |
| Admin | `PUT /api/admin/districts/{id}` | No | No | Yes | Yes | `roles.spec.ts` | Working |
| Admin | `GET /api/admin/roads` | No | No | Yes | Yes | `roles.spec.ts` | Working |
| Admin | `PUT /api/admin/roads/{id}` | No | No | Yes | Yes | `roles.spec.ts` | Working |
| Admin | `GET /api/admin/rules` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Admin | `PATCH /api/admin/rules` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Admin | `GET /api/admin/api-keys` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Admin | `POST /api/admin/api-keys` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Admin | `DELETE /api/admin/api-keys/{id}` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Admin | `GET /api/admin/audit` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Admin | `POST /api/admin/demo/rain` | No | No | Yes | Yes | `weather-risk.spec.ts` | Working |
| Admin | `POST /api/admin/demo/reset` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Admin | `POST /api/admin/roads/snap-geometry` | No | No | Yes | Yes | `admin.spec.ts` | Working |
| Integrations | `POST /api/integrations/road-status` | API Key | API Key | API Key | Yes | `admin.spec.ts` | Working |
| Integrations | `POST /api/integrations/weather` | API Key | API Key | API Key | Yes | `admin.spec.ts` | Working |
| Integrations | `POST /api/integrations/assets` | API Key | API Key | API Key | Yes | `admin.spec.ts` | Working |
| Realtime | `GET /api/realtime/stream` | Yes | Yes | Yes | Yes | `pipeline.spec.ts` | Working |
| UI Route | `/` Entry / Login Page | Yes | Yes | Yes | N/A | `auth.spec.ts` | Working |
| UI Route | `/about` Marketing Page | Yes | Yes | Yes | N/A | `landing.spec.ts` | Working |
| UI Route | `/login` Login Page | Yes | Yes | Yes | N/A | `auth.spec.ts` | Working |
| UI Route | `/register` Register Page | Yes | Yes | Yes | N/A | `auth.spec.ts` | Working |
| UI Route | `/dashboard` Dashboard | Yes | Yes | Yes | N/A | `dashboard.spec.ts` | Working |
| UI Route | `/map` Live Map | Yes | Yes | Yes | N/A | `gis-roads.spec.ts` | Working |
| UI Route | `/roads` Roads Page | No | Yes | Yes | N/A | `gis-roads.spec.ts` | Working |
| UI Route | `/weather` Weather Page | Yes | Yes | Yes | N/A | `weather-risk.spec.ts` | Working |
| UI Route | `/planner` Route Planner | Yes | Yes | Yes | N/A | `planner.spec.ts` | Working |
| UI Route | `/vehicles` Vehicles Page | No | Yes | Yes | N/A | `shipments.spec.ts` | Working |
| UI Route | `/shipments` Shipments Page | Yes (own) | Yes | Yes | N/A | `shipments.spec.ts` | Working |
| UI Route | `/field-report` Field Report Form | Yes | Yes | Yes | N/A | `field-reports.spec.ts` | Working |
| UI Route | `/pending-reports` Verification Queue | No | Yes | Yes | N/A | `field-reports.spec.ts` | Working |
| UI Route | `/incidents` Incidents Log | No | Yes | Yes | N/A | `field-reports.spec.ts` | Working |
| UI Route | `/alerts` Alert Center | Yes | Yes | Yes | N/A | `pipeline.spec.ts` | Working |
| UI Route | `/notifications` Notifications | Yes | Yes | Yes | N/A | `pipeline.spec.ts` | Working |
| UI Route | `/driver` Driver Mode | USER | No | No | N/A | `driver.spec.ts` | Working |
| UI Route | `/settings` Settings | Yes | Yes | Yes | N/A | `auth.spec.ts` | Working |
| UI Route | `/admin/users` User Management | No | No | Yes | N/A | `admin.spec.ts` | Working |
| UI Route | `/admin/network` Network Data | No | No | Yes | N/A | `roles.spec.ts` | Working |
| UI Route | `/admin/rules` Rule Configuration | No | No | Yes | N/A | `admin.spec.ts` | Working |
| UI Route | `/admin/api-keys` API Key Management | No | No | Yes | N/A | `admin.spec.ts` | Working |
| UI Route | `/admin/audit` Audit Logs | No | No | Yes | N/A | `admin.spec.ts` | Working |
| UI Route | `/admin/demo` Demo Tools | No | No | Yes | N/A | `weather-risk.spec.ts` | Working |
