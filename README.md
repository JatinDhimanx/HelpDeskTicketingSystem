# Help Desk Ticketing System

A desktop help desk application for submitting, assigning, tracking, escalating, and resolving support tickets. The application is built with **Java 17**, **Swing**, **MySQL**, and **JDBC**, with role-based workflows for administrators, support agents, and end users.

## Features

- Secure login with BCrypt password hashing
- Role-based dashboards for administrators, agents, and users
- Ticket creation with category, priority, and status tracking
- Ticket assignment with agent availability and workload visibility
- Ticket notes and resolution remarks for support collaboration
- Automatic ticket escalation based on configurable age thresholds
- Reporting dashboards with ticket totals, status metrics, resolution times, and agent performance
- Prepared SQL statements and repository-based data access
- Fat JAR packaging through Maven or Gradle
- GitHub Actions workflow for building and publishing tagged releases

## Technology Stack

| Area | Technology |
| --- | --- |
| Language | Java 17 |
| User interface | Java Swing with FlatLaf |
| Database | MySQL 8+ |
| Data access | JDBC |
| Build tools | Maven and Gradle |
| Authentication | BCrypt |
| Logging | SLF4J Simple |

## Application Roles

| Role | Capabilities |
| --- | --- |
| **Administrator** | Manage tickets, assign agents, monitor workloads, and view reports |
| **Agent** | View assigned tickets, update statuses, add notes, and resolve requests |
| **User** | Create tickets, monitor progress, and read agent responses |

## Project Structure

```text
.
├── src/main/java/helpdesk/
│   ├── core/          # Application entry point and escalation management
│   ├── database/      # JDBC connection management
│   ├── exceptions/    # Application-specific exceptions
│   ├── models/        # Domain models and interfaces
│   ├── repositories/  # Database CRUD operations
│   ├── services/      # Authentication, ticket, agent, and report logic
│   ├── ui/            # Swing windows, dashboards, and reusable components
│   └── utils/         # UI constants and helper utilities
├── src/main/resources/
│   ├── helpdesk.properties  # Database and application configuration
│   └── schema.sql            # Database schema and seed data
├── pom.xml
├── build.gradle
└── run.bat
```

## Prerequisites

- Java Development Kit (JDK) 17 or newer
- MySQL 8 or a compatible MySQL database
- Maven 3.8+ or Gradle 8+
- A database user with permission to create and modify the application schema

## Database Setup

1. Create a database in MySQL.
2. Execute [`src/main/resources/schema.sql`](src/main/resources/schema.sql) against that database.
3. Configure the connection before starting the application.

For local development, create an external `helpdesk.properties` file in the project working directory. An external file takes precedence over the bundled resource configuration.

```properties
db.url=jdbc:mysql://localhost:3306/helpdesk?useSSL=false&serverTimezone=UTC
db.user=YOUR_DATABASE_USER
db.password=YOUR_DATABASE_PASSWORD
escalation.check.interval.minutes=30
```

Do not commit real database passwords or other secrets. Use environment-specific configuration and replace any credentials that may have been exposed in a shared repository.

## Build and Run

### Maven

Build the application and its dependency-inclusive JAR:

```bash
mvn clean package
java -jar target/HelpDeskTicketingSystem-1.0-SNAPSHOT-jar-with-dependencies.jar
```

### Gradle

Build and run with Gradle:

```bash
gradle clean build
gradle run
```

The application entry point is `helpdesk.core.Main`.

### Windows launcher

After building the Maven artifact, `run.bat` can be used to launch the packaged application from Windows. The script expects the executable JAR at:

```text
target/dist/HelpDeskTicketingSystem.jar
```

If required, copy or rename the Maven fat JAR to that location before running the script.

## Default Seed Accounts

The schema includes development accounts for testing. The default password is `admin123` for each account.

| Role | Email |
| --- | --- |
| Administrator | `admin@helpdesk.com` |
| Agent | `agent1@helpdesk.com` |
| Agent | `agent2@helpdesk.com` |
| User | `user1@helpdesk.com` |
| User | `user2@helpdesk.com` |

These credentials are intended for local development only. Change or remove seeded passwords before deploying the application in a production environment.

## Ticket Lifecycle and Escalation

Tickets follow this workflow:

```text
OPEN → ASSIGNED → IN_PROGRESS → RESOLVED
```

The background escalation worker periodically evaluates unresolved tickets and raises their priority when they exceed the configured age threshold:

- Low → Medium after 72 hours
- Medium → High after 48 hours
- High → Critical after 24 hours

The check interval is controlled by `escalation.check.interval.minutes`.

## Releases

Pushing a tag that starts with `v` triggers the GitHub Actions release workflow. The workflow builds the project with JDK 17, packages the dependency-inclusive JAR, and attaches it to a GitHub Release.

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Security Notes

- Passwords are stored as BCrypt hashes.
- SQL operations use parameterized `PreparedStatement` queries.
- Keep database configuration outside source control whenever possible.
- Use strong, unique credentials for non-development environments.
- Review database permissions and enable TLS when connecting to a remote MySQL server.

## Contributing

1. Create a feature branch from `main`.
2. Make focused changes that follow the existing package and naming conventions.
3. Build the project locally with `mvn clean package` or `gradle clean build`.
4. Open a pull request describing the behavior change and validation performed.

## License

No license has currently been specified for this repository. Add a license file before distributing or reusing the project outside its intended development context.
