# Application Architecture

This Spring Boot application is built around a hybrid MVC and REST architecture. The Admin and Doctor dashboards are rendered server-side using Thymeleaf templates through MVC controllers, giving those interfaces a traditional page-based flow. All other modules including patient management, appointments, and prescriptions are exposed through RESTful APIs, allowing them to be consumed by any front-end client or external service.

The application integrates two separate databases to handle distinct data concerns. MySQL stores structured, relational data such as patient records, doctor profiles, appointments, and admin accounts, managed through JPA entities and Spring Data repositories. MongoDB stores prescription data as flexible documents, which suits the variable structure of medical prescription records. All controllers delegate business logic to a shared service layer, which acts as the single point of coordination between the web layer and the underlying repositories keeping the codebase modular and each layer focused on a single responsibility.

## Data Flow

1. The user accesses a page or sends a request — either through the Admin/Doctor dashboard UI or via a REST API endpoint from another module.
2. The request is received by the appropriate controller — an MVC controller for Thymeleaf-rendered dashboard pages, or a REST controller for API-based interactions.
3. The controller validates the incoming request data and delegates the business operation to the corresponding service in the service layer.
4. The service layer applies any business logic — such as validation rules, data transformation, or coordination between multiple entities — before interacting with the data layer.
5. The service calls the appropriate repository — a JPA repository for MySQL data (patients, doctors, appointments, admins) or a MongoDB repository for prescription documents.
6. The repository executes the query or persistence operation against the database and returns the result back up to the service layer.
7. The service returns the processed data to the controller, which either renders a Thymeleaf template (for dashboard views) or serializes the result as a JSON response (for REST API calls).
