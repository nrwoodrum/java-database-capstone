# Smart Clinic Management System

A Spring Boot-based clinic management application for managing doctors, patients, appointments, and prescriptions. The project uses MySQL for core relational data and MongoDB for prescription records, with Flyway handling database migrations.

## Overview

This application is designed for a healthcare workflow where:

- Admins manage clinic operations and doctor records
- Doctors manage patient appointments and availability
- Patients can view and manage their appointments and prescription data

## Tech Stack

- Java 17
- Spring Boot 3.4
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
- Maven
- Docker and Docker Compose
- Git

## Quick Start

### 1. Clone the repository

```bash
git clone <your-repo-url>
cd java-database-capstone
```

### 2. Configure environment variables

The project reads configuration from a local `.env` file in the project root. A sample configuration is already present in the repository and is used by Docker Compose.

```bash
# required values used by docker-compose.yml
MONGO_INITDB_ROOT_USERNAME=root
MONGO_INITDB_ROOT_PASSWORD=your-root-password
MONGO_APP_USERNAME=appuser
MONGO_APP_PASSWORD=your-mongo-password
MONGO_APP_DATABASE=prescriptions

MYSQL_ROOT_PASSWORD=rootpass
MYSQL_DATABASE=cms
MYSQL_USER=clinicdb
MYSQL_PASSWORD=clinicdb
```

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
├── .env
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

## Notes

- The app expects the database services to be running before Spring Boot starts.
- MySQL configuration is defined in [app/src/main/resources/application.properties](app/src/main/resources/application.properties).
- The project uses Docker for a consistent local development setup.

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.