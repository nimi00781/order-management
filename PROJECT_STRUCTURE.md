# ArtiCurated Order Management System - High-Level Architecture

## System Overview

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                         ArtiCurated Order Management System                  │
│                         (Spring Boot 3.x + Java 21 + Maven)                  │
│                    Complex State Management & Multi-Step Workflows           │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 1. High-Level Component Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              API Layer (REST Controllers)                    │
├─────────────────────────────────────────────────────────────────────────────┤
│  • OrderController          • PaymentController        • ReturnController   │
│  • RefundController                                                          │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Service Layer (Business Logic)                      │
├─────────────────────────────────────────────────────────────────────────────┤
│  • OrderService             • PaymentService           • ReturnService      │
│  • RefundService            • EmailService            • StateMachineService│
│  • StateHistoryService      (Tracks all state transitions for auditing)    │
└─────────────────────────────────────────────────────────────────────────────┘
                                    │
                    ┌───────────────┼───────────────┐
                    ▼               ▼               ▼
┌─────────────────────────┐  ┌──────────────┐  ┌──────────────────────┐
│   Repository Layer       │  │  Background  │  │  Integration Layer   │
│   (JPA Repositories)     │  │  Jobs        │  │  (Third-Party APIs)  │
├─────────────────────────┤  ├──────────────┤  ├──────────────────────┤
│  • OrderRepository       │  │  • Scheduled │  │  • PaymentGateway    │
│  • PaymentRepository     │  │    Jobs      │  │  • EmailService      │
│  • ReturnRepository      │  │    (@Scheduled)│  │    (Simple SMTP)    │
│  • RefundRepository      │  │              │  │                      │
│  • StateTransitionRepository│              │  │                      │
└─────────────────────────┘  └──────────────┘  └──────────────────────┘
         │
         ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Database Layer (PostgreSQL)                         │
├─────────────────────────────────────────────────────────────────────────────┤
│  • orders                  • payments              • returns                │
│  • order_items             • refunds               • state_transitions     │
└─────────────────────────────────────────────────────────────────────────────┘
```

## 2. Order State Machine (Complex State Management)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Order Lifecycle State Machine                       │
│                    (Enforced transitions with state machine)                 │
└─────────────────────────────────────────────────────────────────────────────┘

                    ┌──────────────────┐
                    │ PENDING_PAYMENT  │ (Order Created)
                    └────────┬─────────┘
                             │
                ┌────────────┼────────────┐
                │            │            │
                ▼            ▼            ▼
        ┌───────────┐  ┌───────────┐  ┌───────────┐
        │ CANCELLED │  │ CANCELLED │  │   PAID    │ (Payment Successful)
        └───────────┘  └───────────┘  └─────┬─────┘
         (Can cancel)   (Can cancel)        │
         from PENDING   from PAID           │
         _PAYMENT       (if not yet         │
                        processed)          │
                                            ▼
                                    ┌──────────────────────┐
                                    │ PROCESSING_IN_       │
                                    │ WAREHOUSE            │
                                    └──────┬───────────────┘
                                           │
                                           ▼
                                    ┌───────────┐
                                    │  SHIPPED  │
                                    └─────┬─────┘
                                          │
                                          ▼
                                    ┌───────────┐
                                    │ DELIVERED │ (Final State)
                                    └───────────┘

    State Transition Rules:
    ├── PENDING_PAYMENT → PAID (on successful payment)
    ├── PENDING_PAYMENT → CANCELLED (customer/admin cancels)
    ├── PAID → PROCESSING_IN_WAREHOUSE (order processing starts)
    ├── PAID → CANCELLED (only if not yet in PROCESSING_IN_WAREHOUSE)
    ├── PROCESSING_IN_WAREHOUSE → SHIPPED (item shipped)
    └── SHIPPED → DELIVERED (delivery confirmed)

    Note: All state transitions are logged in state_transitions table for auditing
```

## 3. Return State Machine (Multi-Step Workflow)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Return Lifecycle State Machine                        │
│                    (Enforced transitions with state machine)                │
└─────────────────────────────────────────────────────────────────────────────┘

    Prerequisite: Order must be in DELIVERED state to initiate return

                    ┌──────────────┐
                    │  REQUESTED   │ (Customer initiates return)
                    └──────┬───────┘
                           │
                           ▼
                    ┌──────────────┐
                    │  Manager     │ (Store Manager Review)
                    │  Review      │
                    └──────┬───────┘
                           │
                ┌──────────┼──────────┐
                │                     │
                ▼                     ▼
        ┌──────────────┐      ┌──────────────┐
        │  APPROVED    │      │  REJECTED    │ (Final State - No refund)
        └──────┬───────┘      └──────────────┘
               │
               ▼
        ┌──────────────┐
        │  IN_TRANSIT  │ (Customer ships item back)
        └──────┬───────┘
               │
               ▼
        ┌──────────────┐
        │  RECEIVED    │ (Warehouse confirms receipt)
        └──────┬───────┘
               │
               ▼
        ┌──────────────┐
        │  COMPLETED   │ (Refund successfully processed - Final State)
        └──────────────┘

    State Transition Rules:
    ├── REQUESTED → APPROVED (manager approves)
    ├── REQUESTED → REJECTED (manager rejects - terminal state)
    ├── APPROVED → IN_TRANSIT (customer ships item)
    ├── IN_TRANSIT → RECEIVED (warehouse confirms receipt)
    └── RECEIVED → COMPLETED (refund processed)

    Note: All state transitions are logged in state_transitions table for auditing
```

## 4. State Management Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    State Machine & History Tracking                           │
└─────────────────────────────────────────────────────────────────────────────┘

    State Machine Components
         │
         ├──► OrderStateMachine
         │    ├── States: PENDING_PAYMENT, PAID, PROCESSING_IN_WAREHOUSE,
         │    │          SHIPPED, DELIVERED, CANCELLED
         │    ├── Enforces valid transitions
         │    └── Prevents invalid state changes
         │
         └──► ReturnStateMachine
              ├── States: REQUESTED, APPROVED, REJECTED, IN_TRANSIT,
              │          RECEIVED, COMPLETED
              ├── Enforces valid transitions
              └── Validates prerequisites (order must be DELIVERED)

    State History Service
         │
         ├──► Logs every state transition
         │    ├── Entity type (ORDER/RETURN)
         │    ├── Entity ID
         │    ├── From state → To state
         │    ├── Triggered by (user/system)
         │    ├── Transition reason
         │    └── Timestamp
         │
         └──► Provides audit trail queries
              └── Retrieve complete history for any order/return
```

## 5. Background Jobs (Basic Scheduled Tasks)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Background Job Architecture                          │
└─────────────────────────────────────────────────────────────────────────────┘

    Scheduled Jobs (Spring @Scheduled)
         │
         ├──► Payment Status Checker (Every 10 min)
         │    └──► Check pending payments with payment gateway
         │
         └──► Refund Processor (Every 15 min)
              └──► Process approved refunds

    Async Tasks (Spring @Async)
         │
         └──► Email Notifications
              └──► Send order confirmations and status updates
```

## 6. Third-Party Integrations (Essential Only)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        External System Integrations                          │
└─────────────────────────────────────────────────────────────────────────────┘

    Order Management System
         │
         ├──► Payment Gateway (Stripe/PayPal)
         │    ├──► Process payments
         │    └──► Process refunds
         │
         └──► Email Service (SMTP / Simple Email Service)
              └──► Send order notifications
```

## 7. Database Schema (Core Tables)

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Core Database Tables                                │
└─────────────────────────────────────────────────────────────────────────────┘

    orders
    ├── id (PK)
    ├── order_number (unique)
    ├── customer_id
    ├── status (enum: PENDING_PAYMENT, PAID, PROCESSING_IN_WAREHOUSE, 
    │            SHIPPED, DELIVERED, CANCELLED)
    ├── total_amount
    ├── shipping_address
    ├── created_at
    └── updated_at

    order_items
    ├── id (PK)
    ├── order_id (FK)
    ├── product_id
    ├── product_name
    ├── quantity
    ├── price
    └── subtotal

    payments
    ├── id (PK)
    ├── order_id (FK)
    ├── payment_method
    ├── amount
    ├── status (enum: PENDING, SUCCESS, FAILED)
    ├── transaction_id (external payment gateway ID)
    ├── processed_at
    └── created_at

    returns
    ├── id (PK)
    ├── order_id (FK)
    ├── return_reason
    ├── status (enum: REQUESTED, APPROVED, REJECTED, IN_TRANSIT, 
    │            RECEIVED, COMPLETED)
    ├── requested_at
    ├── approved_by (manager user ID)
    ├── approved_at
    ├── shipped_at (when customer ships item)
    ├── received_at (when warehouse receives)
    └── comments

    refunds
    ├── id (PK)
    ├── return_id (FK)
    ├── amount
    ├── status (enum: PENDING, PROCESSING, COMPLETED, FAILED)
    ├── refund_method
    ├── transaction_id (external payment gateway refund ID)
    ├── processed_at
    └── created_at

    state_transitions (Audit Log for State Changes)
    ├── id (PK)
    ├── entity_type (enum: ORDER, RETURN)
    ├── entity_id (FK to orders.id or returns.id)
    ├── from_state (previous state)
    ├── to_state (new state)
    ├── triggered_by (user ID or system)
    ├── transition_reason (optional comment)
    └── timestamp (when transition occurred)
```

## 8. Technology Stack

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                              Technology Stack                                │
└─────────────────────────────────────────────────────────────────────────────┘

    Framework & Language
    ├── Java 21 (LTS)
    ├── Spring Boot 3.x
    ├── Spring Data JPA
    ├── Spring Web (REST APIs)
    ├── Spring State Machine (for order and return state management)
    └── Spring @Async / @Scheduled (Background jobs)

    Build & Dependency Management
    └── Maven

    Database
    ├── PostgreSQL 15+
    └── Flyway (Database migrations)

    State Management
    └── Spring State Machine (enforces valid state transitions)

    Testing
    ├── JUnit 5
    ├── Mockito
    └── Spring Boot Test

    API Documentation
    └── SpringDoc OpenAPI (Swagger)
```

## 9. Core Features Summary

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                          Core Features                                       │
└─────────────────────────────────────────────────────────────────────────────┘

    Order Management (State Machine Enforced)
    ├── Create order (starts in PENDING_PAYMENT)
    ├── View order details
    ├── State transitions (enforced by state machine):
    │   ├── PENDING_PAYMENT → PAID (on payment success)
    │   ├── PENDING_PAYMENT → CANCELLED (can cancel)
    │   ├── PAID → PROCESSING_IN_WAREHOUSE
    │   ├── PAID → CANCELLED (if not yet processed)
    │   ├── PROCESSING_IN_WAREHOUSE → SHIPPED
    │   └── SHIPPED → DELIVERED
    └── All state transitions logged for auditing

    Payment Processing
    ├── Process payment (via payment gateway)
    ├── Check payment status
    └── Handle payment failures

    Return Management (State Machine Enforced)
    ├── Request return (only for DELIVERED orders)
    ├── Multi-step workflow:
    │   ├── REQUESTED → APPROVED/REJECTED (manager review)
    │   ├── APPROVED → IN_TRANSIT (customer ships)
    │   ├── IN_TRANSIT → RECEIVED (warehouse confirms)
    │   └── RECEIVED → COMPLETED (refund processed)
    └── All state transitions logged for auditing

    Refund Processing
    ├── Create refund for returns in RECEIVED state
    ├── Process refund via payment gateway
    └── Track refund status

    State History & Auditing
    ├── Log all state transitions (orders and returns)
    ├── Track who triggered each transition
    ├── Store transition timestamps
    └── Query state history for auditing

    Background Jobs
    ├── Check payment status periodically
    └── Process refunds periodically

    Notifications
    └── Email notifications for order/return status changes
```

## 10. Basic Error Handling

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        Error Handling Strategy                               │
└─────────────────────────────────────────────────────────────────────────────┘

    Error Handling
    ├── Global exception handler (@ControllerAdvice)
    ├── Custom exception classes
    ├── Proper HTTP status codes
    └── Standardized error response format

    Basic Resilience
    ├── Transaction management (@Transactional)
    └── Basic retry for payment gateway calls (simple retry logic)
```

---

## Summary

This architecture provides:
- ✅ **Complex Order State Management** - State machine enforcing valid transitions:
  - PENDING_PAYMENT → PAID → PROCESSING_IN_WAREHOUSE → SHIPPED → DELIVERED
  - Cancellation allowed from PENDING_PAYMENT or PAID (if not yet processed)
- ✅ **Multi-Step Returns Workflow** - State machine for return lifecycle:
  - REQUESTED → APPROVED/REJECTED → IN_TRANSIT → RECEIVED → COMPLETED
  - Returns only allowed for DELIVERED orders
- ✅ **State Change History Logging** - Complete audit trail of all state transitions
  - Tracks entity type (ORDER/RETURN), from/to states, triggered by, timestamp
- ✅ Payment processing integration
- ✅ Refund workflow
- ✅ Basic background job processing
- ✅ Essential third-party integrations (Payment, Email)
- ✅ Core database schema with state management
- ✅ Clean layered architecture

**Key Design Decisions:**
- Spring State Machine used to enforce valid state transitions
- All state changes logged in `state_transitions` table for auditing
- State transitions are atomic and validated before execution
- Return requests can only be initiated for orders in DELIVERED state
