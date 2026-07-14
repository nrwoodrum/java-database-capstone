This Spring Boot application follows a layered architecture that uses both MVC and REST controllers to handle different parts of the system. Thymeleaf templates are used to render the Admin and Doctor dashboards, providing server-side views for management-related tasks. REST APIs are used for the remaining modules, allowing application data and functionality to be accessed through structured HTTP endpoints.

The application connects to two databases based on the type of data being stored. MySQL manages patient, doctor, appointment, and administrator information using JPA entities and repositories, while MongoDB stores prescription data using document models. All controller requests are routed through a shared service layer that contains the application's business logic and delegates database operations to the appropriate repository. This separation of controllers, services, and repositories keeps the application organized, maintainable, and easier to expand.

1. User Request: The user interacts with the application through a Thymeleaf dashboard or another client and sends a request to the Spring Boot application.
2. Controller Processing: The request is received by either an MVC controller for the Admin and Doctor dashboards or a REST controller for the other application modules.
3. Service Layer Routing: The controller sends the request to the common service layer, where the application's business logic is processed.
4. Repository Selection: The service layer determines which repository is needed based on the type of data being accessed or modified.
5. Database Interaction: JPA repositories communicate with MySQL for patient, doctor, appointment, and admin data, while MongoDB repositories interact with MongoDB for prescription data.
6. Data Returned: The selected repository retrieves or updates the data and returns the result to the service layer, which then sends the processed result back to the controller.
7. Response to User: The controller returns the final response as either a rendered Thymeleaf page for the Admin or Doctor dashboard or a REST API response for the other modules.
