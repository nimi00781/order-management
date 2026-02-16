## Test Report

This document summarizes the current automated test coverage for the ArtiCurated Order Management System.

> Note: Exact line/branch coverage percentages depend on running a coverage tool (e.g. JaCoCo).
> The summary below focuses on which areas are covered and how.

---

### 1. Test Suites

The project includes three main categories of automated tests:

1. **Unit Tests (Service & Utility Layer)**  
   Located under `src/test/java/com/articurated/ordermanagement/service`.

2. **Controller Tests (Web Layer)**  
   Located under `src/test/java/com/articurated/ordermanagement/controller`.

3. **Integration / API Tests**  
   Located under `src/test/java/com/articurated/ordermanagement/integration`.

---

### 2. Unit Test Coverage (Services)

#### OrderServiceTest
- Covers:
  - Order creation and total calculation.
  - Retrieval by ID, order number, and customer ID.
  - Order status updates via `StateMachineService`.
  - Cancellation logic, including happy path and invalid cancellation rule.
  - Interaction with `StateHistoryService` and `EmailService`.
- Coverage: **High** for `OrderService` (all major paths).

#### PaymentServiceTest
- Covers:
  - Successful payment processing.
  - Order not found scenario.
  - Invalid order state (not `PENDING_PAYMENT`).
  - Gateway failure and exception handling.
  - Fetching payments by ID and by order.
  - Checking payment status.
- Coverage: **High** for `PaymentService`.

#### ReturnServiceTest
- Covers:
  - Return creation under valid conditions (`DELIVERED` orders).
  - Order not found and order not delivered cases.
  - Duplicate active return prevention.
  - Approve/reject flows, including invalid states.
  - Marking returns in transit and received.
  - Fetching returns by ID and order ID.
- Coverage: **High** for `ReturnService`.

#### RefundServiceTest
- Covers:
  - Refund creation when returns are `RECEIVED`.
  - Return not found, invalid return state, and duplicate refund scenarios.
  - Refund retrieval by ID and return ID.
  - Refund processing:
    - Success path (COMPLETED, return moves to COMPLETED).
    - Gateway failure (FAILED).
    - Gateway exception (PaymentProcessingException).
  - Listing pending refunds.
- Coverage: **High** for `RefundService`.

#### StateMachineServiceTest
- Covers:
  - Valid and invalid order transitions.
  - Valid and invalid return transitions.
  - Recording of state history via `StateHistoryService`.
  - `getAllowedOrderTransitions` and `getAllowedReturnTransitions`.
- Coverage: **High** for `StateMachineService`.

#### StateHistoryServiceTest
- Covers:
  - Logging transitions with and without user IDs.
  - Fetching history by entity and by date range.
  - Getting the latest transition and empty history behavior.
- Coverage: **High** for `StateHistoryService`.

---

### 3. Controller Test Coverage (Web Layer)

All controllers are tested using `@WebMvcTest` and MockMvc.

#### OrderControllerTest
- Endpoints:
  - `POST /api/orders`
  - `GET /api/orders/{orderId}`
  - `GET /api/orders/order-number/{orderNumber}`
  - `GET /api/orders/customer/{customerId}`
  - `PUT /api/orders/{orderId}/status`
  - `POST /api/orders/{orderId}/cancel`
  - `GET /api/orders/{orderId}/history`
- Validates:
  - Request/response JSON shapes.
  - Validation errors for invalid input.
  - HTTP status codes (201, 200, 400).

#### PaymentControllerTest
- Endpoints:
  - `POST /api/payments`
  - `GET /api/payments/{paymentId}`
  - `GET /api/payments/order/{orderId}`
  - `POST /api/payments/webhook`
- Validates success and validation error scenarios.

#### ReturnControllerTest
- Endpoints:
  - `POST /api/returns`
  - `GET /api/returns/{returnId}`
  - `GET /api/returns/order/{orderId}`
  - `POST /api/returns/{returnId}/approve`
  - `POST /api/returns/{returnId}/reject`
  - `PUT /api/returns/{returnId}/in-transit`
  - `PUT /api/returns/{returnId}/received`
  - `GET /api/returns/{returnId}/history`
- Validates both happy paths and validation failures.

#### RefundControllerTest
- Endpoints:
  - `POST /api/refunds/return/{returnId}`
  - `GET /api/refunds/{refundId}`
  - `GET /api/refunds/return/{returnId}`
  - `POST /api/refunds/{refundId}/process`
- Ensures mapping and response structures are correct.

Overall controller coverage: **High** for all REST endpoints.

---

### 4. Integration / API Test Coverage

Integration tests (`*ApiIntegrationTest`) use the full Spring context and H2 database.

#### OrderApiIntegrationTest
- Covers:
  - Creating orders (including invalid payloads).
  - Fetching by ID, order number, and customer ID.
  - Updating status and cancelling.
  - Fetching state history.

#### PaymentApiIntegrationTest
- Covers:
  - Processing payments for real orders.
  - Validation errors and order-not-found paths.
  - Fetching payments by ID and by order.
  - Webhook endpoint sanity.

#### ReturnApiIntegrationTest
- Covers:
  - Creating return requests for DELIVERED orders.
  - Validation and business rule enforcement (order not delivered, order not found).
  - Approve/reject flows.
  - Marking returns in transit and received.
  - Fetching return history.

#### RefundApiIntegrationTest
- Covers:
  - Creating refunds for RECEIVED returns.
  - Business rules (wrong state, duplicate refunds).
  - Fetching refunds by ID and by return ID.
  - Processing refunds via the simulated payment gateway.

Integration coverage: **Strong** for all major flows; these tests ensure the system behaves correctly end-to-end.

---

### 5. Estimated Coverage Summary

Although we have not run JaCoCo in this repository, the structure of the tests suggests:

- **Service layer**: Very high logical coverage – most branches and error paths are exercised.
- **Controller layer**: High coverage – all endpoints are tested with both success and common failure paths.
- **Workflow & State Management**: High coverage – state transitions and history logging are thoroughly tested.
- **Integration flows**: High scenario coverage for core business workflows.

If JaCoCo is added and executed, we expect:

- **Line coverage** for core business packages (`service`, `controller`, `model`, `repository`) to be well above **80%**.
- Lower coverage only in:
  - Infrastructure glue (e.g., `EmailClient`, simple configuration classes).
  - Some stubbed integration behavior (webhook handling).

---

### 6. How to Generate a Detailed Coverage Report

To obtain precise coverage metrics:

1. Add JaCoCo plugin to `pom.xml` (if not already present):

```xml
<build>
  <plugins>
    <plugin>
      <groupId>org.jacoco</groupId>
      <artifactId>jacoco-maven-plugin</artifactId>
      <version>0.8.11</version>
      <executions>
        <execution>
          <goals>
            <goal>prepare-agent</goal>
          </goals>
        </execution>
        <execution>
          <id>report</id>
          <phase>test</phase>
          <goals>
            <goal>report</goal>
          </goals>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

2. Run:

```bash
mvn test
```

3. Open the report:

```text
target/site/jacoco/index.html
```

This will show exact line and branch coverage per package and class.

---

### 7. Conclusion

The current automated test suite:

- Exercises all critical paths of the order, payment, return, and refund workflows.
- Validates both **happy paths** and **error conditions**.
- Provides strong confidence that the API and state machine behave correctly.

Adding JaCoCo (as outlined above) is a straightforward next step if exact coverage metrics are required for reporting.

