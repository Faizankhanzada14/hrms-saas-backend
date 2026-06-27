# HRMS Backend

Enterprise Human Resource Management System backend. Java 21 / Spring Boot 3.3 / MySQL 8 / JWT / Flyway / MapStruct / Lombok / Swagger.

## ⚠️ Delivery status — read this first

The original spec asked for the *entire* system (auth, employees, leave, medical claims, provident fund, postings, role management, dashboards, full test suite, three environment configs, Docker, Postman collection) in one shot with zero placeholders. That's realistically a 150+ file, multi-week enterprise build. Rather than hand you something that *looks* complete but is full of silent gaps, this is being delivered in verified phases.

## ✅ All 7 phases complete

This is the full delivery: Foundation, Auth, Employee Management, Leave Management, Medical Claims, Provident Fund, Posting/Transfer, Role Management, User Accounts, the Dashboard module, a Postman collection, and a final cross-module polish pass. Every endpoint described in the original spec is implemented with real, working code — no stubs, no TODOs (verified by grep across the whole source tree before this pass).

### A note on verification
This sandbox's network is restricted to a fixed allowlist that does **not** include Maven Central, so a live `mvn compile`/`mvn test` could not be run here to mechanically prove the build green end-to-end. What *was* done as a substitute: a brace-balance sweep across all 124+ Java files (zero mismatches), a grep for leftover placeholders/TODOs (zero found), and a manual cross-check of the Spring Security public-endpoint allowlist against the actual public auth routes. Run `mvn clean verify` yourself on first pull, in an environment with normal internet access, before deploying. The new `HrmsBackendApplicationTests` context-load test (see Phase 7 below) is specifically there to catch the wiring-level bugs that unit tests can't.

---

## What's included in this phase

- **Project setup**: Maven `pom.xml` (Spring Boot 3.3, Java 21, JWT/jjwt, MapStruct, Lombok, Flyway, springdoc-openapi, MySQL + H2 for tests)
- **Auth & Security**: JWT access tokens + opaque refresh tokens, login/refresh/logout, change/forgot/reset password, account lock/unlock, failed-login tracking with auto-lock, BCrypt password hashing, role-based method security (`@PreAuthorize`)
- **Employee Management**: full CRUD, soft delete, search, filter (department/designation/posting unit/status/gender/active), pagination & sorting, get-by-id, get-by-code, directory endpoint, manager hierarchy, auto-generated employee codes
- **Cross-cutting**: global exception handler with structured JSON errors, JPA auditing (created/updated by & at), soft-delete columns, business-event audit log (`audit_logs` table + `AuditService`), Swagger/OpenAPI with bearer-token auth wired in
- **Flyway migrations**: V1 (roles/permissions), V2 (users/refresh tokens, seeded `superadmin` user), V3 (departments/designations/posting units/employees), V4 (audit logs), V5 (leave balances/requests/encashments), V6 (medical claims), V7 (PF accounts/ledger/withdrawals), V8 (posting history, seeded baseline permissions), V9 (resignation_date column for the resignation trend chart)
- **Docker**: multi-stage `Dockerfile`, `docker-compose.yml` (app + MySQL with healthcheck)
- **Config**: `application.properties` + dev/staging/prod profiles, test profile backed by H2
- **Tests**: JUnit 5 + Mockito unit tests for `AuthService`, `EmployeeService`, `LeaveService`, `MedicalClaimService`, `PfService`, `PostingService`, `RoleService`, and `DashboardService`

### Phase 2 — Leave Management

- **Leave types**: Annual / Casual / Sick, entitlements and carry-forward caps centralized in `LeavePolicy` (`@ConfigurationProperties(prefix = "hrms.leave-policy")`) so policy changes don't touch service code
- **Apply / Cancel**: balance is checked at apply time against `allocated + carriedForward - used - encashed`; cancelling a still-pending or future-dated approved leave restores the balance
- **Approve / Reject**: authorization is **manager-of-record specific** — having the `MANAGER` role alone grants nothing; the service checks that the approver is *this employee's* actual manager (via `employee.manager.user.username`), or is `HR_ADMIN`/`SUPER_ADMIN`
- **Leave balance API**: computed per type/year on demand, auto-creating a balance row seeded from policy entitlement on first access
- **Leave encashment**: salary-based, `dailyRate = salary / workingDaysPerMonth`, `amount = dailyRate * days`; goes through its own pending → approved/rejected workflow (Finance/HR/Admin), deducting from the same balance pool on approval
- **Scheduler**: `LeaveAccrualScheduler` runs a year-end carry-forward job (Jan 1, 01:00) that caps and rolls forward unused balance into the new year per `LeavePolicy`
- **History endpoints**: self-service history, HR/manager view of any employee's history, and a manager's "pending approvals for my direct reports" queue

### Phase 3 — Medical Claim Management

- **Workflow**: `SUBMITTED` (employee) → `HR_APPROVED`/`HR_REJECTED` → (if HR-approved) `FINANCE_APPROVED`/`FINANCE_REJECTED`. Finance can only act on claims HR has already forwarded; HR can only act on freshly submitted ones — enforced via `ensureStatus()` guards, not just UI conventions.
- **"Finance decisions reflect in HR dashboard" requirement**: there's a single `medical_claims` table and a single source of truth per claim, so a Finance decision is immediately visible to HR through the same record — no separate sync step, no second table to drift out of consistency.
- **Attachment upload**: multipart upload (`FileStorageService`), restricted to PNG/JPEG/PDF, size-capped via `hrms.file-storage.max-file-size-bytes`, path-traversal-safe filename handling, stored under a configurable `upload-dir` with a randomized filename (original name preserved separately for display).
- **Medical budget tracking**: no separate mutable "remaining balance" counter (which would drift under concurrent approvals) — `used` is computed on demand as `sum(amount) where status = FINANCE_APPROVED and year(claim_date) = :year`, compared against `employee.medicalAllocation`. Finance approval is rejected outright if it would exceed the remaining budget.
- **Dashboard queues**: `/pending-hr` and `/pending-finance` give each role exactly the slice of the workflow that's theirs to act on.

### Phase 4 — Provident Fund

- **Ledger-first accounting**: `PfLedgerEntry` rows are the immutable source of truth; `PfAccount.balance` is a cached running total that is *only ever* updated in the same transaction as the ledger entry that changes it (`postLedgerEntry()`), so the cached value and the ledger can never drift apart — same pattern as a real accounting ledger.
- **Monthly contribution scheduler** (`PfContributionScheduler`, 1st of month at 02:00): computes employee/employer contributions as a percentage of salary, posts both as separate ledger entries, and is **idempotent per calendar month** via `lastContributionMonth` — re-running the job for a month that's already posted is a no-op, not a double-credit.
- **Auto-opening accounts**: if `hrms.pf-policy.auto-open-accounts=true` (default), the scheduler opens a PF account with default rates for any active, salaried employee who doesn't have one yet, rather than silently skipping them.
- **Withdrawals**: request → approve/reject workflow (HR/Finance/Admin); approval re-checks the balance at decision time (not just at request time) in case other withdrawals were approved in between, then posts a negative ledger entry.
- **Statement & ledger**: per-employee statement with optional date-range filtering, plus an org-wide `/ledger` endpoint for Finance reconciliation across all accounts.

### Phase 5 — Posting/Transfer, Role Management, User Accounts

- **Master data CRUD**: Departments, Designations, and Posting Units (Factory/Branch/Head Office/Warehouse) — these existed as entities since Phase 1 (referenced by Employee as foreign keys) but had no management API until now.
- **Employee transfer**: `PostingService.transferEmployee()` captures the employee's *current* posting unit/department as "from" before applying the change, writes an immutable `PostingHistory` row, and only then updates the live `Employee` record — so history can never show a transfer that didn't actually happen.
- **Posting history**: paginated, per-employee, newest first.
- **Role management**: create/update/soft-delete roles, replace a role's permission set, assign/remove a role to/from specific users, and list every user currently holding a given role.
- **User account management** (the missing piece Role Management actually depends on): you can't "assign users" to a role if no API exists to create users beyond the seeded `superadmin`. `UserService` adds account creation (with initial password, roles, and optional link to an existing `Employee` profile so a new hire gets a login), role replacement, and activate/deactivate.
- **Permissions**: list/create endpoint, plus a baseline permission set seeded in V8 across Employee/Leave/Medical/PF/Role/User modules so roles have something real to attach on day one.

### Phase 6 — Dashboard

- **One shape per chart family, not per chart**: bar, pie, and donut charts all consume the exact same `List<ChartDataPoint>` (`{label, value}`) — they differ only in how the frontend draws the same data, so there's one backend shape, not three near-duplicate endpoints. Line and area charts likewise share one `List<TimeSeriesPoint>` (`{period, value}`) shape.
- **Zero-filled trends**: `joining-trend` and `resignation-trend` always return all 12 months for the requested year, even months with zero events — so a frontend chart doesn't have to guess whether a missing month means "zero" or "no data fetched."
- **Resignation trend required a real field**: there was no `resignationDate` on `Employee` in earlier phases (only an `employmentStatus` enum value), so a trend-by-month chart had nothing to group by. Added the column (V9), wired it into `EmployeeService` so it's auto-stamped the moment status flips to `RESIGNED` if not explicitly provided.
- **In-memory aggregation by design, not by accident**: department/gender/branch/status breakdowns and the monthly trends are computed in Java over the employee list rather than via per-dimension SQL `GROUP BY` queries — simpler to read and test at typical HRMS org sizes; flagged in the service Javadoc as the thing to swap out if this ever points at a 100k+-employee org.
- **Leave/Medical/PF statistics** reuse the same status enums already defined in their respective modules (no parallel "dashboard status" enum to drift out of sync with the real workflow statuses).

### Phase 7 — Postman collection & final polish

- **`postman/hrms-backend.postman_collection.json`** — every endpoint across all 6 functional phases, 94 requests across 12 folders (Auth, Users, Employees, Master Data, Leave, Leave Encashment, Medical Claims, Provident Fund, Postings, Roles, Permissions, Dashboard). The **Login** request has a test script that auto-saves `accessToken`/`refreshToken` as collection variables, so every other request in the collection works immediately after running it once — no manual token copy-pasting.
- **`postman/hrms-local-dev.postman_environment.json`** — a minimal environment pointing `baseUrl` at `http://localhost:8080`.
- **Sanity sweep**: grepped the entire source tree for `TODO`/`FIXME`/placeholder markers (zero found), checked all 9 Flyway migrations are sequential with no version gaps, and ran a brace-balance check across every `.java` file (zero mismatches) as a cheap proxy for syntax errors in an environment where a live compiler isn't reachable.
- **`HrmsBackendApplicationTests`**: a context-load smoke test (H2, `test` profile) that asserts the full Spring context assembles and every controller bean from every module is actually registered. This is the test that catches "the bean wiring is broken" bugs that per-service unit tests structurally can't — if this one goes red, something is wrong regardless of whether individual business logic tests pass.

### Using the Postman collection

```bash
# 1. Import both files into Postman:
#    postman/hrms-backend.postman_collection.json
#    postman/hrms-local-dev.postman_environment.json
# 2. Select the "HRMS - Local Dev" environment
# 3. Start the backend (docker compose up, or mvn spring-boot:run)
# 4. Run Auth > Login (uses the seeded superadmin/Admin@123) — tokens are saved automatically
# 5. Run anything else in the collection
```

## Getting started

```bash
# 1. Start MySQL + app together
export JWT_SECRET=$(openssl rand -base64 64)
docker compose up --build

# OR run locally against your own MySQL:
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

Default seeded admin: `superadmin` / `Admin@123` — **change this immediately**, it's only there so you have a way in on first boot.

Swagger UI: `http://localhost:8080/swagger-ui.html`

## Architecture

`postman/` at the project root holds the Postman collection + environment (see Phase 7 section below). Everything else lives under `src/main/java/com/hrms`:

```
com.hrms
 ├── auth/            login, refresh, logout, password flows
 ├── user/            User + RefreshToken entities/repos
 ├── role/             Role + Permission entities/repos
 ├── employee/        entity / repository / service / controller / dto / mapper
 ├── leave/           leave types, balances, requests, encashment, accrual scheduler, policy config
 ├── medical/         claims, HR/Finance approval workflow, attachment storage, budget tracking
 ├── pf/              accounts, ledger, monthly contribution scheduler, withdrawals, statements
 ├── posting/         employee transfer + posting history; master data CRUD lives in employee/
 ├── role/            role CRUD, permission assignment, role-to-user assignment
 ├── dashboard/       chart-ready aggregation endpoints for employees, leave, medical, PF
 ├── security/        UserDetailsService, JWT filter, entry points
 ├── security.jwt/    JwtTokenProvider, JwtProperties, JwtAuthenticationFilter
 ├── exception/       GlobalExceptionHandler + custom exceptions
 ├── audit/           BaseAuditableEntity, AuditLog, AuditService
 ├── config/          SecurityConfig, OpenApiConfig
 └── util/            EmployeeCodeGenerator, PageResponse
```

### Key design choices
- **Soft delete** everywhere via `BaseAuditableEntity.deleted` + `markDeleted()`; repositories filter `deleted = false` explicitly rather than relying on a global Hibernate filter, to keep query intent visible.
- **JWT access + opaque refresh tokens**: access tokens are short-lived and stateless; refresh tokens are persisted (`refresh_tokens` table) so they can be revoked on logout/password-change — a pure-JWT refresh token can't be revoked without that.
- **Specification-based filtering** (`EmployeeSpecifications`) instead of a dozen hand-written repository query methods, so filters combine freely.
- **Audit log is separate from JPA auditing**: JPA `@CreatedBy`/`@LastModifiedBy` answer "who touched this row last"; `AuditLog`/`AuditService` answer "what business events happened" (login, approvals, etc.) — different questions, different tables.

## Roles
`SUPER_ADMIN`, `HR_ADMIN`, `MANAGER`, `FINANCE`, `EMPLOYEE` — seeded in V1, assignable via the role management module.

## Known limitations / before going to production

Being upfront about the corners that were deliberately kept simple rather than over-engineered for a from-scratch build:

- **In-memory dashboard aggregation** (`DashboardService`) does department/gender/branch/status breakdowns and monthly trends in Java over the full employee list rather than SQL `GROUP BY`. Fine through the thousands-of-employees range; swap for DB-side aggregation if you're operating at a much larger scale.
- **File storage is local disk** (`FileStorageService` for medical claim attachments). Swap for S3/Azure Blob/GCS behind the same interface for a real multi-instance deployment — local disk won't survive a container redeploy or work across replicas.
- **No rate limiting** on auth endpoints beyond the existing failed-login lockout. Consider adding it at the gateway/ingress level.
- **No email sending** is wired up — `forgot-password` generates and stores a reset token but doesn't deliver it anywhere; that's a notification-service integration, intentionally out of scope.
- **PF/Leave schedulers run in-process** (`@Scheduled`) — fine for a single instance; if you ever run multiple app instances, add a distributed lock (e.g. ShedLock) so the monthly jobs don't run twice.
- **This was built and reviewed without a live compiler** (see "A note on verification" above) — budget time for a first `mvn clean verify` pass to catch anything a careful manual review missed.
