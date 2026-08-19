# Smart Clinic Managemenrt System

## Database migrations

This project uses Flyway for MySQL schema and seed migrations, and MongoDB initialization scripts for the prescriptions collection.

### Start the databases

```bash
docker compose up -d
```

### Run the Spring Boot app

```bash
cd app
./mvnw spring-boot:run
```

Flyway will automatically apply the SQL migrations from [app/src/main/resources/db/migration](app/src/main/resources/db/migration) when the application starts. The MongoDB seed script is loaded from [mongo-init/01-seed-mongo.js](mongo-init/01-seed-mongo.js) when the Mongo container is initialized.