# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build & Run

Use the Maven wrapper — never assume a system `mvn` is available.

```
# Windows
mvnw.cmd clean install
mvnw.cmd spring-boot:run

# Linux / macOS
./mvnw clean install
./mvnw spring-boot:run
```

## Environment Setup

Copy `.env.example` to `.env` before starting anything. The `.env` file is gitignored and required at runtime.

```
cp .env.example .env
```

`JWT_SECRET` must be at least 256 bits. Change both `JWT_SECRET` and `DB_PASSWORD` from the placeholder values.

## Database

Start PostgreSQL via Docker Compose before running the app or integration tests that hit the DB:

```
docker-compose up -d
```

`spring.jpa.hibernate.ddl-auto=validate` — the schema must exist before the app starts. Hibernate will not create tables. Flyway is planned but not yet added; schema migrations are currently manual.

## Testing

Integration tests use TestContainers (`@Testcontainers` + `@ServiceConnection`) and spin up their own PostgreSQL container automatically — no external database needed for `./mvnw test`.

`ArchitectureTest` calls `modules.verify()` (Spring Modulith). It will fail if module boundaries are violated (cross-module package access, circular dependencies between modules). Keep module interactions through published interfaces.
