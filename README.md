# TIP Governance Service

**CONTROLLED // FDIC INTERNAL ONLY**

A Spring Boot 3.2 microservice implementing the full TIP records-governance lifecycle — GOV-001 through GOV-019.

---

## Table of Contents

- [Architecture Overview](#architecture-overview)
- [User Stories Covered](#user-stories-covered)
- [Role Matrix](#role-matrix)
- [Prerequisites](#prerequisites)
- [Running Locally](#running-locally)
- [Authentication](#authentication)
- [API Reference](#api-reference)
- [Project Structure](#project-structure)
- [Testing](#testing)
- [Security Design](#security-design)
- [Database](#database)
- [Configuration](#configuration)

---

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    HTTP Clients / Swagger UI                 │
└────────────────────────────┬────────────────────────────────┘
                             │
                    JWT Bearer Token
                             │
┌────────────────────────────▼────────────────────────────────┐
│               JwtAuthFilter (OncePerRequestFilter)           │
│   Validates JWT → populates SecurityContext with roles       │
└────────────────────────────┬────────────────────────────────┘
                             │
┌────────────────────────────▼────────────────────────────────┐
│   @PreAuthorize role checks on every Controller method       │
├──────────────┬──────────────┬──────────────┬────────────────┤
│  Retention   │  Governed    │  Legal Hold  │  Audit /       │
│  Policy      │  Item        │  Controller  │  Batch         │
│  Controller  │  Controller  │              │  Controllers   │
└──────────────┴──────┬───────┴──────────────┴────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│                    Service Layer                              │
│  RetentionPolicyService │ GovernedItemService                │
│  LegalHoldService       │ BatchDispositionService            │
│  AuditService           │                                    │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│             Spring Data JPA Repositories                      │
└─────────────────────┬───────────────────────────────────────┘
                      │
┌─────────────────────▼───────────────────────────────────────┐
│              PostgreSQL (Flyway-managed schema)               │
└─────────────────────────────────────────────────────────────┘
```

---

## User Stories Covered

| Story  | Title                                              | Epic | Controller                    |
|--------|----------------------------------------------------|------|-------------------------------|
| GOV-001 | Create a retention policy                         | 1    | `RetentionPolicyController`   |
| GOV-002 | Activate a retention policy                       | 1    | `RetentionPolicyController`   |
| GOV-003 | Retire a retention policy                         | 1    | `RetentionPolicyController`   |
| GOV-004 | View a retention policy                           | 1    | `RetentionPolicyController`   |
| GOV-005 | Register a document under governance              | 2    | `GovernedItemController`      |
| GOV-006 | Register a database record under governance       | 2    | `GovernedItemController`      |
| GOV-007 | View a governed item                              | 2    | `GovernedItemController`      |
| GOV-008 | Create a legal hold                               | 3    | `LegalHoldController`         |
| GOV-009 | Apply a legal hold to governed items              | 3    | `LegalHoldController`         |
| GOV-010 | Release a legal hold                              | 3    | `LegalHoldController`         |
| GOV-011 | Check disposition recommendation                  | 4    | `GovernedItemController`      |
| GOV-012 | Automatically record governance actions in audit  | 5    | `AuditService` (auto)         |
| GOV-013 | View the audit history for a governance record    | 5    | `AuditController`             |
| GOV-014 | Automatically process governed items (batch)      | 6    | `BatchDispositionController`  |
| GOV-015 | Archive a document at archive eligibility date    | 6    | `GovernedItemController`      |
| GOV-016 | Archive a database record at archive eligibility  | 6    | `GovernedItemController`      |
| GOV-017 | Purge a document at purge eligibility date        | 6    | `GovernedItemController`      |
| GOV-018 | Purge a database record at purge eligibility date | 6    | `GovernedItemController`      |
| GOV-019 | View a legal hold                                 | 3    | `LegalHoldController`         |

---

## Role Matrix

| Role                  | Create/Activate/Retire Policy | View Policy | Register Items | View Items | Archive/Purge | Create/Apply/Release Hold | View Hold | Audit Trail | Batch |
|-----------------------|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| `ROLE_TIP_ADMIN`      | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| `ROLE_MANAGER`        | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | — |
| `ROLE_SR_ANALYST`     | — | ✅ | ✅ | ✅ | — | — | — | — | — |
| `ROLE_ANALYST`        | — | — | — | ✅ | — | — | — | — | — |
| `ROLE_CASH_MGMT`      | — | — | ✅ | ✅ | — | — | ✅ | ✅ | — |
| `ROLE_COMPLIANCE_ANALYST` | — | — | ✅ | ✅ | — | — | ✅ | ✅ | — |
| `ROLE_AUDITOR`        | — | — | — | ✅ | — | — | ✅ | ✅ | ✅ |
| `ROLE_SCHEDULER`      | — | — | — | — | ✅ | — | — | — | ✅ |

---

## Prerequisites

- Java 21+
- Maven 3.9+
- PostgreSQL 15+
- Docker (optional, for local DB)

---

## Running Locally

### 1. Start PostgreSQL

```bash
docker run -d \
  --name tip-postgres \
  -e POSTGRES_DB=tip_governance \
  -e POSTGRES_USER=tipgov \
  -e POSTGRES_PASSWORD=tipgov \
  -p 5432:5432 \
  postgres:15
```

### 2. Build and Run

```bash
mvn clean package -DskipTests
java -jar target/tip-governance-1.0.0-SNAPSHOT.jar
```

### 3. Open Swagger UI

```
http://localhost:8080/swagger-ui.html
```

---

## Authentication

The API uses **stateless JWT Bearer authentication**.

### Step 1 — Get a token

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}' | jq .
```

Response:
```json
{
  "token": "eyJhbGci...",
  "username": "admin",
  "roles": ["ROLE_TIP_ADMIN", "ROLE_MANAGER"]
}
```

### Step 2 — Use the token

```bash
curl -s http://localhost:8080/api/v1/retention-policies \
  -H "Authorization: Bearer eyJhbGci..."
```

### Built-in Test Users

| Username     | Password     | Roles                                        |
|--------------|--------------|----------------------------------------------|
| `admin`      | `admin123`   | `ROLE_TIP_ADMIN`, `ROLE_MANAGER`             |
| `analyst`    | `analyst123` | `ROLE_SR_ANALYST`, `ROLE_ANALYST`            |
| `compliance` | `comply123`  | `ROLE_COMPLIANCE_ANALYST`                    |
| `auditor`    | `audit123`   | `ROLE_AUDITOR`                               |
| `scheduler`  | `sched123`   | `ROLE_SCHEDULER`                             |

> ⚠️ Replace in-memory users with a database-backed `UserDetailsService` in production.

---

## API Reference

### Retention Policies

| Method | Path                                    | Story   | Role Required          |
|--------|-----------------------------------------|---------|------------------------|
| POST   | `/api/v1/retention-policies`            | GOV-001 | TIP_ADMIN, MANAGER     |
| POST   | `/api/v1/retention-policies/{id}/activate` | GOV-002 | TIP_ADMIN, MANAGER  |
| POST   | `/api/v1/retention-policies/{id}/retire`   | GOV-003 | TIP_ADMIN, MANAGER  |
| GET    | `/api/v1/retention-policies/{id}`       | GOV-004 | TIP_ADMIN, MANAGER, SR_ANALYST |
| GET    | `/api/v1/retention-policies`            | GOV-004 | TIP_ADMIN, MANAGER, SR_ANALYST |

### Governed Items

| Method | Path                                                       | Story   |
|--------|------------------------------------------------------------|---------|
| POST   | `/api/v1/governed-items/documents`                         | GOV-005 |
| POST   | `/api/v1/governed-items/database-records`                  | GOV-006 |
| GET    | `/api/v1/governed-items/{id}`                              | GOV-007 |
| GET    | `/api/v1/governed-items/{id}/disposition-recommendation`   | GOV-011 |
| POST   | `/api/v1/governed-items/{id}/archive-document`             | GOV-015 |
| POST   | `/api/v1/governed-items/{id}/archive-database-record`      | GOV-016 |
| POST   | `/api/v1/governed-items/{id}/purge-document`               | GOV-017 |
| POST   | `/api/v1/governed-items/{id}/purge-database-record`        | GOV-018 |
| POST   | `/api/v1/governed-items/{id}/events`                       | EventDate clock |
| GET    | `/api/v1/governed-items/{id}/events`                       | EventDate clock |

### Legal Holds

| Method | Path                                    | Story   |
|--------|-----------------------------------------|---------|
| POST   | `/api/v1/legal-holds`                   | GOV-008 |
| POST   | `/api/v1/legal-holds/{holdId}/apply`    | GOV-009 |
| POST   | `/api/v1/legal-holds/{holdId}/release`  | GOV-010 |
| GET    | `/api/v1/legal-holds/{holdId}`          | GOV-019 |
| GET    | `/api/v1/legal-holds`                   | GOV-019 |

### Audit Trail

| Method | Path                    | Story   | Role Required                     |
|--------|-------------------------|---------|-----------------------------------|
| GET    | `/api/v1/audit?recordType=&recordId=` | GOV-013 | AUDITOR, CASH_MGMT, COMPLIANCE_ANALYST, TIP_ADMIN |

### Batch Disposition

| Method | Path                            | Story   |
|--------|---------------------------------|---------|
| POST   | `/api/v1/disposition/batch/run` | GOV-014 |
| GET    | `/api/v1/disposition/batch/runs`| GOV-014 |

---

## Project Structure

```
src/main/java/gov/fdic/tip/governance/
├── TipGovernanceApplication.java     # Entry point + @EnableScheduling
├── config/
│   ├── SecurityConfig.java           # JWT filter chain, @EnableMethodSecurity
│   └── OpenApiConfig.java            # Swagger/OpenAPI bean
├── controller/
│   ├── AuthController.java           # POST /auth/login
│   ├── RetentionPolicyController.java # GOV-001..004
│   ├── GovernedItemController.java    # GOV-005..007, 011, 015..018
│   ├── GovernedItemEventController.java # EventDate support
│   ├── LegalHoldController.java       # GOV-008..010, 019
│   ├── AuditController.java           # GOV-012..013
│   └── BatchDispositionController.java # GOV-014
├── service/
│   ├── RetentionPolicyService.java
│   ├── GovernedItemService.java
│   ├── LegalHoldService.java
│   ├── BatchDispositionService.java   # @Scheduled daily sweep
│   └── AuditService.java
├── entity/         # JPA entities (7 tables)
├── repository/     # Spring Data JPA repositories (7)
├── dto/            # Request/Response DTOs (13)
├── enums/          # Domain enums (11)
├── security/
│   ├── JwtUtils.java
│   ├── JwtAuthFilter.java
│   ├── TipUserDetails.java
│   └── TipUserDetailsService.java
└── exception/
    ├── GlobalExceptionHandler.java   # RFC 9457 ProblemDetail responses
    ├── ResourceNotFoundException.java
    └── GovernanceBusinessException.java

src/main/resources/
├── application.yml
└── db/migration/
    └── V1__create_governance_schema.sql  # Flyway

src/test/
├── service/
│   ├── RetentionPolicyServiceTest.java  (GOV-001..004)
│   ├── GovernedItemServiceTest.java     (GOV-005..007, 011, 015..018)
│   ├── LegalHoldServiceTest.java        (GOV-008..010, 019)
│   └── BatchDispositionServiceTest.java (GOV-014)
└── controller/
    ├── RetentionPolicyControllerTest.java
    ├── LegalHoldControllerTest.java
    └── AuditControllerTest.java
```

---

## Testing

```bash
# Run all tests
mvn test

# Run a specific class
mvn test -Dtest=RetentionPolicyServiceTest

# Run with coverage report
mvn verify
```

Test strategy:
- **Service tests**: Mockito unit tests — all acceptance criteria per story
- **Controller tests**: `@WebMvcTest` + `@WithMockUser` — verifies `@PreAuthorize` rules and HTTP contract
- Each GOV story has at least one positive and one negative (forbidden / business error) test

---

## Security Design

| Aspect              | Implementation |
|---------------------|----------------|
| Auth mechanism      | Stateless JWT (HMAC-SHA256) |
| Token lifetime      | 24 hours (configurable) |
| Session management  | `STATELESS` — no server-side session |
| CSRF                | Disabled (stateless API) |
| Authorization       | `@EnableMethodSecurity` + `@PreAuthorize("hasAnyRole(...)")` on every endpoint |
| Audit trail         | Append-only (`AuditService`), PostgreSQL RLS prevents DELETE/UPDATE |
| Password encoding   | `NoOpPasswordEncoder` for demo — **replace with BCrypt in production** |

---

## Database

- **Flyway** manages schema versioning (`V1__create_governance_schema.sql`)
- All enums are native PostgreSQL enum types
- `audit_event` is protected with Row-Level Security (INSERT only for app users)
- Archive/purge eligibility dates computed in the service layer at registration time and stored for efficient sweep queries

---

## Configuration

Key properties in `application.yml`:

```yaml
app:
  jwt:
    secret: ${JWT_SECRET}          # Set via env var in production
    expiration-ms: 86400000        # 24 hours

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/tip_governance
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}
```

Set these as environment variables for production deployments. Never commit secrets to source control.

---

## Batch Disposition Schedule

The daily sweep (`GOV-014`) runs automatically at **01:00 AM server time**:

```java
@Scheduled(cron = "0 0 1 * * *")
public BatchRunResponse runScheduled() { ... }
```

To change the schedule, update `cron` in `BatchDispositionService` or externalize it to `application.yml`.

An audit entry (`BatchRunCompleted`) is always written at the end of each sweep with full counts (evaluated / archived / purged / skipped / errors).
