# IoTeka Cloud Platform

> A multi-tenant IoT data ingestion, storage, and visualization platform built for Africa â€” starting with Kenya, designed to scale continent-wide.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [System Architecture](#2-system-architecture)
3. [Technology Stack](#3-technology-stack)
4. [Service Breakdown](#4-service-breakdown)
5. [Data Model & Database Schema](#5-data-model--database-schema)
6. [API Key Design](#6-api-key-design)
7. [Ingestion Pipeline](#7-ingestion-pipeline)
8. [Device Connection Methods](#8-device-connection-methods)
9. [Project Configuration Options](#9-project-configuration-options)
10. [Quota & Free Tier System](#10-quota--free-tier-system)
11. [Dashboard & Visualizations](#11-dashboard--visualizations)
12. [Admin Panel](#12-admin-panel)
13. [Containerization & Deployment](#13-containerization--deployment)
14. [Directory Structure](#14-directory-structure)
15. [Environment Variables](#15-environment-variables)
16. [Development Setup](#16-development-setup)
17. [Roadmap](#17-roadmap)

---

## 1. Project Overview

IoTeca is a cloud IoT data platform that allows developers, researchers, and businesses across Africa to:

- Register an account and create IoT projects
- Connect sensors, microcontrollers, and applications using their protocol of choice (MQTT, HTTP, WebSocket, CoAP)
- Stream sensor data to the cloud in real time
- Visualize data via an interactive dashboard with time-series graphs, device maps, and custom metrics
- Receive quota alerts and subscribe to paid tiers as their project grows

The platform is built around the principle that IoT data is fundamentally different from general application data: it is high-frequency, small-payload, time-indexed, and comes from many sources simultaneously. Every architectural decision reflects this.

### Target Users

- IoT developers building smart home, agriculture, logistics, or environmental monitoring projects in Kenya and across Africa
- Academic researchers needing a simple cloud sink for sensor data
- Businesses deploying fleets of connected devices

### Core Design Principles

- **Speed over everything else at the ingestion layer** â€” a device should never have to wait
- **Multi-tenancy with hard isolation** â€” one project's load must never affect another
- **Free tier that is genuinely useful** â€” enough to prototype and demo, with a clear, transparent upgrade path
- **Self-hosted infrastructure** â€” no dependency on third-party IoT managed services; full control over auth, data, and cost

---

## 2. System Architecture

```
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                         DEVICE LAYER                                â”‚
â”‚         MQTT Â· HTTP REST Â· WebSocket Â· CoAP Â· SDK (Python/JS)       â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                           â”‚ API Key in connection credentials
                           â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                    EMQX BROKER (self-hosted)                         â”‚
â”‚    Auth webhook â†’ Java Auth Service Â· Topic routing Â· TLS           â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                           â”‚ validated messages
                           â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                   APACHE KAFKA (message queue)                       â”‚
â”‚        topic: raw.ingest.{region} Â· backpressure buffer             â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
         â”‚                  â”‚                       â”‚
         â–¼                  â–¼                       â–¼
  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
  â”‚  Ingestion  â”‚   â”‚ Alert Engine â”‚   â”‚   Quota Counter     â”‚
  â”‚   Service   â”‚   â”‚    Service   â”‚   â”‚      Service        â”‚
  â”‚   (Java)    â”‚   â”‚    (Java)    â”‚   â”‚   (Java + Redis)    â”‚
  â””â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”˜   â””â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”˜   â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
         â”‚                  â”‚                     â”‚
         â–¼                  â–¼                     â–¼
  â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”   â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
  â”‚TimescaleDB  â”‚   â”‚  PostgreSQL  â”‚   â”‚       Redis         â”‚
  â”‚ (sensor     â”‚   â”‚  (relational â”‚   â”‚  (rate limits,      â”‚
  â”‚  hypertable)â”‚   â”‚   metadata)  â”‚   â”‚   counters, cache)  â”‚
  â””â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”˜   â””â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”˜   â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
         â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”´â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                           â”‚
                           â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚              API GATEWAY  (Java Spring Boot)                         â”‚
â”‚  /ingest Â· /query Â· /projects Â· /devices Â· /auth Â· /admin Â· /keys   â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”¬â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
                           â”‚
                           â–¼
â”Œâ”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”
â”‚                 FRONTEND (Vue.js Â· Options API)                      â”‚
â”‚     Dashboard Â· Device Map Â· Project Manager Â· Admin Panel          â”‚
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”˜
```

All services are containerized. Each box above is a separate Docker container, orchestrated via Docker Compose.

---

## 3. Technology Stack

| Layer | Technology | Reason |
|---|---|---|
| MQTT Broker | EMQX (self-hosted, open-source) | Clusterable, high-connection-count, built-in auth webhook, Kafka bridge |
| Message Queue | Apache Kafka | Durable log, replay capability, decouples ingest from storage speed |
| Time-series DB | TimescaleDB (PostgreSQL extension) | SQL interface, hypertables, continuous aggregates, single DB for relational + time-series |
| Relational DB | PostgreSQL (via TimescaleDB instance) | Users, projects, API keys, billing, device registry |
| Cache / Counters | Redis | Atomic rate-limit counters, session cache, real-time quota tracking |
| Backend Services | Java (Spring Boot) | Microservices, strong typing, good Kafka client ecosystem |
| Scripting / QA | Python | Automation scripts, load testing, data migration utilities |
| Frontend | Vue.js (Options API) | Reactive dashboard, familiar and stable API surface |
| Charting | Apache ECharts | Handles large time-series datasets efficiently in browser |
| Device Map | Leaflet.js | Open-source, lightweight, no API key required |
| Containerization | Docker + Docker Compose | Dev parity, service isolation, production orchestration |
| Reverse Proxy | Nginx | TLS termination, rate limiting at edge, static frontend serving |

---

## 4. Service Breakdown

The platform is composed of the following independent microservices. Each runs in its own container and communicates over the internal Docker network (`IoTeca-internal`).

---

### 4.1 `auth-service` â€” Port 8081

**Package:** `io.ioteca.auth` Â· **Tech:** Spring Boot 3.2, Spring Security 6, JJWT 0.12

**Responsibilities**

| Concern | Mechanism |
|---|---|
| User registration | Validates email uniqueness, bcrypt-hashes password, assigns FREE tier subscription |
| Login | Verifies password against bcrypt hash, issues access + refresh JWT pair |
| JWT issuance | HS256, signed with `JWT_SECRET`; access token 60 min, refresh token 30 days |
| Token refresh | Validates refresh token `type` claim, issues new access token without re-login |
| API key generation | Produces `IoTeca_{env}_{project_short_id}_{20char_secret}`, stores SHA-256 hash only |
| API key revocation | Sets `is_active=false`, stamps `revoked_at`, records reason |
| EMQX auth webhook | Validates API key hash + project ownership on every device connect attempt |
| EMQX ACL webhook | Enforces topic-level publish rights: device may only publish to `IoTeca/{its_project_id}/#` |

**REST Endpoints**

```
POST /api/v1/auth/register          â€” create account, returns JWT pair
POST /api/v1/auth/login             â€” returns JWT pair
POST /api/v1/auth/refresh           â€” exchange refresh token for new access token
GET  /api/v1/auth/me                â€” current user profile [JWT required]
POST /api/v1/keys                   â€” create API key [JWT required]
GET  /api/v1/keys?projectId={uuid}  â€” list project's API keys [JWT required]
DELETE /api/v1/keys/{id}            â€” revoke key [JWT required]
POST /api/internal/emqx/auth        â€” EMQX auth webhook (internal network only)
POST /api/internal/emqx/acl         â€” EMQX ACL webhook (internal network only)
GET  /api/internal/emqx/key/validate?key={k} â€” key validation for other services
```

**Key implementation details**

- `JwtService` â€” issues and validates tokens locally; shared `JWT_SECRET` allows api-gateway to validate without calling auth-service on every request
- `ApiKeyService` â€” API key format: `IoTeca_{live|test}_{8-char project id}_{20-char URL-safe base64 secret}`; first 20 chars stored as `key_prefix` for UI display, rest stored only as `SHA-256(full_key)`
- `JwtAuthenticationFilter` â€” extracts `Bearer` token per request, sets Spring Security context
- Security: `/api/internal/**` and `/api/v1/auth/**` are permit-all at the filter chain; all other routes require a valid, non-refresh JWT

---

### 4.2 `ingestion-service` â€” Port 8082

**Package:** `io.ioteca.ingestion` Â· **Tech:** Spring Boot 3.2, Spring Kafka, Spring Data JPA

**Responsibilities**

| Concern | Mechanism |
|---|---|
| Consume raw messages | Kafka listener on `raw.ingest.af-ke-1`, concurrency=3 |
| Payload validation | Rejects messages missing `device_id` or empty `metrics` map; bad messages are acked immediately (no retry) |
| Narrow-row storage | Each metric key in the payload becomes a separate row in `tsdata.sensor_data` (wide payloads â†’ multiple rows) |
| Timestamp handling | Uses device-supplied `timestamp` (epoch ms) if present; falls back to server ingestion time |
| Device auto-registration | First message from an unknown `device_id` inserts a new row in `platform.devices`; subsequent messages update `last_seen_at` |
| Downstream fan-out | After successful write, emits `ProcessedMessage` to `processed.ingest.af-ke-1` for alert-engine and other consumers |

**Data flow**

```
Kafka raw.ingest.af-ke-1
  â†’ RawIngestConsumer (concurrency 3)
    â†’ IngestionService.ingest()
      â†’ SensorDataRepository.insertRaw()   â†’ tsdata.sensor_data
      â†’ DeviceRepository.touchLastSeen()   â†’ platform.devices
    â†’ KafkaTemplate.send(processed.ingest.af-ke-1)
```

**Key implementation details**

- `SensorData` entity uses a composite `@IdClass` of `(time, projectId, deviceId, metric)` matching the hypertable's natural key
- `insertRaw()` uses a native SQL upsert with `ON CONFLICT DO NOTHING` to safely handle duplicate delivery
- Project ID is resolved from: (1) `project_id` field in the JSON body (set by api-gateway for HTTP ingestion), or (2) the Kafka message key (set by the EMQX-Kafka bridge for MQTT ingestion)

---

### 4.3 `alert-engine` â€” Port 8083

**Package:** `io.ioteca.alert` Â· **Tech:** Spring Boot 3.2, Spring Kafka, Spring Data JPA

**Responsibilities**

| Concern | Mechanism |
|---|---|
| Consume processed messages | Kafka listener on `processed.ingest.af-ke-1`, concurrency=2 |
| Rule lookup | For each metric in the message, queries `platform.alert_rules` matching project + metric + device (or all devices) |
| Condition evaluation | Supports: `gt`, `gte`, `lt`, `lte`, `eq`; non-numeric metric values are skipped |
| Suppression | Skips firing if `last_fired_at + suppression_window_s > now()` â€” prevents alert flooding |
| Alert event emission | Writes `AlertEvent` to Kafka topic `alert.events`; notification-service handles delivery |

**Supported conditions**

| Condition | Description |
|---|---|
| `gt` | Fires when value > threshold |
| `gte` | Fires when value â‰¥ threshold |
| `lt` | Fires when value < threshold |
| `lte` | Fires when value â‰¤ threshold |
| `eq` | Fires when value == threshold |
| `absence` | Planned â€” fires when no message received within `absence_window_s` |

**Key implementation details**

- `AlertEvaluator` stamps `last_fired_at` on the rule before emitting the event (within the same transaction), preventing race conditions in concurrent consumers
- Rules with `device_id = NULL` apply to all devices in the project; device-specific rules take precedence
- Evaluation errors (bad JSON, DB failure) are caught and acked â€” they don't block the pipeline

---

### 4.4 `quota-service` â€” Port 8084

**Package:** `io.ioteca.quota` Â· **Tech:** Spring Boot 3.2, Spring Kafka, Spring Data Redis, Spring Data JPA

**Responsibilities**

| Concern | Mechanism |
|---|---|
| Message counting | O(1) atomic `INCR` on Redis key `project:{id}:msgs:{YYYY-MM-DD}` |
| Counter TTL | Set to 172,800 s (2 days) on first write; resets automatically at midnight |
| Tier limit lookup | Joins `user_subscriptions â†’ subscription_tiers` to find the active tier's `max_messages_per_day` |
| Quota events | Publishes to `quota.events` when count crosses 80% (`WARNING_80`) or 100% (`EXCEEDED`) of limit |
| Unlimited tiers | `max_messages_per_day = -1` short-circuits all checks â€” Enterprise tier never throttled |

**Redis key design**

```
project:{uuid}:msgs:2025-08-15   â†’   atomic counter, TTL 2 days
```

**Key implementation details**

- Quota-service and ingestion-service both consume `raw.ingest.af-ke-1` with **different consumer group IDs** â€” each receives every message independently
- Quota checks happen after the message is already written to TimescaleDB; enforcement at 100% is done at the broker level (EMQX rate limiter) and at the api-gateway (HTTP 429)
- `QuotaStatus` enum: `OK` | `WARNING_80` | `EXCEEDED`

---

### 4.5 `api-gateway` â€” Port 8080 (external-facing)

**Package:** `io.ioteca.gateway` Â· **Tech:** Spring Boot 3.2, Spring Security 6, Spring Kafka, Spring Data Redis, JJWT

**Responsibilities**

| Concern | Mechanism |
|---|---|
| JWT validation | Validates `Bearer` tokens locally using shared `JWT_SECRET` â€” no auth-service call per request |
| Rate limiting | Redis token-bucket per user: `ratelimit:{userId}:api` incremented per request, TTL 1 min |
| HTTP ingestion | Validates JWT, adds `project_id` to payload, publishes to `raw.ingest.af-ke-1` |
| Batch ingestion | Accepts up to 100 readings per POST; each published as an individual Kafka message |
| Project management | Full CRUD for `platform.projects`, scoped to the authenticated user |
| Device management | Read + patch for `platform.devices`, scoped to project ownership |
| Auth pass-through | `/api/v1/auth/**` is permit-all; clients call auth-service endpoints proxied through here |

**REST Endpoints**

```
POST /api/v1/ingest                         â€” single reading [JWT or API key]
POST /api/v1/ingest/batch                   â€” up to 100 readings [JWT or API key]
GET  /api/v1/projects                       â€” list user projects [JWT]
POST /api/v1/projects                       â€” create project [JWT]
GET  /api/v1/projects/{id}                  â€” get project [JWT]
PUT  /api/v1/projects/{id}                  â€” update project [JWT]
DELETE /api/v1/projects/{id}                â€” soft-delete project [JWT]
GET  /api/v1/projects/{id}/devices          â€” list devices [JWT]
GET  /api/v1/projects/{id}/devices/{did}    â€” get device [JWT]
PATCH /api/v1/projects/{id}/devices/{did}   â€” update device metadata [JWT]
GET  /api/v1/admin/**                       â€” admin endpoints [ROLE_ADMIN]
```

**Key implementation details**

- `JwtAuthenticationFilter` mirrors the one in auth-service â€” same secret, same validation logic, no cross-service call
- Project soft-delete sets `is_active=false` rather than deleting rows, preserving TimescaleDB data for the retention window
- `IngestController` resolves project ID from the JWT subject (user â†’ project lookup) for HTTP ingestion; the Kafka message key is set to `project_id` so ingestion-service can correlate it

---

### 4.6 `notification-service` â€” Port 8085

**Package:** `io.ioteca.notification` Â· **Tech:** Spring Boot 3.2, Spring Kafka, Spring Mail, Spring Data JPA

**Responsibilities**

| Concern | Mechanism |
|---|---|
| Quota warnings | Consumes `quota.events`; sends email at `WARNING_80` and `EXCEEDED` |
| Alert notifications | Consumes `alert.events`; delivers via email and/or webhook per rule's `notification_channels` |
| Deduplication | Checks `notification_log` for same `(project_id, type)` within configurable window before sending |
| Audit trail | Every send attempt (success or failure) is written to `platform.notification_log` |
| Webhook delivery | HTTP POST to `project.alert_webhook_url` with full alert event payload |

**Email templates**

| Type | Trigger | Content |
|---|---|---|
| `QUOTA_WARNING` | 80% of daily limit consumed | Usage count, limit, upgrade link |
| `QUOTA_EXCEEDED` | 100% of daily limit consumed | Rejection notice, upgrade link |
| `ALERT` | Alert rule fires | Device, metric, value, condition, rule ID |

**Key implementation details**

- Recipient email priority: `project.alert_email` â†’ project owner's `users.email`
- All Kafka consumption errors are caught and acked â€” a failed notification does not stall the pipeline
- `EmailService.wasSentRecently()` prevents duplicate emails within a 60-minute window for the same project + type
- SMTP configuration maps to standard Spring Mail properties via env vars (`SMTP_HOST`, `SMTP_PORT`, `SMTP_USER`, `SMTP_PASSWORD`)

---

### 4.7 `frontend` (Vue.js)

Single-page application served by Nginx:
- User authentication flows (login, register, password reset)
- Project creation and management
- Per-project dashboard with time-series charts and device status
- Device registration and API key management
- Admin panel (accessible only to `ROLE_ADMIN` users)

---

## 5. Data Model & Database Schema

### Design Decisions

The platform uses a **single TimescaleDB instance** (PostgreSQL with the TimescaleDB extension) for both relational metadata and time-series sensor data. This avoids operating two separate database systems early on. The relational tables live in a `platform` schema; the time-series data lives in a `tsdata` schema.

Multi-tenancy is implemented using **Model A: shared table with `project_id` as the tenant discriminator**, with a composite index on `(project_id, time DESC)` to ensure per-tenant queries never scan other tenants' data. Row-level security is enabled as an additional isolation guarantee.

---

### Schema: `platform` (relational metadata)

```sql
-- ============================================================
-- USERS
-- ============================================================
CREATE TABLE platform.users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           TEXT NOT NULL UNIQUE,
    password_hash   TEXT NOT NULL,                    -- bcrypt, never plaintext
    full_name       TEXT,
    phone           TEXT,                             -- for SMS notifications
    country         TEXT NOT NULL DEFAULT 'KE',
    role            TEXT NOT NULL DEFAULT 'USER',     -- USER | ADMIN
    email_verified  BOOLEAN NOT NULL DEFAULT FALSE,
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_login_at   TIMESTAMPTZ
);

CREATE INDEX idx_users_email ON platform.users (email);


-- ============================================================
-- SUBSCRIPTION TIERS
-- (Configurable at runtime, not hardcoded in application)
-- ============================================================
CREATE TABLE platform.subscription_tiers (
    id                      SERIAL PRIMARY KEY,
    name                    TEXT NOT NULL UNIQUE,         -- FREE | STARTER | PROFESSIONAL | ENTERPRISE
    max_messages_per_day    BIGINT NOT NULL,              -- hard limit, -1 = unlimited
    max_devices             INTEGER NOT NULL,
    max_projects            INTEGER NOT NULL,
    data_retention_days     INTEGER NOT NULL,             -- -1 = indefinite
    max_message_rate_per_sec INTEGER NOT NULL,            -- per-project rate cap at broker
    storage_limit_mb        BIGINT NOT NULL,              -- -1 = unlimited
    api_calls_per_minute    INTEGER NOT NULL,
    price_kes_monthly       NUMERIC(10,2) NOT NULL DEFAULT 0,
    features                JSONB,                        -- { "webhooks": true, "csv_export": true }
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Seed data
INSERT INTO platform.subscription_tiers
    (name, max_messages_per_day, max_devices, max_projects, data_retention_days,
     max_message_rate_per_sec, storage_limit_mb, api_calls_per_minute, price_kes_monthly, features)
VALUES
    ('FREE',         10000,    5,   2,   7,   10,    500,   60,   0,      '{"webhooks": false, "csv_export": true, "downsampling": false}'),
    ('STARTER',      100000,   25,  5,   30,  50,    5000,  300,  1500,   '{"webhooks": true,  "csv_export": true, "downsampling": true}'),
    ('PROFESSIONAL', 1000000,  100, 20,  90,  200,   50000, 1000, 6000,   '{"webhooks": true,  "csv_export": true, "downsampling": true, "api_access": true}'),
    ('ENTERPRISE',   -1,      -1,  -1,  -1,  -1,    -1,    -1,   0,      '{"webhooks": true,  "csv_export": true, "downsampling": true, "api_access": true, "sla": true}');


-- ============================================================
-- USER SUBSCRIPTIONS
-- ============================================================
CREATE TABLE platform.user_subscriptions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES platform.users(id) ON DELETE CASCADE,
    tier_id         INTEGER NOT NULL REFERENCES platform.subscription_tiers(id),
    status          TEXT NOT NULL DEFAULT 'ACTIVE',   -- ACTIVE | CANCELLED | PAST_DUE | TRIAL
    started_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    expires_at      TIMESTAMPTZ,                      -- NULL = indefinite (enterprise)
    cancelled_at    TIMESTAMPTZ,
    payment_ref     TEXT,                             -- external payment gateway reference
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_subscriptions_user ON platform.user_subscriptions (user_id);


-- ============================================================
-- PROJECTS
-- ============================================================
CREATE TABLE platform.projects (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES platform.users(id) ON DELETE CASCADE,
    name                TEXT NOT NULL,
    description         TEXT,
    environment         TEXT NOT NULL DEFAULT 'production',  -- production | development | testing
    region              TEXT NOT NULL DEFAULT 'af-ke-1',     -- af-ke-1 | af-ng-1 | af-za-1 | ...
    timezone            TEXT NOT NULL DEFAULT 'Africa/Nairobi',

    -- Device configuration
    expected_device_count    INTEGER NOT NULL DEFAULT 1,
    declared_send_interval_s INTEGER,                        -- seconds between messages per device; NULL = irregular

    -- Data configuration
    data_format         TEXT NOT NULL DEFAULT 'json',        -- json | csv | msgpack | cbor
    retention_override_days INTEGER,                         -- NULL = use tier default

    -- Alert configuration
    alert_webhook_url   TEXT,
    alert_email         TEXT,

    -- Status
    is_active           BOOLEAN NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_project_name_per_user UNIQUE (user_id, name)
);

CREATE INDEX idx_projects_user ON platform.projects (user_id);


-- ============================================================
-- API KEYS
-- Structured format: IoTeca_{env}_{project_short_id}_{secret}
-- Example:           IoTeca_live_a3f9b2c1_k7x2m9q4r8v3n1p5
-- Only the hash is stored; the full key is shown to the user once.
-- ============================================================
CREATE TABLE platform.api_keys (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES platform.projects(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,                        -- user-given label e.g. "Main sensor cluster"
    key_prefix      TEXT NOT NULL,                        -- first 20 chars, shown in UI for identification
    key_hash        TEXT NOT NULL UNIQUE,                 -- SHA-256 of full key
    environment     TEXT NOT NULL DEFAULT 'live',         -- live | test
    scopes          TEXT[] NOT NULL DEFAULT '{ingest}',   -- ingest | read | admin
    last_used_at    TIMESTAMPTZ,
    expires_at      TIMESTAMPTZ,                          -- NULL = non-expiring
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    revoked_at      TIMESTAMPTZ,
    revoke_reason   TEXT
);

CREATE INDEX idx_api_keys_project ON platform.api_keys (project_id);
CREATE INDEX idx_api_keys_hash    ON platform.api_keys (key_hash);     -- fast lookup on every ingest


-- ============================================================
-- DEVICES
-- A device is a registered physical unit that sends data.
-- Devices are identified by a device_id string they include in their payload.
-- First-seen devices are auto-registered; users can then label and manage them.
-- ============================================================
CREATE TABLE platform.devices (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES platform.projects(id) ON DELETE CASCADE,
    device_id       TEXT NOT NULL,            -- user-defined string in payload e.g. "sensor_01"
    name            TEXT,                     -- user-assigned friendly name
    firmware_version TEXT,
    hardware_type   TEXT,                     -- "Arduino Uno" | "Raspberry Pi" | "ESP32" | custom
    location_label  TEXT,                     -- "Greenhouse North" etc.
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    tags            JSONB,                    -- arbitrary key-value labels
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    first_seen_at   TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_seen_at    TIMESTAMPTZ,
    last_ip         INET,                     -- IP at last connection (for geo-inference)
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT unique_device_per_project UNIQUE (project_id, device_id)
);

CREATE INDEX idx_devices_project   ON platform.devices (project_id);
CREATE INDEX idx_devices_last_seen ON platform.devices (last_seen_at);


-- ============================================================
-- ALERT RULES
-- User-defined conditions evaluated against the ingest stream.
-- ============================================================
CREATE TABLE platform.alert_rules (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id      UUID NOT NULL REFERENCES platform.projects(id) ON DELETE CASCADE,
    name            TEXT NOT NULL,
    description     TEXT,
    metric_name     TEXT NOT NULL,            -- field name to watch e.g. "temperature"
    device_id       TEXT,                     -- NULL = apply to all devices in project
    condition       TEXT NOT NULL,            -- gt | lt | eq | gte | lte | absence
    threshold       DOUBLE PRECISION,         -- NULL for absence condition
    absence_window_s INTEGER,                 -- for absence: fire if no message in X seconds
    notification_channels TEXT[] NOT NULL,    -- webhook | email | sms
    suppression_window_s INTEGER DEFAULT 300, -- minimum seconds between repeated alerts
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    last_fired_at   TIMESTAMPTZ,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_alert_rules_project ON platform.alert_rules (project_id);


-- ============================================================
-- NOTIFICATIONS LOG
-- Audit trail of all notifications sent.
-- ============================================================
CREATE TABLE platform.notification_log (
    id              BIGSERIAL PRIMARY KEY,
    user_id         UUID NOT NULL REFERENCES platform.users(id),
    project_id      UUID REFERENCES platform.projects(id),
    type            TEXT NOT NULL,            -- QUOTA_WARNING | QUOTA_EXCEEDED | ALERT | SYSTEM
    channel         TEXT NOT NULL,            -- EMAIL | SMS | WEBHOOK
    subject         TEXT,
    body            TEXT,
    sent_at         TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    delivered       BOOLEAN NOT NULL DEFAULT FALSE,
    error           TEXT
);

CREATE INDEX idx_notifications_user ON platform.notification_log (user_id, sent_at DESC);


-- ============================================================
-- ADMIN AUDIT LOG
-- Every admin action is recorded.
-- ============================================================
CREATE TABLE platform.admin_audit_log (
    id              BIGSERIAL PRIMARY KEY,
    admin_user_id   UUID NOT NULL REFERENCES platform.users(id),
    action          TEXT NOT NULL,            -- SUSPEND_PROJECT | OVERRIDE_QUOTA | DELETE_USER | etc.
    target_type     TEXT NOT NULL,            -- USER | PROJECT | API_KEY | TIER
    target_id       TEXT NOT NULL,
    details         JSONB,
    performed_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    ip_address      INET
);
```

---

### Schema: `tsdata` (time-series sensor data)

```sql
-- ============================================================
-- SENSOR DATA  (the core hypertable)
-- All sensor readings from all projects land here.
-- Partitioned by time automatically by TimescaleDB.
-- ============================================================
CREATE TABLE tsdata.sensor_data (
    time        TIMESTAMPTZ     NOT NULL,
    project_id  UUID            NOT NULL,   -- tenant key â€” always filter on this first
    device_id   TEXT            NOT NULL,
    metric      TEXT            NOT NULL,   -- field name e.g. "temperature", "humidity", "voltage"
    value       DOUBLE PRECISION,           -- numeric reading; NULL if non-numeric
    value_str   TEXT,                       -- for non-numeric fields e.g. "status": "online"
    tags        JSONB,                      -- arbitrary metadata from payload
    ingested_at TIMESTAMPTZ     NOT NULL DEFAULT NOW()   -- server-side receipt timestamp
);

-- Convert to hypertable, partition by time in 1-day chunks
SELECT create_hypertable('tsdata.sensor_data', 'time', chunk_time_interval => INTERVAL '1 day');

-- Composite index for per-project time-series queries
CREATE INDEX idx_sensor_data_project_time
    ON tsdata.sensor_data (project_id, time DESC);

-- Index for per-device drill-down
CREATE INDEX idx_sensor_data_device_time
    ON tsdata.sensor_data (project_id, device_id, metric, time DESC);

-- Enable row-level security
ALTER TABLE tsdata.sensor_data ENABLE ROW LEVEL SECURITY;

-- Policy: applications connect as app_user; projects are filtered by project_id claim
-- (enforced at API layer; RLS is a safety net)
CREATE POLICY tenant_isolation ON tsdata.sensor_data
    USING (project_id = current_setting('app.current_project_id')::UUID);


-- ============================================================
-- CONTINUOUS AGGREGATES (automatic downsampling)
-- These are materialized views maintained by TimescaleDB.
-- They run in the background â€” no cron job needed.
-- ============================================================

-- 1-minute averages (for data > 24 hours old)
CREATE MATERIALIZED VIEW tsdata.sensor_data_1min
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 minute', time) AS bucket,
    project_id,
    device_id,
    metric,
    AVG(value)   AS avg_value,
    MIN(value)   AS min_value,
    MAX(value)   AS max_value,
    COUNT(*)     AS sample_count
FROM tsdata.sensor_data
GROUP BY bucket, project_id, device_id, metric;

-- Add policy: maintain within a rolling window
SELECT add_continuous_aggregate_policy('tsdata.sensor_data_1min',
    start_offset => INTERVAL '2 days',
    end_offset   => INTERVAL '1 minute',
    schedule_interval => INTERVAL '1 minute');

-- 1-hour averages (for data > 7 days old)
CREATE MATERIALIZED VIEW tsdata.sensor_data_1hr
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 hour', time) AS bucket,
    project_id,
    device_id,
    metric,
    AVG(value)   AS avg_value,
    MIN(value)   AS min_value,
    MAX(value)   AS max_value,
    COUNT(*)     AS sample_count
FROM tsdata.sensor_data
GROUP BY bucket, project_id, device_id, metric;

SELECT add_continuous_aggregate_policy('tsdata.sensor_data_1hr',
    start_offset => INTERVAL '8 days',
    end_offset   => INTERVAL '1 hour',
    schedule_interval => INTERVAL '1 hour');

-- 1-day averages (for data > 30 days old)
CREATE MATERIALIZED VIEW tsdata.sensor_data_1day
WITH (timescaledb.continuous) AS
SELECT
    time_bucket('1 day', time) AS bucket,
    project_id,
    device_id,
    metric,
    AVG(value)   AS avg_value,
    MIN(value)   AS min_value,
    MAX(value)   AS max_value,
    COUNT(*)     AS sample_count
FROM tsdata.sensor_data
GROUP BY bucket, project_id, device_id, metric;

SELECT add_continuous_aggregate_policy('tsdata.sensor_data_1day',
    start_offset => INTERVAL '31 days',
    end_offset   => INTERVAL '1 day',
    schedule_interval => INTERVAL '1 day');


-- ============================================================
-- RETENTION POLICIES (auto-delete raw data after tier limit)
-- ============================================================

-- Default: delete raw data older than 90 days
-- Override per project via the application (conditional drop chunks)
SELECT add_retention_policy('tsdata.sensor_data', INTERVAL '90 days');
-- Raw data older than 2 days collapses to 1-min aggregates (above)
-- 1-min aggregates older than 7 days collapse to 1-hour (above)
-- 1-hour aggregates are retained for 1 year by default


-- ============================================================
-- PROJECT QUOTA SNAPSHOTS
-- Daily snapshot of usage per project â€” used for billing,
-- admin dashboards, and trend charts. Written by quota-service.
-- ============================================================
CREATE TABLE tsdata.project_quota_snapshots (
    time            TIMESTAMPTZ NOT NULL,
    project_id      UUID NOT NULL,
    messages_today  BIGINT NOT NULL DEFAULT 0,
    storage_mb      NUMERIC(12,4) NOT NULL DEFAULT 0,
    active_devices  INTEGER NOT NULL DEFAULT 0,
    api_calls_today BIGINT NOT NULL DEFAULT 0
);

SELECT create_hypertable('tsdata.project_quota_snapshots', 'time',
    chunk_time_interval => INTERVAL '7 days');

CREATE INDEX idx_quota_snapshots_project
    ON tsdata.project_quota_snapshots (project_id, time DESC);
```

---

### Entity Relationships Summary

```
users
  â””â”€â”€< user_subscriptions >â”€â”€ subscription_tiers
  â””â”€â”€< projects
            â””â”€â”€< api_keys
            â””â”€â”€< devices
            â””â”€â”€< alert_rules
            â””â”€â”€< notification_log

tsdata.sensor_data  (project_id FK â†’ projects.id, device_id soft-ref â†’ devices.device_id)
tsdata.sensor_data_1min / 1hr / 1day  (continuous aggregates of sensor_data)
tsdata.project_quota_snapshots  (project_id FK â†’ projects.id)
```

---

## 6. API Key Design

### Key Structure

```
IoTeca_live_a3f9b2c1_k7x2m9q4r8v3n1p5w6y0
â”‚         â”‚    â”‚           â”‚
â”‚         â”‚    â”‚           â””â”€â”€ 20-char random secret (URL-safe base64)
â”‚         â”‚    â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ 8-char project identifier (first 8 chars of project UUID)
â”‚         â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ environment: live | test
â””â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€â”€ platform prefix (registered with GitLeaks / TruffleHog)
```

### Why This Structure

- The prefix `IoTeca_` allows GitHub's secret scanner and tools like TruffleHog to detect accidentally committed keys and alert the project owner automatically. You register the prefix pattern with these services.
- The embedded project short ID means the ingestion service can extract the project context directly from the key string without a database lookup â€” only a hash comparison is needed to verify the secret portion.
- The environment segment prevents a test device from accidentally writing to a production project.
- The full key is shown to the user **once only** at creation. After that, only `key_prefix` (first 20 chars) is stored in plaintext for UI display. The rest is stored as `SHA-256(full_key)`.

### Key Lifecycle

```
1. User requests a new key for their project
2. Server generates: prefix + project_short_id + random_secret
3. Server computes: key_hash = SHA256(full_key)
4. Server stores: (id, project_id, name, key_prefix, key_hash, environment, scopes, ...)
5. Server returns: full key to user â€” THIS IS THE ONLY TIME IT IS VISIBLE
6. On ingest: device sends key in MQTT password or HTTP Authorization header
7. Auth service: key_hash = SHA256(incoming_key), lookup by key_hash, check is_active + expiry
8. On revoke: set is_active = false, revoked_at = NOW(), revoke_reason = ...
```

---

## 7. Ingestion Pipeline

### Flow

```
Device  â†’  EMQX Broker  â†’  Kafka (raw.ingest.af-ke-1)  â†’  ingestion-service  â†’  TimescaleDB
                â”‚
                â””â”€â”€ Auth webhook â†’ auth-service
                    (called on connect, validates API key, checks quota)
```

### MQTT Topic Convention

Devices publish to:
```
IoTeca/{project_id}/{device_id}/{metric_group}
```

Examples:
```
IoTeca/550e8400-e29b-41d4-a716/sensor_01/environment
IoTeca/550e8400-e29b-41d4-a716/sensor_01/power
```

The broker validates the project_id in the topic matches the project embedded in the API key. A device cannot publish to another project's topic.

### Payload Format (JSON default)

```json
{
  "device_id": "sensor_01",
  "timestamp": 1712345678000,
  "metrics": {
    "temperature": 24.5,
    "humidity": 63.2,
    "battery_v": 3.7
  },
  "tags": {
    "location": "greenhouse_north",
    "firmware": "v1.2.3"
  }
}
```

The `timestamp` field is optional. If absent, the server ingestion time is used. If present, both `time` (device timestamp) and `ingested_at` (server time) are stored, allowing detection of delayed delivery.

### HTTP REST Ingestion Fallback

For devices that cannot maintain a persistent TCP connection:

```
POST /api/v1/ingest
Authorization: Bearer IoTeca_live_a3f9b2c1_...
Content-Type: application/json

{ same payload as above }
```

Batch ingest (up to 100 readings per request):
```
POST /api/v1/ingest/batch
```

---

## 8. Device Connection Methods

The platform supports all four major IoT connection protocols. The choice is entirely up to the device developer.

### MQTT (Recommended for embedded devices)

**Best for:** Arduino, ESP32, Raspberry Pi, any device on a persistent network connection.

**Why:** Persistent TCP connection, 2-byte protocol overhead per message, designed for low-bandwidth unreliable networks. A sensor sending every second does not pay TCP handshake cost per message.

```
Broker host:  mqtt.ioteca.io
Port:         1883 (plain) | 8883 (TLS â€” required for production)
Username:     {project_id}
Password:     {api_key}
Client ID:    {device_id}  (must be unique per device per project)
Topic:        IoTeca/{project_id}/{device_id}/{metric_group}
QoS:          1 (at-least-once delivery â€” recommended)
```

### HTTP REST (Recommended for scripts and server-side apps)

**Best for:** Python scripts, Node.js applications, server-side integrations, any environment with an HTTP client.

**Why:** Universal support, easy to debug, stateless â€” no persistent connection required.

```
POST https://api.ioteca.io/v1/ingest
Authorization: Bearer {api_key}
Content-Type: application/json
```

### WebSocket (Recommended for browser-based or real-time apps)

**Best for:** Browser applications sending sensor data, real-time dashboards that also produce data, applications needing bidirectional streaming.

```
wss://ws.ioteca.io/v1/stream
Authorization header or ?token={api_key} query param
```

### CoAP (For ultra-constrained devices)

**Best for:** Devices on very low-power networks, NB-IoT deployments, sensors with extremely limited memory.

**Why:** UDP-based, binary protocol, even smaller than MQTT. Designed for 8-bit microcontrollers with less than 10KB of RAM.

```
coap://coap.ioteca.io/ingest
Uri-Query: key={api_key}
Payload: CBOR or JSON
```

---

## 9. Project Configuration Options

When creating a project, users configure:

| Option | Values | Effect |
|---|---|---|
| Name | String | Identifier shown in dashboard |
| Description | String | Optional context |
| Environment | `production` / `development` / `testing` | Affects display and potentially retention rules |
| Region | `af-ke-1` (Nairobi) â€” more regions added as platform scales | Data residency |
| Timezone | e.g. `Africa/Nairobi` | Affects time bucketing in dashboard charts |
| Expected device count | Integer | Informs quota planning; broker rejects beyond tier max |
| Send interval | Seconds or `irregular` | Used to warn user if declared vs actual rate diverges |
| Data format | `json` / `csv` / `msgpack` / `cbor` | Instructs ingestion service how to parse payloads |
| Retention override | Days or `use tier default` | Project-level data retention, capped by tier |
| Alert webhook URL | URL | Where to POST when alert rules fire |
| Alert email | Email address | Who receives alert and quota notifications for this project |

---

## 10. Quota & Free Tier System

### Tier Limits

| Feature | Free | Starter | Professional | Enterprise |
|---|---|---|---|---|
| Messages / day | 10,000 | 100,000 | 1,000,000 | Unlimited |
| Devices | 5 | 25 | 100 | Unlimited |
| Projects | 2 | 5 | 20 | Unlimited |
| Data retention | 7 days | 30 days | 90 days | Custom |
| Message rate | 10 / sec | 50 / sec | 200 / sec | Custom |
| Storage | 500 MB | 5 GB | 50 GB | Custom |
| Webhooks | No | Yes | Yes | Yes |
| Price (KES/month) | Free | 1,500 | 6,000 | Contact |

### How Quotas Are Enforced

Rate limiting happens at two points:

**1. At the broker (connection-time and publish-time)**
EMQX's rate limiter enforces per-connection message rates. A project on the Free tier cannot publish more than 10 messages/second regardless of how many devices are connected. This is configured dynamically via the EMQX management API from the `quota-service` on tier change.

**2. At the quota-service (daily message count)**
Every message consumed from Kafka increments a Redis counter:
```
INCR project:{project_id}:msgs:{YYYY-MM-DD}
EXPIRE project:{project_id}:msgs:{YYYY-MM-DD} 172800   -- 2 days TTL
```
This is O(1), atomic, and adds zero latency to the ingestion path. The quota-service reads this counter and compares to the tier limit.

**Notification triggers:**
- At 80%: send warning email â€” "You've used 8,000 of your 10,000 daily messages. Consider upgrading."
- At 100%: send exceeded email with upgrade link. New messages are rejected at the broker with a PUBACK reason code or HTTP 429.
- After midnight: counter resets automatically (TTL expiry).

---

## 11. Dashboard & Visualizations

### Per-Project Dashboard

Each project has a dashboard showing:

- **Live status strip** â€” number of online devices right now (WebSocket-driven), messages in last 60 minutes, current message rate
- **Time-series charts** (Apache ECharts) â€” plot any metric from any device over a user-selected time window (last 1h, 6h, 24h, 7d, 30d, custom). Data is served from the appropriate aggregate view depending on the time window:
  - < 24h window â†’ raw `sensor_data`
  - 24hâ€“7d window â†’ `sensor_data_1min`
  - 7dâ€“30d window â†’ `sensor_data_1hr`
  - > 30d window â†’ `sensor_data_1day`
- **Device map** (Leaflet.js) â€” pins for each device with last-known coordinates. Pin color indicates device status: green (active, seen < 5 min ago), amber (stale, seen < 1h), red (offline, not seen > 1h)
- **Device table** â€” sortable list of devices with: device_id, last seen, message count today, firmware version, assigned label
- **Quota bar** â€” visual indicator of daily message usage vs limit

### API Key Management

Under each project: list of API keys with name, prefix (for identification), environment, scopes, last-used timestamp, and revoke button.

---

## 12. Admin Panel

Accessible at `/admin` â€” only rendered for `ROLE_ADMIN` users. The admin frontend is a separate Vue.js route group served from the same SPA but conditionally shown.

### Platform Overview

- Total registered users (and growth trend)
- Total active projects
- Platform-wide message rate (live, from Kafka consumer lag metrics)
- Total storage consumed across all tenants
- Active MQTT connections right now
- Ingestion error rate

### Per-Project Governance

Searchable table of all projects:
- Owner email, tier, project name, region, created date
- Messages today vs quota (visual bar, red if > 80%)
- Storage used vs quota
- Active devices vs limit
- Last activity timestamp
- Actions: view details, suspend, override quota, change tier

Per-project drill-down:
- Full usage history chart
- All devices and their last-seen time
- API keys (prefix, last used, active/revoked)
- Raw ingestion log (last 100 messages, useful for debugging user issues)

### User Management

- User list with email, subscription tier, account status, join date
- Force password reset, deactivate account, change tier manually
- Abuse detection flag: accounts with suspiciously many free-tier projects

### System Health

- Kafka consumer group lag per topic (if lag grows, ingestion-service is falling behind)
- TimescaleDB: slow queries, hypertable chunk sizes, compression ratio
- Redis: memory usage, eviction rate, key count
- EMQX: connection count, publish rate, authentication failures per minute
- Per-service container health (via Docker/Kubernetes health checks)

### Governance Actions

- Manually send quota notification to any user
- Send platform-wide maintenance announcement
- Emergency throttle: reduce platform-wide rate limits without a code deploy
- Edit tier configuration at runtime (changes stored in `subscription_tiers` table, picked up by services without restart)
- View and export admin audit log

---

## 13. Containerization & Deployment

### Container Map

```
docker-compose.yml
â”œâ”€â”€ nginx              (reverse proxy, TLS, static frontend)
â”œâ”€â”€ frontend           (Vue.js built into Nginx image)
â”œâ”€â”€ auth-service       (Java Spring Boot, port 8081)
â”œâ”€â”€ ingestion-service  (Java Spring Boot, port 8082)
â”œâ”€â”€ alert-engine       (Java Spring Boot, port 8083)
â”œâ”€â”€ quota-service      (Java Spring Boot, port 8084)
â”œâ”€â”€ api-gateway        (Java Spring Boot, port 8080 â€” only port exposed externally via Nginx)
â”œâ”€â”€ notification-service (Java Spring Boot, port 8085)
â”œâ”€â”€ emqx               (EMQX broker, ports 1883, 8883, 8083/ws, 18083/dashboard)
â”œâ”€â”€ kafka              (Apache Kafka)
â”œâ”€â”€ zookeeper          (Kafka dependency â€” or use KRaft mode for Kafka 3.3+)
â”œâ”€â”€ timescaledb        (TimescaleDB, port 5432)
â”œâ”€â”€ redis              (Redis, port 6379)
â””â”€â”€ kafka-ui           (optional: Kafka management UI, dev only)
```

### Network Design

All services communicate on an internal Docker network `IoTeca-internal`. Only Nginx and EMQX expose ports to the host. No database or Kafka port is ever exposed externally.

```
External internet
  â”‚
  â”œâ”€â”€ :443 (HTTPS) â†’ Nginx â†’ api-gateway (:8080)
  â”œâ”€â”€ :8883 (MQTT TLS) â†’ EMQX
  â””â”€â”€ :443/ws â†’ Nginx â†’ EMQX WebSocket bridge
```

---

## 14. Directory Structure

```
IoTeca/
â”œâ”€â”€ docker-compose.yml
â”œâ”€â”€ docker-compose.dev.yml         # dev overrides (hot reload, exposed DB ports)
â”œâ”€â”€ .env.example
â”œâ”€â”€ README.md
â”‚
â”œâ”€â”€ services/
â”‚   â”œâ”€â”€ auth-service/
â”‚   â”‚   â”œâ”€â”€ Dockerfile
â”‚   â”‚   â””â”€â”€ src/main/java/io/ioteca/auth/
â”‚   â”œâ”€â”€ ingestion-service/
â”‚   â”‚   â”œâ”€â”€ Dockerfile
â”‚   â”‚   â””â”€â”€ src/main/java/io/ioteca/ingestion/
â”‚   â”œâ”€â”€ alert-engine/
â”‚   â”‚   â”œâ”€â”€ Dockerfile
â”‚   â”‚   â””â”€â”€ src/main/java/io/ioteca/alert/
â”‚   â”œâ”€â”€ quota-service/
â”‚   â”‚   â”œâ”€â”€ Dockerfile
â”‚   â”‚   â””â”€â”€ src/main/java/io/ioteca/quota/
â”‚   â”œâ”€â”€ api-gateway/
â”‚   â”‚   â”œâ”€â”€ Dockerfile
â”‚   â”‚   â””â”€â”€ src/main/java/io/ioteca/gateway/
â”‚   â””â”€â”€ notification-service/
â”‚       â”œâ”€â”€ Dockerfile
â”‚       â””â”€â”€ src/main/java/io/ioteca/notification/
â”‚
â”œâ”€â”€ frontend/
â”‚   â”œâ”€â”€ Dockerfile
â”‚   â”œâ”€â”€ nginx.conf
â”‚   â”œâ”€â”€ package.json
â”‚   â””â”€â”€ src/
â”‚       â”œâ”€â”€ main.js
â”‚       â”œâ”€â”€ router/
â”‚       â”œâ”€â”€ store/
â”‚       â”œâ”€â”€ views/
â”‚       â”‚   â”œâ”€â”€ auth/
â”‚       â”‚   â”œâ”€â”€ dashboard/
â”‚       â”‚   â”œâ”€â”€ project/
â”‚       â”‚   â”œâ”€â”€ admin/
â”‚       â”‚   â””â”€â”€ onboarding/
â”‚       â””â”€â”€ components/
â”‚           â”œâ”€â”€ charts/
â”‚           â”œâ”€â”€ map/
â”‚           â””â”€â”€ ui/
â”‚
â”œâ”€â”€ infra/
â”‚   â”œâ”€â”€ emqx/
â”‚   â”‚   â”œâ”€â”€ emqx.conf
â”‚   â”‚   â””â”€â”€ acl.conf
â”‚   â”œâ”€â”€ kafka/
â”‚   â”‚   â””â”€â”€ server.properties
â”‚   â”œâ”€â”€ nginx/
â”‚   â”‚   â””â”€â”€ nginx.conf
â”‚   â”œâ”€â”€ postgres/
â”‚   â”‚   â”œâ”€â”€ 01-init-schemas.sql
â”‚   â”‚   â”œâ”€â”€ 02-platform-tables.sql
â”‚   â”‚   â””â”€â”€ 03-tsdata-tables.sql
â”‚   â””â”€â”€ redis/
â”‚       â””â”€â”€ redis.conf
â”‚
â””â”€â”€ scripts/
    â”œâ”€â”€ seed-tiers.sql
    â”œâ”€â”€ load-test.py               # Python: simulate N devices sending data
    â”œâ”€â”€ migrate.sh                 # run database migrations
    â””â”€â”€ create-kafka-topics.sh
```

---

## 15. Environment Variables

```bash
# ---- Database ----
DB_HOST=timescaledb
DB_PORT=5432
DB_NAME=IoTeca
DB_USER=IoTeca_app
DB_PASSWORD=changeme

# ---- Redis ----
REDIS_HOST=redis
REDIS_PORT=6379
REDIS_PASSWORD=changeme

# ---- Kafka ----
KAFKA_BOOTSTRAP_SERVERS=kafka:9092
KAFKA_CONSUMER_GROUP_ID=ioteca-services

# ---- EMQX ----
EMQX_API_URL=http://emqx:18083/api/v5
EMQX_API_KEY=changeme
EMQX_AUTH_WEBHOOK_SECRET=changeme

# ---- Auth ----
JWT_SECRET=changeme-min-32-chars
JWT_EXPIRY_MINUTES=60
JWT_REFRESH_EXPIRY_DAYS=30

# ---- Notification ----
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USER=noreply@ioteca.io
SMTP_PASSWORD=changeme
SMS_PROVIDER_API_KEY=changeme     # Africa's Talking or similar

# ---- Platform ----
PLATFORM_BASE_URL=https://ioteca.io
DEFAULT_REGION=af-ke-1
API_KEY_PREFIX=IoTeca
```

---

## 16. Development Setup

### Prerequisites

- Docker Desktop or Docker Engine + Docker Compose v2
- Java 21 (for local service development)
- Node.js 20+ (for frontend development)
- Python 3.11+ (for scripts)

### Start all services

```bash
git clone https://github.com/yourusername/IoTeca.git
cd IoTeca
cp .env.example .env
# Edit .env with your local values

docker compose -f docker-compose.yml -f docker-compose.dev.yml up --build
```

### Run database migrations

```bash
./scripts/migrate.sh
```

### Create Kafka topics

```bash
./scripts/create-kafka-topics.sh
```

### Simulate device traffic (load test)

```bash
pip install paho-mqtt faker
python scripts/load-test.py --devices 50 --interval 2 --project-id <your-project-uuid> --api-key <your-key>
```

### Service URLs (development)

| Service | URL |
|---|---|
| Frontend | http://localhost:3000 |
| API Gateway | http://localhost:8080 |
| EMQX Dashboard | http://localhost:18083 |
| Kafka UI | http://localhost:8090 |
| TimescaleDB | localhost:5432 |
| Redis | localhost:6379 |

---

## 17. Roadmap

### Phase 1 â€” Core Platform (MVP)
- [ ] User auth (register, login, JWT)
- [ ] Project and API key management
- [ ] EMQX broker with auth webhook
- [ ] Kafka pipeline
- [ ] Ingestion service writing to TimescaleDB
- [ ] Basic dashboard (time-series chart, device list)
- [ ] Free tier quota enforcement

### Phase 2 â€” Feature Complete
- [ ] All four connection protocols (MQTT, HTTP, WebSocket, CoAP)
- [ ] Device map (Leaflet.js)
- [ ] Alert rules engine
- [ ] Notification service (email + webhook)
- [ ] Quota notifications at 80% and 100%
- [ ] Admin panel (platform overview + per-project governance)
- [ ] Data export (CSV download)

### Phase 3 â€” Scale & Monetisation
- [ ] Payment integration (M-Pesa via Daraja API, card via Flutterwave)
- [ ] Subscription tier management and upgrade flow
- [ ] Continuous aggregate downsampling (retention policies)
- [ ] Multi-region support (Nairobi â†’ Lagos â†’ Johannesburg)
- [ ] EMQX cluster (multi-node)
- [ ] Schema inference and auto-tagging
- [ ] OTA metadata service

### Phase 4 â€” Ecosystem
- [ ] Official SDK: Python, JavaScript/Node.js, Arduino library
- [ ] Grafana integration (expose a Grafana data source API)
- [ ] Public device data sharing (opt-in)
- [ ] Developer API (full programmatic project and device management)

---

## Contributing

This is an active development project. Before contributing, please read the architecture section carefully â€” understanding why each layer exists is more important than any single implementation detail.

Areas where contributions are especially welcome:
- Python SDK for device simulation and testing
- Frontend component improvements (Vue.js, Options API)
- Load testing scripts and benchmarks
- Infrastructure-as-code (Terraform for cloud provisioning)

---

## License

MIT License â€” see `LICENSE` file.

---

*Built for Africa. Starting in Kenya.*
