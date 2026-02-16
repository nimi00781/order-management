## ArtiCurated Order Management System – Architecture

### 1. Overview

The ArtiCurated backend is a single Spring Boot microservice responsible for:
- Managing the full order lifecycle from creation to delivery and possible cancellation.
- Handling payments via an external payment gateway.
- Orchestrating a multi-step return and refund workflow.
- Persisting a complete audit trail of state changes for orders and returns.

The system follows a classic layered architecture:

- **API layer**: REST controllers in `controller/`.
- **Service layer**: business logic and workflows in `service/`.
- **Repository layer**: persistence via Spring Data JPA repositories in `repository/`.
- **Database layer**: PostgreSQL with Flyway migrations.

---

### 2. Main Components and Responsibilities

- **Order Management**
  - Create orders with one or more items.
  - Maintain order status using a state machine.
  - Support cancellation from early states.

- **Payment Management**
  - Process payments for orders via `PaymentGatewayClient`.
  - Update order status to `PAID` on success.

- **Return Management**
  - Allow returns only for `DELIVERED` orders.
  - Implement a multi-step workflow: `REQUESTED → APPROVED/REJECTED → IN_TRANSIT → RECEIVED → COMPLETED`.

- **Refund Management**
  - Create refunds only when returns are `RECEIVED`.
  - Trigger refund with payment gateway and complete return workflow.

- **State History**
  - Central `StateMachineService` transitions states.
  - `StateHistoryService` writes all transitions into `state_transitions`.

- **Background Jobs**
  - `PaymentStatusCheckerJob`: periodically checks pending payments.
  - `RefundProcessorJob`: periodically processes pending refunds.

---

### 3. Order Flow (Happy Path)

1. **Create Order**
   - API: `POST /api/orders`
   - Input: `CreateOrderRequest { customerId, orderItems[], shippingAddress }`
   - Behavior:
     - Persist `Order` and `OrderItem` entities.
     - Initialize status to `PENDING_PAYMENT`.
     - Log state transition: `null → PENDING_PAYMENT`.

2. **Process Payment**
   - API: `POST /api/payments`
   - Input: `ProcessPaymentRequest { orderId, paymentMethod, amount, paymentDetails }`
   - Behavior:
     - Ensure order is `PENDING_PAYMENT`.
     - Create `Payment` with status `PENDING`.
     - Call `PaymentGatewayClient.processPayment`.
     - On success:
       - Set payment status to `SUCCESS`.
       - Transition order: `PENDING_PAYMENT → PAID`.
       - Notify customer via `EmailService.sendOrderConfirmation`.

3. **Fulfillment & Shipment** (typically by back-office UI)
   - API: `PUT /api/orders/{orderId}/status`
   - Sequence of transitions enforced by `StateMachineService`:
     - `PAID → PROCESSING_IN_WAREHOUSE`
     - `PROCESSING_IN_WAREHOUSE → SHIPPED`
     - `SHIPPED → DELIVERED`
   - Each transition is logged to `state_transitions`.

4. **Cancellation (Optional)**
   - API: `POST /api/orders/{orderId}/cancel`
   - Allowed from:
     - `PENDING_PAYMENT`
     - `PAID` (only if not yet `PROCESSING_IN_WAREHOUSE`)
   - Transition: `PENDING_PAYMENT/PAID → CANCELLED`.

---

### 4. Return & Refund Flow

#### 4.1 Return Workflow

1. **Initiate Return**
   - API: `POST /api/returns`
   - Conditions:
     - Order must exist and be in `DELIVERED` state.
     - No existing active return (`REQUESTED`) for that order.
   - Behavior:
     - Create `Return` with status `REQUESTED`.
     - Log transition: `null → REQUESTED` (entity type `RETURN`).

2. **Approval / Rejection**
   - Approve:
     - API: `POST /api/returns/{returnId}/approve`
     - Transition: `REQUESTED → APPROVED`.
     - Set `approvedBy` and `approvedAt`.
   - Reject:
     - API: `POST /api/returns/{returnId}/reject`
     - Transition: `REQUESTED → REJECTED`.
     - Terminal state, no refund.

3. **Customer Ships Item Back**
   - API: `PUT /api/returns/{returnId}/in-transit`
   - Transition: `APPROVED → IN_TRANSIT`.
   - Set `shippedAt`.

4. **Warehouse Receives Item**
   - API: `PUT /api/returns/{returnId}/received`
   - Transition: `IN_TRANSIT → RECEIVED`.
   - Set `receivedAt`.

5. **Refund Processing**
   - API: `POST /api/refunds/return/{returnId}`
   - Conditions:
     - Return must be in `RECEIVED` state.
     - No existing refund for this return.
   - Behavior:
     - Create `Refund` with status `PENDING`.
     - Optionally queued for background processing.

6. **Complete Refund**
   - API: `POST /api/refunds/{refundId}/process`
   - Steps:
     - Call `PaymentGatewayClient.processRefund`.
     - On success: set refund status to `COMPLETED` and transition return: `RECEIVED → COMPLETED`.
     - On failure: set refund status to `FAILED`.
   - Notify customer via `EmailService.sendRefundConfirmation`.

---

### 5. Database Schema & Relationships

Core tables (see `V1__create_initial_schema.sql`):

- **orders**
  - `id` (PK), `order_number` (unique), `customer_id`, `status`, `total_amount`,
    `shipping_address`, `created_at`, `updated_at`.

- **order_items**
  - `id` (PK), `order_id` (FK → orders.id), `product_id`, `product_name`,
    `quantity`, `price`, `subtotal`.
  - Relationship: **orders 1 → N order_items**.

- **payments**
  - `id` (PK), `order_id` (FK → orders.id), `payment_method`, `amount`,
    `status`, `transaction_id`, `processed_at`, `created_at`.
  - Relationship: **orders 1 → N payments** (supports multiple attempts).

- **returns**
  - `id` (PK), `order_id` (FK → orders.id), `return_reason`, `status`,
    `requested_at`, `approved_by`, `approved_at`, `shipped_at`, `received_at`, `comments`.
  - Relationship: **orders 1 → N returns**.

- **refunds**
  - `id` (PK), `return_id` (FK → returns.id, UNIQUE), `amount`, `status`,
    `refund_method`, `transaction_id`, `processed_at`, `created_at`.
  - Relationship: **returns 1 → 1 refunds**.

- **state_transitions**
  - `id` (PK), `entity_type` (`ORDER` or `RETURN`), `entity_id`,
    `from_state`, `to_state`, `triggered_by`, `transition_reason`, `timestamp`.
  - Relationships:
    - **orders 1 → N state_transitions** (when `entity_type = ORDER`).
    - **returns 1 → N state_transitions** (when `entity_type = RETURN`).

These tables, combined with foreign keys and indexes, allow:
- Efficient lookups by order, return, or refund.
- Quick retrieval of all state transitions for a given entity.
- Strong integrity via constraints and `CHECK` clauses (amount > 0, valid status strings).

---

### 6. API Overview

#### Order APIs
- `POST /api/orders` – Create order.  
- `GET /api/orders/{orderId}` – Get by ID.  
- `GET /api/orders/order-number/{orderNumber}` – Get by business key.  
- `GET /api/orders/customer/{customerId}` – List for a customer.  
- `PUT /api/orders/{orderId}/status` – Advance order state.  
- `POST /api/orders/{orderId}/cancel` – Cancel order.  
- `GET /api/orders/{orderId}/history` – Audit history.

#### Payment APIs
- `POST /api/payments` – Process payment.  
- `GET /api/payments/{paymentId}` – Get by ID.  
- `GET /api/payments/order/{orderId}` – List by order.  
- `POST /api/payments/webhook` – Payment status webhook (stub).

#### Return APIs
- `POST /api/returns` – Create return request.  
- `GET /api/returns/{returnId}` – Get by ID.  
- `GET /api/returns/order/{orderId}` – List by order.  
- `POST /api/returns/{returnId}/approve` – Approve.  
- `POST /api/returns/{returnId}/reject` – Reject.  
- `PUT /api/returns/{returnId}/in-transit` – Mark in transit.  
- `PUT /api/returns/{returnId}/received` – Mark received.  
- `GET /api/returns/{returnId}/history` – Audit history.

#### Refund APIs
- `POST /api/refunds/return/{returnId}` – Create refund.  
- `GET /api/refunds/{refundId}` – Get by ID.  
- `GET /api/refunds/return/{returnId}` – Get by return.  
- `POST /api/refunds/{refundId}/process` – Process refund.

---

### 7. State History & Auditing

Every state transition for `Order` and `Return` flows through `StateMachineService`.  
For each transition:
- The current and target states are validated.
- A `StateTransition` record is saved with:
  - `entity_type` (ORDER/RETURN)
  - `entity_id`
  - `from_state` and `to_state`
  - `triggered_by` (user ID or `SYSTEM`)
  - `transition_reason`
  - `timestamp`

This design ensures:
- Complete auditability of all lifecycle changes.
- Easy debugging of complex workflows.
- Support for future reporting/analytics.

