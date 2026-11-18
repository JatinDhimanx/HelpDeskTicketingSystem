# Group 10 – Help Desk Ticketing System

A full-featured desktop Help Desk Ticketing System engineered in **Java (Swing GUI)** with **MySQL** via **JDBC**, structured under clean **MVC / OOP principles**, and packaged as both an **executable fat JAR** and a **native Windows application (`HelpDesk.exe`)**.

---

## Architecture & Design Patterns

The project enforces strict modularity and separation of concerns:

```
HelpDeskTicketingSystem/
├── src/main/java/helpdesk/
│   ├── core/           # Main entry point, AppConfig, EscalationManager
│   ├── database/       # DBConnection singleton, JDBC Connection pooling
│   ├── models/         # Person (base), User, Agent, Ticket, TicketNote, Assignable (interface)
│   ├── repositories/   # UserRepository, TicketRepository, AgentRepository (PreparedStatement CRUD)
│   ├── services/       # AuthService (BCrypt), TicketService, AgentService, ReportService, TicketEscalationService
│   ├── exceptions/     # TicketNotFoundException
│   ├── ui/             # Swing GUI (LoginWindow, Dashboards, Panels, PriorityRenderer, StatCards)
│   └── utils/          # UIConstants (design system tokens), UIHelper
├── src/main/resources/
│   ├── helpdesk.properties  # External configuration template
│   ├── schema.sql           # Database DDL with tables & seed records
│   └── app_icon.ico         # Application icon
├── target/
│   ├── dist/
│   │   ├── HelpDeskTicketingSystem.jar  # Standalone executable Fat JAR
│   │   └── HelpDeskApp/                 # Self-contained native Windows app (HelpDesk.exe)
└── pom.xml                  # Maven dependency descriptor
```

### Core OOP Principles Applied:
1. **Inheritance & Abstraction**:
   - `Person` is an abstract base class containing common attributes (`name`, `email`).
   - `User` extends `Person` with authentication details (`userId`, `passwordHash`, `role`).
   - `Agent` extends `Person` and implements the `Assignable` interface.
2. **Polymorphic Ticket Assignment**:
   - `Assignable` interface defines `void assignTicket(Ticket ticket)`.
   - `Agent` implements `assignTicket(Ticket ticket)` by binding its `agentId` to the ticket.
3. **Encapsulation**:
   - All model fields are private/protected with full getters, setters, and constructors.
4. **Security & Data Integrity**:
   - Passwords stored and authenticated using **BCrypt** hashing (`org.mindrot.jbcrypt`).
   - All SQL queries use `PreparedStatement` with parameterized inputs (SQL injection proof).
   - Strict `try-with-resources` throughout for deterministic connection closing.

---

## Database Schema (MySQL)

The system operates on 4 normalized relational tables in MySQL:

1. **`users`**: `user_id` (PK), `name`, `email` (UNIQUE), `password_hash`, `role` (`ADMIN`, `AGENT`, `USER`), `created_at`.
2. **`agents`**: `agent_id` (PK), `user_id` (FK to `users`), `available` (`BOOLEAN`).
3. **`tickets`**: `ticket_id` (PK), `user_id` (FK to `users`), `agent_id` (FK to `agents`, NULLABLE), `title`, `description`, `category`, `priority`, `status` (`OPEN`, `ASSIGNED`, `IN_PROGRESS`, `RESOLVED`), `created_at`, `updated_at`, `resolved_at`.
4. **`ticket_notes`**: `note_id` (PK), `ticket_id` (FK to `tickets`), `agent_id` (FK to `agents`), `note`, `created_at`.

### Default Seed Accounts (Password for all: `admin123`)
| Role | Email | Password | Access Level |
|---|---|---|---|
| **Admin** | `admin@helpdesk.com` | `admin123` | Full control: tickets, agent dispatch, aggregate reports |
| **Agent 1** | `agent1@helpdesk.com` | `admin123` | Agent workspace: assigned tickets, status lifecycle, notes |
| **Agent 2** | `agent2@helpdesk.com` | `admin123` | Agent workspace: assigned tickets, status lifecycle, notes |
| **User 1** | `user1@helpdesk.com` | `admin123` | User portal: submit tickets, track status, view agent replies |
| **User 2** | `user2@helpdesk.com` | `admin123` | User portal: submit tickets, track status, view agent replies |

---

## Configuration (`helpdesk.properties`)

The application supports zero-recompile external configuration. You can configure database credentials via:
1. `src/main/resources/helpdesk.properties` (bundled default)
2. An external `helpdesk.properties` in the working directory (takes precedence)
3. Environment variables (`DB_URL`, `DB_USER`, `DB_PASSWORD`)

```properties
# Cloud MySQL (Filess.io / Remote / Local)
db.url=jdbc:mysql://k5v6gm.h.filess.io:61001/helpdesk_dutyshopso?useSSL=true&allowPublicKeyRetrieval=true&serverTimezone=UTC
db.user=helpdesk_dutyshopso
db.password=YOUR_PASSWORD_HERE
escalation.check.interval.minutes=30
```

---

## Key Modules & Features

### 1. Authentication & Role-Based Routing (Task 04)
- Modern light-theme card login with password visibility toggle.
- Validates credentials against BCrypt hashes in MySQL.
- Routes users automatically to their designated dashboard:
  - **`AdminDashboard`** for `ADMIN`
  - **`AgentDashboard`** for `AGENT`
  - **`UserDashboard`** for `USER`

### 2. Ticket Creation & Classification (Tasks 05 & 06)
- Categories: `Technical`, `Network`, `Hardware`, `Software`, `Account/Login`.
- Priorities: `Low`, `Medium`, `High`, `Critical`.
- Visual color badges in tables for fast priority scanning.
- Dynamic filtering by Category, Priority, Status, and Search keywords (Title or Ticket ID).

### 3. Agent Assignment & Workload Tracking (Task 07)
- Admin interface showing tickets side-by-side with agent availability and workload counts.
- Enforces availability check: unavailable agents cannot receive ticket assignments.
- Dispatches tickets polymorphically using the `Assignable` interface.

### 4. Ticket Status Lifecycle (Task 08)
- Lifecycle: `OPEN` ➔ `ASSIGNED` ➔ `IN_PROGRESS` ➔ `RESOLVED`.
- Agents review full issue descriptions, update statuses, and log resolution audit notes.
- Quick Resolve feature timestamps `resolved_at` and captures closing remarks.
- Users track ticket progression and review responses in real-time.

### 5. Multithreaded Ticket Escalation Daemon (Task 09)
- `TicketEscalationService` runs on a background daemon thread (`TicketEscalationWorker`) independent of the Swing EDT.
- Periodically checks unresolved tickets against configurable age thresholds:
  - `Low` ➔ `Medium` after 72 hours
  - `Medium` ➔ `High` after 48 hours
  - `High` ➔ `Critical` after 24 hours
- Critical tickets are never re-escalated.
- Cleanly shuts down via JVM shutdown hooks and `EscalationManager.stop()`.

### 6. Resolution Reports & Analytics (Task 10)
- Real-time aggregate KPI cards: Total, Open, Assigned, In-Progress, Resolved.
- Average resolution time calculation in hours and days via SQL timestamp diffs.
- Category breakdown and priority distribution with percentages.
- Agent performance ranking (resolved ticket counts).
- Pending ticket priority queue.

---

## Execution Instructions

### Option 1: Running the Native Windows App (`HelpDesk.exe`)
No Java installation required on the client computer:
1. Navigate to:
   ```
   target/dist/HelpDeskApp/
   ```
2. Double-click **`HelpDesk.exe`**.

### Option 2: Running the Executable Fat JAR
Requires Java 17+ installed:
```bash
java -jar target/dist/HelpDeskTicketingSystem.jar
```

### Option 3: Compiling from Source
```bash
cd HelpDeskTicketingSystem
javac -cp "libs/*" -d target/classes @sources.txt
java -cp "target/classes;libs/*" helpdesk.core.Main
```
