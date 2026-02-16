## Chat History & Design Journey

This document summarizes how the ArtiCurated Order Management System was designed and implemented with the help of an AI coding assistant.

### 1. Initial Requirements & High-Level Design

- We started from a business brief describing **ArtiCurated**, a boutique marketplace for high-value artisanal goods.
- Core needs:
  - Manage complex **order lifecycle** with strict state transitions.
  - Support a **multi-step returns workflow** with approvals and refunds.
  - Maintain a detailed **audit trail** of all state changes.
- With the assistant, we:
  - Produced a **high-level architecture diagram** showing controllers, services, repositories, integrations, and PostgreSQL.
  - Defined two state machines:
    - Order: `PENDING_PAYMENT → PAID → PROCESSING_IN_WAREHOUSE → SHIPPED → DELIVERED` (+ `CANCELLED` paths).
    - Return: `REQUESTED → APPROVED/REJECTED → IN_TRANSIT → RECEIVED → COMPLETED`.
  - Decided to log all transitions into a dedicated `state_transitions` table using a centralized `StateMachineService`.

### 2. Low-Level Design & Domain Modeling

- Next, we worked with the assistant to create a detailed **low-level design**, including:
  - Package structure (`controller`, `service`, `repository`, `model`, `integration`, `scheduler`, `config`).
  - JPA entities (`Order`, `OrderItem`, `Payment`, `Return`, `Refund`, `StateTransition`) and their relationships.
  - Enum types for all workflow states (`OrderStatus`, `ReturnStatus`, `PaymentStatus`, `RefundStatus`, `EntityType`).
  - DTOs for requests and responses, keeping API models decoupled from persistence.
- The assistant helped ensure:
  - **Transaction boundaries** were clearly identified (`@Transactional` on service methods).
  - Database schema was consistent, with **Flyway migrations** defining tables, indexes, and constraints.

### 3. Implementation & Key Decisions

With the design in place, we used the assistant to generate and iteratively refine the implementation:

- **Services & Workflows**
  - `OrderService`, `PaymentService`, `ReturnService`, `RefundService` encapsulate business logic.
  - `StateMachineService` enforces valid transitions and throws `InvalidStateTransitionException` on invalid moves.
  - `StateHistoryService` writes all transitions to `state_transitions` for auditing.
  - Decision: keep state-machine logic **explicit** (switch expressions) instead of over-abstracting with a heavy state machine framework — this keeps the codebase transparent and easier to review.

- **Error Handling**
  - The assistant guided the introduction of a **GlobalExceptionHandler** using `@RestControllerAdvice`.
  - We added custom exceptions (`OrderNotFoundException`, `ReturnNotAllowedException`, `BusinessRuleViolationException`, etc.) and mapped them to clear HTTP responses.
  - Decision: standardize error responses via `ErrorResponse` DTO to ensure consistent client experience.

- **Background Jobs**
  - Implemented `PaymentStatusCheckerJob` and `RefundProcessorJob` using `@Scheduled`.
  - Decision: keep job logic **simple and idempotent**, deferring more advanced queueing or retry strategies to future iterations.

- **Integrations**
  - `PaymentGatewayClient` and `EmailClient` were implemented as **simulated adapters** (no real external calls), to make the system runnable out of the box.
  - Decision: define clear interfaces and DTOs now so they can be replaced with real gateway/email providers later without changing business services.

### 4. Testing Strategy & Evolution

- The assistant helped design a **layered testing approach**:
  - **Unit tests** with JUnit + Mockito:
    - For services: verify business rules, state transitions, and interactions with repositories.
    - For state management: validate allowed/denied transitions and history logging.
  - **Controller tests** using `@WebMvcTest` and `MockMvc`:
    - Validate request/response shapes and HTTP status codes.
  - **Integration tests** using `@SpringBootTest` + H2:
    - End-to-end flows covering order → payment → return → refund.
- Key decision: use **H2** in memory for test profile and disable Flyway there to keep integration tests fast and self-contained.

### 5. API & Documentation

- With the assistant, we created:
  - `Architecture.md` – high-level architecture, flows, and database schema.
  - `PROJECT_STRUCTURE.md` – explanation of each folder and module.
  - `API-SPECIFICATION.yml` – OpenAPI 3–style spec for all endpoints.
  - `README.md` – setup instructions, API overview, and sample curl commands.
- Decision: keep docs **text-first and self-contained** so reviewers do not need external tools (e.g. Postman) to understand the system.

### 6. Docker & Environment

- The assistant helped author a **docker-compose.yml** that:
  - Spins up a `postgres:15` container with the expected database.
  - Runs the app using a `maven:3.9-eclipse-temurin-21` image with the project mounted, executing `mvn spring-boot:run`.
- Decision: avoid an additional Dockerfile to keep the setup minimal, while still allowing one-command startup for validation.

### 7. Key Trade-offs

- **Simplicity vs. Extensibility**
  - Chose explicit switch-based state logic over a more complex state machine framework configuration.
  - Used in-memory simulations for payment and email integrations to keep the project runnable without external dependencies.

- **Performance vs. Observability**
  - Always log state transitions to `state_transitions`, which slightly increases write volume but provides strong observability and audit capabilities.

- **Scope vs. Time**
  - Focused on core flows (order, payment, return, refund) and essential error handling, leaving advanced features (multi-tenant support, complex approval hierarchies, analytics) for future work.

### 8. How the AI Assistant Helped

Throughout the project, the AI assistant was used to:
- Brainstorm architecture and refine it into clear diagrams and documents.
- Generate boilerplate for entities, repositories, DTOs, and controllers.
- Sketch and then refine service logic, especially state transitions and validation rules.
- Design and implement a layered testing approach (unit, controller, integration).
- Create documentation files required for submission and ensure consistency between code and docs.

The collaboration allowed rapid iteration while still keeping the design well-structured, documented, and testable.

