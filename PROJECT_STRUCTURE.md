## Project Structure

This document explains the layout of the ArtiCurated Order Management System and the purpose of each key folder and module.

### Root

- **pom.xml**  
  Maven build descriptor defining dependencies (Spring Boot, Spring Data JPA, Spring State Machine, Flyway, PostgreSQL, testing libraries) and build plugins.

- **docker-compose.yml**  
  Orchestrates the Spring Boot application and PostgreSQL database for local development and validation.

- **README.md**  
  High-level overview, setup instructions, and quick API examples.

- **Architecture.md**  
  High-level architecture, flows, and database schema description.

- **API-SPECIFICATION.yml**  
  OpenAPI-style specification describing the REST endpoints, request/response payloads, and error formats.

- **PROJECT_STRUCTURE.md**  
  This file – explains the project layout.

- **CHAT_HISTORY.md**  
  Summary of the design and implementation journey with the AI assistant.

- **TESTING_STRATEGY.md**  
  Testing approach for unit, integration, and API tests.

- **TEST_REPORT.md**  
  Summary of the implemented tests and coverage at a high level.

---

### `src/main/java/com/articurated/ordermanagement`

The main application code is organized by responsibility.

- **OrderManagementApplication.java**  
  Spring Boot entry point. Enables scheduling for background jobs.

#### `config/`

- **AsyncConfig.java**  
  Configures the `@Async` executor (thread pool) for email sending and other async tasks.

- **WebConfig.java**  
  Configures CORS and general MVC settings for the REST API.

#### `controller/`

Spring MVC REST controllers. These are thin layers that:
- Validate input (`@Valid`)
- Delegate to services
- Map HTTP status codes and response bodies

- **OrderController.java**  
  CRUD and lifecycle operations for orders:
  - Create orders
  - Fetch orders by ID, order number, and customer
  - Update order status
  - Cancel orders
  - Fetch order state history

- **PaymentController.java**  
  Endpoints to:
  - Process payments
  - Fetch payments by ID or order
  - Receive webhooks from the payment gateway (stubbed)

- **ReturnController.java**  
  Manages return requests and their workflow:
  - Create return request
  - Approve/reject returns
  - Mark returns as in transit / received
  - Fetch returns and their state history

- **RefundController.java**  
  Manages refunds:
  - Create refund for a return
  - Process refund
  - Fetch refund by ID or by return ID

#### `service/`

Business logic layer. Services encapsulate workflows, state transitions, and interaction with repositories/integrations.

- **OrderService.java**  
  - Creates orders and associated `OrderItem` records  
  - Calculates totals  
  - Orchestrates order state transitions via `StateMachineService`  
  - Logs state history and sends notifications

- **PaymentService.java**  
  - Validates that orders are in `PENDING_PAYMENT`  
  - Creates `Payment` records  
  - Integrates with `PaymentGatewayClient`  
  - Transitions orders to `PAID` on success  
  - Handles payment failures and exceptions

- **ReturnService.java**  
  - Validates that orders are `DELIVERED`  
  - Creates `Return` records  
  - Enforces the return state machine (REQUESTED → APPROVED/REJECTED → IN_TRANSIT → RECEIVED → COMPLETED) via `StateMachineService`  
  - Sends email notifications and logs state transitions

- **RefundService.java**  
  - Validates that returns are in `RECEIVED` state  
  - Creates and processes `Refund` records  
  - Calls `PaymentGatewayClient` to trigger refunds  
  - Transitions returns to `COMPLETED` on success

- **StateMachineService.java**  
  - Centralized order and return state machine logic  
  - Validates allowed transitions and throws `InvalidStateTransitionException` on invalid moves  
  - Writes to `StateHistoryService` for each transition.

- **StateHistoryService.java**  
  - Persists `StateTransition` records  
  - Exposes querying APIs for histories and latest transitions.

- **EmailService.java**  
  - Asynchronous notification layer built on top of `EmailClient`  
  - Sends order confirmations, status updates, return updates, and refund confirmations.

#### `repository/`

Spring Data JPA repositories for persistence. They are thin data-access abstractions:

- **OrderRepository.java** – `orders` table operations.  
- **PaymentRepository.java** – `payments` table operations.  
- **ReturnRepository.java** – `returns` table operations.  
- **RefundRepository.java** – `refunds` table operations.  
- **StateTransitionRepository.java** – `state_transitions` audit log operations.

#### `model/entity/`

JPA entities mapped directly to PostgreSQL tables:

- **Order.java / OrderItem.java** – Order header and line items.  
- **Payment.java** – Payment attempts and status.  
- **Return.java** – Return requests and workflow-related timestamps.  
- **Refund.java** – Refund records.  
- **StateTransition.java** – Historical log of every state change for orders and returns.

#### `model/enums/`

Enum types modeling state and classification:

- **OrderStatus** – PENDING_PAYMENT, PAID, PROCESSING_IN_WAREHOUSE, SHIPPED, DELIVERED, CANCELLED.  
- **ReturnStatus** – REQUESTED, APPROVED, REJECTED, IN_TRANSIT, RECEIVED, COMPLETED.  
- **PaymentStatus**, **RefundStatus** – Simple lifecycle states.  
- **EntityType** – ORDER or RETURN for the audit log.

#### `model/dto/request` & `model/dto/response`

Request and response DTOs shaping the public API:

- Request DTOs (`CreateOrderRequest`, `ProcessPaymentRequest`, `CreateReturnRequest`, `ApproveReturnRequest`, `UpdateOrderStatusRequest`)  
  - Include `jakarta.validation` annotations for input validation.

- Response DTOs (`OrderResponse`, `PaymentResponse`, `ReturnResponse`, `RefundResponse`, `StateHistoryResponse`, `OrderItemResponse`, `ErrorResponse`)  
  - Decouple API payloads from entity internals.

#### `model/exception/`

Domain-specific exceptions and global error handling:

- **OrderNotFoundException**, **ReturnNotFoundException**, **RefundNotFoundException**, **PaymentNotFoundException** – 404s.  
- **InvalidStateTransitionException** – invalid workflow transitions.  
- **ReturnNotAllowedException**, **BusinessRuleViolationException** – business rule violations.  
- **PaymentProcessingException** – technical issues when calling the payment gateway.  
- **GlobalExceptionHandler** – centralized `@RestControllerAdvice` mapping exceptions to consistent JSON error responses.

#### `integration/`

Infrastructure-facing clients:

- **PaymentGatewayClient.java**  
  Simulated payment/refund client; in production replaced by a real gateway integration.

- **EmailClient.java**  
  Simulated email sending (currently logs messages); in production backed by SMTP or a cloud email provider.

#### `scheduler/`

Background jobs driven by `@Scheduled`:

- **PaymentStatusCheckerJob.java** – periodically checks pending payments and updates their status.  
- **RefundProcessorJob.java** – periodically processes pending refunds.

---

### `src/main/resources`

- **application.yml**  
  Runtime configuration (PostgreSQL datasource, Flyway, logging, payment/email config).

- **db/migration/V1__create_initial_schema.sql**  
  Flyway migration that creates the core tables (`orders`, `order_items`, `payments`, `returns`, `refunds`, `state_transitions`) with indexes and constraints.

---

### `src/test/java`

Unit, controller, and integration tests.

- **service/** – JUnit + Mockito unit tests for service classes.  
- **controller/** – `@WebMvcTest` tests for controllers.  
- **integration/** – `@SpringBootTest` + MockMvc integration tests for full API flows.

### `src/test/resources`

- **application-test.yml**  
  Test profile configuration using an H2 in-memory database and disabling Flyway for fast, isolated tests.

---

### How it all fits together

- Controllers expose a clear REST API surface.  
- Services encode business rules, invoke state machines, persist entities via repositories, and integrate with external systems.  
- Entities and repositories encapsulate persistence.  
- Schedulers and integrations operate in the background while sharing the same service layer.  
- State transitions are always funneled through `StateMachineService` and recorded via `StateHistoryService`, ensuring consistent auditing across the system.

