## Testing Strategy

This document describes the overall testing strategy for the ArtiCurated Order Management System.

### 1. Goals

- Validate **business rules** and **state machine transitions** for orders and returns.
- Ensure all **REST APIs** behave correctly (status codes, payloads, validation).
- Provide confidence that the **return/refund workflow** and **background jobs** work as intended.
- Keep tests **fast, isolated, and repeatable**.

---

### 2. Test Layers

We use a multi-layered approach:

1. **Unit Tests (Service Layer)**
   - Scope: Individual service classes with dependencies mocked.
   - Tools: JUnit 5, Mockito.
   - Focus:
     - Business rules (e.g., when an order can be cancelled).
     - Valid/invalid state transitions.
     - Exception throwing and handling.
     - Interactions with repositories and external clients.

2. **Controller Tests (Web Layer)**
   - Scope: REST controllers in isolation using `@WebMvcTest`.
   - Tools: JUnit 5, Spring MockMvc.
   - Focus:
     - HTTP method/URL mapping.
     - Request validation (`@Valid`) and error responses.
     - JSON payload shape and status codes (200, 201, 400, 404, etc.).

3. **Integration Tests (API Flows)**
   - Scope: End-to-end flows with the full Spring context and an in-memory database.
   - Tools: JUnit 5, Spring Boot Test (`@SpringBootTest`), MockMvc, H2.
   - Focus:
     - Full workflows:
       - Order → Payment → Fulfillment → Return → Refund.
     - State machine enforcement across layers.
     - Persistence and Flyway schema.
     - Global exception handling behavior.

---

### 3. Unit Test Strategy

#### 3.1 Services

- **OrderServiceTest**
  - Verify order creation, retrieval, update, and cancellation.
  - Validate that `StateMachineService` is called for status changes.
  - Ensure `BusinessRuleViolationException` is thrown for invalid cancellations.

- **PaymentServiceTest**
  - Verify payment processing flow with:
    - Successful payments.
    - Gateway failures.
    - Gateway exceptions.
    - Non-existent or invalid-state orders.
  - Confirm proper transitions and email notifications.

- **ReturnServiceTest**
  - Validate:
    - Return request creation only for `DELIVERED` orders.
    - Prevention of duplicate active returns.
    - Approval/rejection rules.
    - In-transit/received transitions.

- **RefundServiceTest**
  - Validate:
    - Refund creation only when returns are `RECEIVED`.
    - Single refund per return.
    - Processing success/failure paths with the payment gateway.
    - Correct transitions to `COMPLETED`.

- **StateMachineServiceTest**
  - Unit-test state transition rules for both order and return workflows.
  - Validate returned allowed transitions.

- **StateHistoryServiceTest**
  - Verify that all transition metadata is written correctly and retrieved as expected.

#### 3.2 Exception Handling

- Global exception handler is indirectly tested via:
  - Controller and integration tests (asserting `error`, `message`, `status` fields).

---

### 4. Controller Test Strategy

Using `@WebMvcTest`:

- **OrderControllerTest**
  - Test each endpoint with mocked `OrderService`.
  - Validate:
    - Validation failures (missing fields, invalid data).
    - Successful JSON responses.

- **PaymentControllerTest**
  - Similar approach for payment endpoints.

- **ReturnControllerTest** and **RefundControllerTest**
  - Ensure the workflow endpoints are wired correctly (approve, reject, in-transit, received, process refund).

Controllers themselves stay thin; most logic is verified at the service layer.

---

### 5. Integration Test Strategy

Using `@SpringBootTest` with `@AutoConfigureMockMvc` and the `test` profile:

- **Database**
  - H2 in-memory database (`application-test.yml`).
  - Flyway disabled in tests for speed; schema is derived from JPA or the migration script, depending on configuration.

- **Flows Covered**
  - **Order API Integration**
    - Create, fetch, update status, cancel, view history.
    - Validation and 404 behavior.
  - **Payment API Integration**
    - Process payment + subsequent order state change.
    - Validation and error responses for invalid data or missing orders.
  - **Return API Integration**
    - Full return lifecycle triggered via HTTP.
    - Ensures order must be `DELIVERED` to allow returns.
    - Validates state history entries.
  - **Refund API Integration**
    - Full path from return to refund completion.
    - Validates business rules and error responses (duplicate refunds, wrong state).

---

### 6. Background Jobs

Current scope:
- Jobs are relatively simple and rely on **service methods** that are already well-covered by unit tests.
- Strategy:
  - Keep job methods thin and delegate to services.
  - Optionally add targeted tests to verify that jobs call service methods for pending entities (can be added as future work).

---

### 7. Test Data & Isolation

- Each test class uses `@BeforeEach` to build its own test data.
- Unit tests use pure Mockito; no database is touched.
- Integration tests:
  - Use H2, with `@Transactional` so each test runs in a transaction that is rolled back afterward.
  - Have independent fixtures; tests do not depend on each other.

---

### 8. Tools & Commands

- **Run all tests**
  ```bash
  mvn test
  ```

- **Run only unit tests for a specific service**
  ```bash
  mvn test -Dtest=OrderServiceTest
  ```

- **Run all integration tests**
  ```bash
  mvn test -Dtest='*ApiIntegrationTest'
  ```

To get line coverage, you can add the JaCoCo Maven plugin and run:

```bash
mvn test jacoco:report
```

---

### 9. Coverage Expectations

While exact coverage numbers depend on running JaCoCo, the current strategy targets:

- **High coverage** for:
  - Service classes (business logic and state transitions).
  - Controller endpoints.
  - State history and state machine behavior.

- **Moderate coverage** for:
  - Integration flows (end-to-end scenarios).

Combined, this provides strong confidence that core workflows (order, payment, return, refund) behave correctly and that regressions will be caught early.

