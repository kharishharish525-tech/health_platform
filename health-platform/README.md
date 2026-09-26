# Rural Health Network Platform

AI-Based Patient Health Record & Telemedicine Triage Management System.
A Spring Boot + MySQL backend **with a built-in website front-end**,
covering secure patient intake, automated rule-based triage queue sorting,
diagnostic service billing with inventory tracking, and departmental
budget/accounting reports.

## Stack
- Java 17
- Spring Boot 3.3.4 (Web, Data JPA, Validation, **Security**)
- MySQL 8
- Maven
- Plain HTML/CSS/JS website (no build step, served directly by Spring Boot)

## What's included

**Website** (open http://localhost:8080 after starting):
- **Dashboard** — live stats (patients, doctors, triage queue, emergency
  count, today's revenue, unpaid invoices, low-stock alerts) + a snapshot
  of the triage queue
- **Patients** — registration form + patient list
- **Triage Queue** — submit vitals/symptoms, see the computed score and
  priority instantly, and watch the auto-refreshing waiting queue sorted
  by urgency
- **Billing** — create invoices, record payments, see invoice history and
  low-stock products
- **Budgets** — allocate departmental budgets, view/download utilization
  reports
- **Reports** — Profit & Loss and Balance Sheet, viewable on-screen or as
  downloadable CSV

**Extra backend features added beyond the original 5 stages:**
- **Inventory tracking** — physical products/supplies have a stock
  quantity and reorder level; billing automatically deducts stock,
  receiving a purchase order automatically restocks it, and a
  `/api/products/low-stock` endpoint flags anything at or below its
  reorder level
- **Dashboard summary API** (`GET /api/dashboard/summary`) aggregating the
  stats above in one call
- **CSV export** for the P&L, Balance Sheet, and Budget Utilization
  reports
- **Basic authentication** on all `/api/**` endpoints (see Security below)
- **Global error handling** — API errors come back as clean, consistent
  JSON instead of raw stack traces
- **Demo data seeding** — on first run, sample departments, doctors, and
  billable products/services are inserted automatically so the website
  isn't empty (see `DataSeeder.java`; safe to delete if you don't want
  this)

## Prerequisites (on your own machine — NOT this sandbox)
1. **JDK 17+** installed (`java -version`)
2. **Maven 3.8+** installed (`mvn -version`), or use an IDE (IntelliJ/Eclipse/VS Code) that has its own bundled Maven
3. **MySQL 8** running locally (or reachable), with a user that can create databases

> This project could not be compiled inside the assistant's sandbox because
> outbound network access there is restricted and does not allow reaching
> Maven Central (`repo.maven.apache.org`) to download Spring Boot's
> dependencies. Every file has been manually checked for consistency, but
> you must run the actual build on a machine with normal internet access.

## Quickest way to run it — double-click / one command

Three auto-run scripts are included at the project root so you don't have
to remember any Maven commands:

| Script | Platform | What it does |
|---|---|---|
| `run.bat` | Windows Command Prompt | Checks Java/Maven are installed, then runs `mvn spring-boot:run` |
| `build-and-run.bat` | Windows Command Prompt | Builds a packaged jar (`mvn clean package`) and runs it with `java -jar` — closer to how you'd deploy it |
| `run.sh` | macOS / Linux | Same checks as `run.bat`, then `mvn spring-boot:run` |

**On Windows:** open Command Prompt in the project folder and run:
```cmd
run.bat
```
or just double-click `run.bat` in File Explorer. It will check that Java
and Maven are installed, remind you to have MySQL running, then start the
server. When it says the app has started, open **http://localhost:8080**
in your browser.

**On macOS/Linux:**
```bash
./run.sh
```

Each script checks for Java and Maven first and gives you a clear message
(with install suggestions) if either is missing, instead of failing with a
confusing Maven error.

## Stage 1 — Database

The schema will be **auto-created by Hibernate** on first run
(`spring.jpa.hibernate.ddl-auto=update` in `application.properties`), so
you don't have to run anything by hand — just make sure a MySQL server is
running and the credentials in `application.properties` are correct
(default: `root` / `root`, `localhost:3306`). The app auto-creates the
`rural_health_db` schema itself.

If you'd rather set the schema up manually first (and inspect the table
design), a standalone script is included:

```bash
mysql -u root -p < src/main/resources/schema.sql
```

If you use this script, change `spring.jpa.hibernate.ddl-auto=update` to
`validate` in `application.properties` afterwards so Hibernate doesn't try
to alter what you already created.

## Stage 2 — Run the Spring Boot project

Easiest: use `run.bat` (Windows) or `run.sh` (Mac/Linux) as shown above.

Manually, the equivalent commands are:

```bash
cd health-platform
mvn spring-boot:run
```

Or build a jar and run it (what `build-and-run.bat` does):

```bash
mvn clean package -DskipTests
java -jar target/health-platform.jar
```

The website and API both start on **http://localhost:8080**.

## Security

All `/api/**` endpoints require HTTP Basic authentication:

- **Username:** `admin`
- **Password:** `admin123`

The website's JavaScript (`static/js/api.js`) already sends these
credentials automatically, so the site works out of the box. The static
pages themselves (`/`, `*.html`, `/css/**`, `/js/**`) are public so they
can load before any login happens.

To change the credentials, edit both:
1. `src/main/java/com/ruralhealth/platform/config/SecurityConfig.java`
   (the `userDetailsService()` bean)
2. `src/main/resources/static/js/api.js` (`ADMIN_USER` / `ADMIN_PASS`)

This is intentionally simple (single hardcoded admin account) for a small
clinic deployment. Before handling real patient data in production,
replace it with proper per-user accounts, hashed passwords, and
role-based access control.

## Stage 3 — REST APIs (Patient/Doctor/etc. CRUD)

| Resource      | Endpoints |
|---------------|-----------|
| Patients      | `GET/POST /api/patients`, `GET/PUT/DELETE /api/patients/{id}` |
| Doctors       | `GET/POST /api/doctors`, `GET/PUT/DELETE /api/doctors/{id}` |
| Departments   | `GET/POST /api/departments`, `PUT/DELETE /api/departments/{id}` |
| Products/Services | `GET/POST /api/products`, `PUT/DELETE /api/products/{id}`, `GET /api/products/low-stock` |
| Dashboard | `GET /api/dashboard/summary` |
| Consultations | `GET /api/consultations`, `GET /api/consultations/{id}`, `GET /api/consultations/patient/{patientId}`, `PATCH /api/consultations/{id}/status` |
| Suppliers     | `GET/POST /api/suppliers` |
| Purchase Orders | `GET/POST /api/purchase-orders`, `PATCH /api/purchase-orders/{id}/receive` |

Example — create a patient:
```bash
curl -X POST http://localhost:8080/api/patients \
  -H "Content-Type: application/json" \
  -d '{"fullName":"Asha Devi","dateOfBirth":"1990-05-12","gender":"F","phone":"9876543210","address":"Pollachi","bloodGroup":"O+","emergencyContact":"9876500000"}'
```

## Stage 4 — AI Triage (rule-based)

`POST /api/triage/evaluate` accepts symptoms + vitals, saves a `Consultation`
record, and returns a computed triage score/priority
(`EMERGENCY` / `URGENT` / `NORMAL`). The scoring logic (in
`TriageService.java`) is a simplified early-warning score built from heart
rate, blood pressure, SpO2, temperature, respiratory rate, plus red/amber
keyword flags in the free-text symptoms.

```bash
curl -X POST http://localhost:8080/api/triage/evaluate \
  -H "Content-Type: application/json" \
  -d '{
    "patientId": 1,
    "departmentId": 1,
    "symptoms": "severe chest pain and dizziness",
    "heartRate": 128,
    "systolicBp": 88,
    "diastolicBp": 60,
    "temperatureCelsius": 37.9,
    "spo2": 91,
    "respiratoryRate": 26
  }'
```

`GET /api/triage/queue` returns the current waiting-room queue, ordered by
triage score (most urgent first) — this is the automated triage queue
sorting requirement.

**Swapping in an ML model later:** replace the body of
`TriageService.evaluate()` with a call to your trained model (a REST call
to a Python/FastAPI microservice, or a Java ML runtime like ONNX/DJL). The
method signature (`TriageRequest` in, `TriageResponse` out) and the
`Consultation` persistence step can stay exactly the same, so no
controller or other service needs to change.

## Billing (diagnostic services)

- `POST /api/invoices` — create an invoice from one or more billable
  products/services (uses `BillingService`, which also posts a journal
  entry: Debit Accounts Receivable / Credit Revenue).
- `POST /api/payments` — record a payment against an invoice (posts
  Debit Cash / Credit Accounts Receivable, and updates invoice status to
  `PARTIAL` or `PAID`).

```bash
curl -X POST http://localhost:8080/api/invoices \
  -H "Content-Type: application/json" \
  -d '{"patientId":1,"consultationId":1,"lines":[{"productId":1,"quantity":1}]}'

curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{"invoiceId":1,"amount":500,"method":"CASH"}'
```

## Stage 5 — Reports

| Report | Endpoint |
|--------|----------|
| Profit & Loss | `GET /api/reports/profit-loss?year=2026` |
| Balance Sheet | `GET /api/reports/balance-sheet` |
| Budget Utilization | `GET /api/budgets/utilization?year=2026&month=9` |

The P&L and Balance Sheet are derived live from the double-entry
`chart_of_accounts` / `journal_entries` / `journal_lines` ledger — every
invoice, payment, and received purchase order automatically posts a
balanced journal entry (see `JournalService.java`), so these reports stay
accurate without any separate reconciliation step.

Budget workflow:
```bash
# Allocate a department's monthly budget
curl -X POST http://localhost:8080/api/budgets \
  -H "Content-Type: application/json" \
  -d '{"department":{"departmentId":1},"fiscalYear":2026,"fiscalMonth":9,"allocatedAmount":50000}'

# Record spend against it (e.g. after a purchase order)
curl -X PATCH http://localhost:8080/api/budgets/1/spend \
  -H "Content-Type: application/json" -d '{"amount":1200}'
```

## Project layout

```
run.bat / build-and-run.bat / run.sh   ← auto-run scripts (see above)

src/main/java/com/ruralhealth/platform/
  entity/       15 JPA entities (one per table)
  repository/   Spring Data JPA repositories
  service/      TriageService, BillingService, BudgetService, JournalService, ReportService
  controller/   REST controllers (Patient, Doctor, Department, Product,
                Triage, Consultation, Invoice, Payment, Supplier,
                PurchaseOrder, Budget, Report, Dashboard)
  dto/          Request/response payloads (Triage, reports, dashboard)
  config/       SecurityConfig, GlobalExceptionHandler, DataSeeder

src/main/resources/
  application.properties   DB connection + JPA settings
  schema.sql               Optional manual Stage-1 reference script
  static/                  The website (served directly by Spring Boot)
    index.html, patients.html, triage.html, billing.html, budget.html, reports.html
    css/style.css
    js/api.js (shared fetch helper), + one .js file per page
```

## Notes / things to adjust before production use

- **Security**: there is no authentication/authorization layer yet. For a
  real deployment, add Spring Security (JWT or session-based) in front of
  all `/api/**` endpoints, since this handles patient health data.
- **Validation**: add `@Valid` + Bean Validation annotations on the
  request DTOs/entities for stricter input checking.
- **Database credentials**: change the default `root/root` credentials in
  `application.properties` before deploying anywhere shared.
