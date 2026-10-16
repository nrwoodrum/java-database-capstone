# Smart Clinic Management System

A Spring Boot-based clinic management application for managing doctors, patients, appointments, and prescriptions. The project uses MySQL for core relational data and MongoDB for prescription records, with Flyway handling database migrations.

## Overview

This application is designed for a healthcare workflow where:

- Admins manage clinic operations and doctor records
- Doctors manage patient appointments and availability
- Patients can view and manage their appointments and prescription data

## Tech Stack

- Java 17
- Spring Boot 3.5
- Maven
- MySQL 8
- MongoDB 8
- Flyway
- Thymeleaf + static frontend assets
- Docker Compose

## Features

- Admin login and role checks
- Doctor registration, update, deletion, and scheduling
- Patient appointment management
- Prescription storage and retrieval via MongoDB
- Automated MySQL schema setup with Flyway
- Local development environment with Docker

## Prerequisites

Before running the project, make sure you have:

- Java 17+
- Maven is optional; the included Maven Wrapper downloads it automatically.
- Docker and Docker Compose
- Git

## Quick Start

### 1. Clone the repository

```bash
git clone <your-repo-url>
cd java-database-capstone
```

### 2. Configure environment variables

Create a `.env` file in the project root **before starting Docker**. Docker Compose and the application read this file. You can copy the provided template:

```bash
cp .env.example .env
```

Fill in these required keys with nonempty local passwords and a random signing key:

```dotenv
MONGO_INITDB_ROOT_PASSWORD=replace-with-your-mongo-root-password
MONGO_APP_PASSWORD=replace-with-your-mongo-app-password
MYSQL_ROOT_PASSWORD=replace-with-your-mysql-root-password
MYSQL_PASSWORD=replace-with-your-mysql-app-password
JWT_SECRET=replace-with-a-long-random-signing-key
```

These keys are optional; Compose uses the following defaults when omitted:

```dotenv
MONGO_INITDB_ROOT_USERNAME=root
MONGO_APP_USERNAME=appuser
MONGO_APP_DATABASE=prescriptions
MYSQL_DATABASE=cms
MYSQL_USER=clinicdb
```

Generate the signing key with `openssl rand -base64 48` and paste the result into `JWT_SECRET`. It has no default: startup fails if it is missing. Never commit the generated value. Changing it invalidates existing login tokens; sign in again after rotation.

Keep `.env` local; it is ignored by Git. The committed `.env.example` contains no passwords. Database initialization runs only when Docker creates a new database volume; changing `.env` does not change credentials in existing databases.

### 3. Start the databases

```bash
docker compose up -d
```

This starts:

- MySQL on `localhost:3306`
- MongoDB on `localhost:27017`

### 4. Run the application

```bash
cd app
./mvnw spring-boot:run
```

By default, the app runs on:

- `http://localhost:8080`

## Demo walkthrough

After starting the app, open `http://localhost:8080`:

1. Select **Patient**, then **Create New User** to register and sign in. Alternatively, use the local demo patient **Jane Doe**: `jane.doe@example.com` / `passJane1`.
2. Pick a doctor, click **Book Now**, select a future date and available time, and confirm the booking.
3. Open **Appointments** in the header and select **All Appointments** or **Upcoming Appointments** to confirm the booking appears.
4. Once the doctor adds a prescription, open **Past Appointments → View Prescription** to read the instructions.

For the admin dashboard, select **Admin** and sign in with the local demo account `admin` / `admin@1234` to manage doctors.

For the doctor dashboard, select **Doctor** and sign in as **Dr. Adams** with `dr.adams@example.com` / `pass12345`; select the appointment date to add or view prescriptions.

These are **local demo credentials**, not passwords to reuse elsewhere. Flyway's `V2__seed_data.sql` loads them when that migration first runs (normally against a new MySQL volume). Existing database accounts may have been changed. The seed migration intentionally uses plaintext passwords for the demo and is kept unchanged to preserve Flyway checksums. A real deployment must store BCrypt hashes through Spring Security's `PasswordEncoder` and verify with `matches()`, never raw-string `equals()`.

## Database Setup

The project uses two database systems:

- MySQL: Stores clinic data such as doctors, patients, appointments, and related records
- MongoDB: Stores prescription data in the `prescriptions` database

### Flyway migrations

Flyway automatically applies SQL migration files from:

- [app/src/main/resources/db/migration](app/src/main/resources/db/migration)

### MongoDB seed data

MongoDB initialization scripts are located in:

- [mongo-init/01-seed-mongo.js](mongo-init/01-seed-mongo.js)

## Project Structure

```text
.
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/
│   │   │   ├── resources/
│   │   │   └── static/
│   │   └── test/
│   ├── pom.xml
│   └── mvnw
├── mongo-init/
├── docker-compose.yml
├── .env.example
├── README.md
└── LICENSE
```

## Useful Commands

Start database services:

```bash
docker compose up -d
```

Stop database services:

```bash
docker compose down
```

Run app with Maven:

```bash
cd app
./mvnw clean spring-boot:run
```

## Build, Tests, and Formatting

From `app/`, build and run unit/controller tests without starting MySQL, MongoDB, or Docker:

```bash
./mvnw clean package
```

The wrapper and plugins need internet access for their first download. Java 17 is the source target. The pinned formatter requires JDK 17 or newer to run.

Full Spring context and prescription database tests are opt-in integration tests (`*IT.java`). Configure `.env` and start the databases first, then run:

```bash
# From the project root:
docker compose up -d --wait
cd app
./mvnw -Pintegration verify
```

This runs the unit tests, packages the application, and runs the integration tests through Maven Failsafe. The default build excludes integration tests by naming convention.

Format Java sources and the POM, or check formatting:

```bash
# From app/:
./mvnw spotless:apply
./mvnw spotless:check
```

Formatting is also checked during Maven's `validate` phase. Commit the initial formatting changes separately from behavior changes when practical.

Frontend regression checks require Node.js 18+ and no npm dependencies. From the project root:

```bash
node app/src/test/js/prescription.test.mjs
node app/src/test/js/booking.test.mjs
node app/src/test/js/patientPrescription.test.mjs
node app/src/test/js/doctorTimeFilter.test.mjs
```

## Notes

- The app expects the database services to be running before Spring Boot starts.
- MySQL configuration is defined in [app/src/main/resources/application.properties](app/src/main/resources/application.properties).
- The project uses Docker for a consistent local development setup.

## License

This project is licensed under the Apache License, Version 2.0. See [LICENSE](LICENSE) for details.
