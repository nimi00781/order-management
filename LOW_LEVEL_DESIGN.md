# ArtiCurated Order Management System - Low-Level Design

## 1. Package Structure

```
com.articurated.ordermanagement
├── config/
│   ├── StateMachineConfig.java
│   ├── AsyncConfig.java
│   ├── WebConfig.java
│   └── DatabaseConfig.java
├── controller/
│   ├── OrderController.java
│   ├── PaymentController.java
│   ├── ReturnController.java
│   └── RefundController.java
├── service/
│   ├── OrderService.java
│   ├── PaymentService.java
│   ├── ReturnService.java
│   ├── RefundService.java
│   ├── StateMachineService.java
│   ├── StateHistoryService.java
│   └── EmailService.java
├── repository/
│   ├── OrderRepository.java
│   ├── PaymentRepository.java
│   ├── ReturnRepository.java
│   ├── RefundRepository.java
│   └── StateTransitionRepository.java
├── model/
│   ├── entity/
│   │   ├── Order.java
│   │   ├── OrderItem.java
│   │   ├── Payment.java
│   │   ├── Return.java
│   │   ├── Refund.java
│   │   └── StateTransition.java
│   ├── enums/
│   │   ├── OrderStatus.java
│   │   ├── ReturnStatus.java
│   │   ├── PaymentStatus.java
│   │   ├── RefundStatus.java
│   │   └── EntityType.java
│   ├── dto/
│   │   ├── request/
│   │   │   ├── CreateOrderRequest.java
│   │   │   ├── ProcessPaymentRequest.java
│   │   │   ├── CreateReturnRequest.java
│   │   │   ├── ApproveReturnRequest.java
│   │   │   └── UpdateOrderStatusRequest.java
│   │   └── response/
│   │       ├── OrderResponse.java
│   │       ├── PaymentResponse.java
│   │       ├── ReturnResponse.java
│   │       ├── RefundResponse.java
│   │       └── StateHistoryResponse.java
│   └── exception/
│       ├── OrderNotFoundException.java
│       ├── InvalidStateTransitionException.java
│       ├── PaymentProcessingException.java
│       ├── ReturnNotAllowedException.java
│       └── GlobalExceptionHandler.java
├── integration/
│   ├── PaymentGatewayClient.java
│   └── EmailClient.java
├── scheduler/
│   ├── PaymentStatusCheckerJob.java
│   └── RefundProcessorJob.java
└── OrderManagementApplication.java
```

## 2. Entity Models (JPA Entities)

### 2.1 Order Entity

```
Order
├── Fields:
│   ├── id: Long (PK, Auto-generated)
│   ├── orderNumber: String (Unique, e.g., "ORD-2024-001")
│   ├── customerId: Long
│   ├── status: OrderStatus (Enum)
│   ├── totalAmount: BigDecimal
│   ├── shippingAddress: String
│   ├── orderItems: List<OrderItem> (OneToMany)
│   ├── payments: List<Payment> (OneToMany)
│   ├── returns: List<Return> (OneToMany)
│   ├── createdAt: LocalDateTime
│   └── updatedAt: LocalDateTime
├── Relationships:
│   ├── @OneToMany → OrderItem
│   ├── @OneToMany → Payment
│   └── @OneToMany → Return
└── Methods:
    ├── calculateTotal()
    └── canBeCancelled()
```

### 2.2 OrderItem Entity

```
OrderItem
├── Fields:
│   ├── id: Long (PK)
│   ├── order: Order (ManyToOne, FK)
│   ├── productId: Long
│   ├── productName: String
│   ├── quantity: Integer
│   ├── price: BigDecimal
│   └── subtotal: BigDecimal
└── Relationships:
    └── @ManyToOne → Order
```

### 2.3 Payment Entity

```
Payment
├── Fields:
│   ├── id: Long (PK)
│   ├── order: Order (ManyToOne, FK)
│   ├── paymentMethod: String
│   ├── amount: BigDecimal
│   ├── status: PaymentStatus (Enum)
│   ├── transactionId: String (External payment gateway ID)
│   ├── processedAt: LocalDateTime
│   └── createdAt: LocalDateTime
└── Relationships:
    └── @ManyToOne → Order
```

### 2.4 Return Entity

```
Return
├── Fields:
│   ├── id: Long (PK)
│   ├── order: Order (ManyToOne, FK)
│   ├── returnReason: String
│   ├── status: ReturnStatus (Enum)
│   ├── requestedAt: LocalDateTime
│   ├── approvedBy: Long (Manager user ID)
│   ├── approvedAt: LocalDateTime
│   ├── shippedAt: LocalDateTime
│   ├── receivedAt: LocalDateTime
│   ├── comments: String
│   └── refund: Refund (OneToOne)
├── Relationships:
│   ├── @ManyToOne → Order
│   └── @OneToOne → Refund
└── Methods:
    ├── canBeApproved()
    └── canTransitionTo(ReturnStatus)
```

### 2.5 Refund Entity

```
Refund
├── Fields:
│   ├── id: Long (PK)
│   ├── return: Return (OneToOne, FK)
│   ├── amount: BigDecimal
│   ├── status: RefundStatus (Enum)
│   ├── refundMethod: String
│   ├── transactionId: String (External payment gateway refund ID)
│   ├── processedAt: LocalDateTime
│   └── createdAt: LocalDateTime
└── Relationships:
    └── @OneToOne → Return
```

### 2.6 StateTransition Entity (Audit Log)

```
StateTransition
├── Fields:
│   ├── id: Long (PK)
│   ├── entityType: EntityType (Enum: ORDER, RETURN)
│   ├── entityId: Long
│   ├── fromState: String
│   ├── toState: String
│   ├── triggeredBy: Long (User ID or "SYSTEM")
│   ├── transitionReason: String
│   └── timestamp: LocalDateTime
└── Indexes:
    ├── idx_entity_type_id (entityType, entityId)
    └── idx_timestamp (timestamp)
```

## 3. Enum Definitions

### 3.1 OrderStatus Enum

```
OrderStatus
├── PENDING_PAYMENT
├── PAID
├── PROCESSING_IN_WAREHOUSE
├── SHIPPED
├── DELIVERED
└── CANCELLED
```

### 3.2 ReturnStatus Enum

```
ReturnStatus
├── REQUESTED
├── APPROVED
├── REJECTED
├── IN_TRANSIT
├── RECEIVED
└── COMPLETED
```

### 3.3 PaymentStatus Enum

```
PaymentStatus
├── PENDING
├── SUCCESS
└── FAILED
```

### 3.4 RefundStatus Enum

```
RefundStatus
├── PENDING
├── PROCESSING
├── COMPLETED
└── FAILED
```

### 3.5 EntityType Enum

```
EntityType
├── ORDER
└── RETURN
```

## 4. Repository Interfaces (Spring Data JPA)

### 4.1 OrderRepository

```
OrderRepository extends JpaRepository<Order, Long>
├── Methods:
│   ├── findByOrderNumber(String orderNumber): Optional<Order>
│   ├── findByCustomerId(Long customerId): List<Order>
│   ├── findByStatus(OrderStatus status): List<Order>
│   └── findByCustomerIdAndStatus(Long customerId, OrderStatus status): List<Order>
```

### 4.2 PaymentRepository

```
PaymentRepository extends JpaRepository<Payment, Long>
├── Methods:
│   ├── findByOrderId(Long orderId): List<Payment>
│   ├── findByTransactionId(String transactionId): Optional<Payment>
│   └── findByStatus(PaymentStatus status): List<Payment>
```

### 4.3 ReturnRepository

```
ReturnRepository extends JpaRepository<Return, Long>
├── Methods:
│   ├── findByOrderId(Long orderId): List<Return>
│   ├── findByStatus(ReturnStatus status): List<Return>
│   └── findByOrderIdAndStatus(Long orderId, ReturnStatus status): List<Return>
```

### 4.4 RefundRepository

```
RefundRepository extends JpaRepository<Refund, Long>
├── Methods:
│   ├── findByReturnId(Long returnId): Optional<Refund>
│   └── findByStatus(RefundStatus status): List<Refund>
```

### 4.5 StateTransitionRepository

```
StateTransitionRepository extends JpaRepository<StateTransition, Long>
├── Methods:
│   ├── findByEntityTypeAndEntityId(EntityType type, Long entityId): List<StateTransition>
│   ├── findByEntityTypeAndEntityIdOrderByTimestampDesc(EntityType, Long): List<StateTransition>
│   └── findByTimestampBetween(LocalDateTime start, LocalDateTime end): List<StateTransition>
```

## 5. Service Layer (Business Logic)

### 5.1 OrderService

```
OrderService
├── Methods:
│   ├── createOrder(CreateOrderRequest request): OrderResponse
│   ├── getOrderById(Long orderId): OrderResponse
│   ├── getOrderByOrderNumber(String orderNumber): OrderResponse
│   ├── getOrdersByCustomerId(Long customerId): List<OrderResponse>
│   ├── updateOrderStatus(Long orderId, OrderStatus newStatus, Long userId): OrderResponse
│   ├── cancelOrder(Long orderId, Long userId): OrderResponse
│   └── validateOrderForCancellation(Order order): boolean
├── Dependencies:
│   ├── OrderRepository
│   ├── StateMachineService
│   └── StateHistoryService
```

### 5.2 PaymentService

```
PaymentService
├── Methods:
│   ├── processPayment(ProcessPaymentRequest request): PaymentResponse
│   ├── getPaymentById(Long paymentId): PaymentResponse
│   ├── getPaymentsByOrderId(Long orderId): List<PaymentResponse>
│   ├── checkPaymentStatus(Long paymentId): PaymentStatus
│   └── handlePaymentWebhook(PaymentWebhookRequest request): void
├── Dependencies:
│   ├── PaymentRepository
│   ├── OrderRepository
│   ├── PaymentGatewayClient
│   └── StateMachineService
```

### 5.3 ReturnService

```
ReturnService
├── Methods:
│   ├── createReturnRequest(CreateReturnRequest request): ReturnResponse
│   ├── getReturnById(Long returnId): ReturnResponse
│   ├── getReturnsByOrderId(Long orderId): List<ReturnResponse>
│   ├── approveReturn(Long returnId, ApproveReturnRequest request): ReturnResponse
│   ├── rejectReturn(Long returnId, String reason, Long managerId): ReturnResponse
│   ├── markReturnInTransit(Long returnId): ReturnResponse
│   ├── markReturnReceived(Long returnId): ReturnResponse
│   └── validateReturnRequest(Order order): boolean
├── Dependencies:
│   ├── ReturnRepository
│   ├── OrderRepository
│   ├── StateMachineService
│   └── StateHistoryService
```

### 5.4 RefundService

```
RefundService
├── Methods:
│   ├── createRefund(Long returnId): RefundResponse
│   ├── getRefundById(Long refundId): RefundResponse
│   ├── getRefundByReturnId(Long returnId): RefundResponse
│   ├── processRefund(Long refundId): RefundResponse
│   └── getPendingRefunds(): List<RefundResponse>
├── Dependencies:
│   ├── RefundRepository
│   ├── ReturnRepository
│   ├── PaymentGatewayClient
│   └── StateMachineService
```

### 5.5 StateMachineService

```
StateMachineService
├── Methods:
│   ├── transitionOrderState(Order order, OrderStatus targetStatus, Long userId, String reason): Order
│   ├── transitionReturnState(Return returnEntity, ReturnStatus targetStatus, Long userId, String reason): Return
│   ├── validateOrderTransition(OrderStatus from, OrderStatus to): boolean
│   ├── validateReturnTransition(ReturnStatus from, ReturnStatus to): boolean
│   ├── getAllowedOrderTransitions(OrderStatus currentStatus): List<OrderStatus>
│   └── getAllowedReturnTransitions(ReturnStatus currentStatus): List<ReturnStatus>
├── Dependencies:
│   ├── StateMachine<OrderStatus, OrderEvent> (Spring State Machine)
│   ├── StateMachine<ReturnStatus, ReturnEvent> (Spring State Machine)
│   └── StateHistoryService
└── State Machine Configuration:
    ├── Order State Machine:
    │   ├── States: PENDING_PAYMENT, PAID, PROCESSING_IN_WAREHOUSE, SHIPPED, DELIVERED, CANCELLED
    │   ├── Transitions:
    │   │   ├── PENDING_PAYMENT → PAID (on payment success)
    │   │   ├── PENDING_PAYMENT → CANCELLED (can cancel)
    │   │   ├── PAID → PROCESSING_IN_WAREHOUSE
    │   │   ├── PAID → CANCELLED (if not processed)
    │   │   ├── PROCESSING_IN_WAREHOUSE → SHIPPED
    │   │   └── SHIPPED → DELIVERED
    │   └── Guards: Validate transition rules
    └── Return State Machine:
        ├── States: REQUESTED, APPROVED, REJECTED, IN_TRANSIT, RECEIVED, COMPLETED
        ├── Transitions:
        │   ├── REQUESTED → APPROVED (manager approves)
        │   ├── REQUESTED → REJECTED (manager rejects)
        │   ├── APPROVED → IN_TRANSIT (customer ships)
        │   ├── IN_TRANSIT → RECEIVED (warehouse confirms)
        │   └── RECEIVED → COMPLETED (refund processed)
        └── Guards: Validate transition rules
```

### 5.6 StateHistoryService

```
StateHistoryService
├── Methods:
│   ├── logStateTransition(EntityType type, Long entityId, String fromState, String toState, Long userId, String reason): StateTransition
│   ├── getStateHistory(EntityType type, Long entityId): List<StateTransition>
│   ├── getStateHistoryByDateRange(EntityType type, Long entityId, LocalDateTime start, LocalDateTime end): List<StateTransition>
│   └── getLatestStateTransition(EntityType type, Long entityId): Optional<StateTransition>
├── Dependencies:
│   └── StateTransitionRepository
```

### 5.7 EmailService

```
EmailService
├── Methods:
│   ├── sendOrderConfirmation(Order order): void
│   ├── sendOrderStatusUpdate(Order order, OrderStatus newStatus): void
│   ├── sendReturnStatusUpdate(Return returnEntity, ReturnStatus newStatus): void
│   └── sendRefundConfirmation(Refund refund): void
├── Dependencies:
│   └── EmailClient
```

## 6. Controller Layer (REST APIs)

### 6.1 OrderController

```
OrderController
├── Endpoints:
│   ├── POST /api/orders
│   │   └── createOrder(@RequestBody CreateOrderRequest): ResponseEntity<OrderResponse>
│   ├── GET /api/orders/{orderId}
│   │   └── getOrderById(@PathVariable Long orderId): ResponseEntity<OrderResponse>
│   ├── GET /api/orders/order-number/{orderNumber}
│   │   └── getOrderByOrderNumber(@PathVariable String orderNumber): ResponseEntity<OrderResponse>
│   ├── GET /api/orders/customer/{customerId}
│   │   └── getOrdersByCustomerId(@PathVariable Long customerId): ResponseEntity<List<OrderResponse>>
│   ├── PUT /api/orders/{orderId}/status
│   │   └── updateOrderStatus(@PathVariable Long orderId, @RequestBody UpdateOrderStatusRequest): ResponseEntity<OrderResponse>
│   ├── POST /api/orders/{orderId}/cancel
│   │   └── cancelOrder(@PathVariable Long orderId, @RequestHeader("userId") Long userId): ResponseEntity<OrderResponse>
│   └── GET /api/orders/{orderId}/history
│       └── getOrderStateHistory(@PathVariable Long orderId): ResponseEntity<List<StateHistoryResponse>>
├── Dependencies:
│   └── OrderService
```

### 6.2 PaymentController

```
PaymentController
├── Endpoints:
│   ├── POST /api/payments
│   │   └── processPayment(@RequestBody ProcessPaymentRequest): ResponseEntity<PaymentResponse>
│   ├── GET /api/payments/{paymentId}
│   │   └── getPaymentById(@PathVariable Long paymentId): ResponseEntity<PaymentResponse>
│   ├── GET /api/payments/order/{orderId}
│   │   └── getPaymentsByOrderId(@PathVariable Long orderId): ResponseEntity<List<PaymentResponse>>
│   └── POST /api/payments/webhook
│       └── handlePaymentWebhook(@RequestBody PaymentWebhookRequest): ResponseEntity<Void>
├── Dependencies:
│   └── PaymentService
```

### 6.3 ReturnController

```
ReturnController
├── Endpoints:
│   ├── POST /api/returns
│   │   └── createReturnRequest(@RequestBody CreateReturnRequest): ResponseEntity<ReturnResponse>
│   ├── GET /api/returns/{returnId}
│   │   └── getReturnById(@PathVariable Long returnId): ResponseEntity<ReturnResponse>
│   ├── GET /api/returns/order/{orderId}
│   │   └── getReturnsByOrderId(@PathVariable Long orderId): ResponseEntity<List<ReturnResponse>>
│   ├── POST /api/returns/{returnId}/approve
│   │   └── approveReturn(@PathVariable Long returnId, @RequestBody ApproveReturnRequest): ResponseEntity<ReturnResponse>
│   ├── POST /api/returns/{returnId}/reject
│   │   └── rejectReturn(@PathVariable Long returnId, @RequestParam String reason, @RequestHeader("userId") Long managerId): ResponseEntity<ReturnResponse>
│   ├── PUT /api/returns/{returnId}/in-transit
│   │   └── markReturnInTransit(@PathVariable Long returnId): ResponseEntity<ReturnResponse>
│   ├── PUT /api/returns/{returnId}/received
│   │   └── markReturnReceived(@PathVariable Long returnId): ResponseEntity<ReturnResponse>
│   └── GET /api/returns/{returnId}/history
│       └── getReturnStateHistory(@PathVariable Long returnId): ResponseEntity<List<StateHistoryResponse>>
├── Dependencies:
│   └── ReturnService
```

### 6.4 RefundController

```
RefundController
├── Endpoints:
│   ├── POST /api/refunds/return/{returnId}
│   │   └── createRefund(@PathVariable Long returnId): ResponseEntity<RefundResponse>
│   ├── GET /api/refunds/{refundId}
│   │   └── getRefundById(@PathVariable Long refundId): ResponseEntity<RefundResponse>
│   ├── GET /api/refunds/return/{returnId}
│   │   └── getRefundByReturnId(@PathVariable Long returnId): ResponseEntity<RefundResponse>
│   └── POST /api/refunds/{refundId}/process
│       └── processRefund(@PathVariable Long refundId): ResponseEntity<RefundResponse>
├── Dependencies:
│   └── RefundService
```

## 7. DTOs (Data Transfer Objects)

### 7.1 Request DTOs

```
CreateOrderRequest
├── customerId: Long
├── orderItems: List<OrderItemRequest>
│   ├── productId: Long
│   ├── productName: String
│   ├── quantity: Integer
│   └── price: BigDecimal
└── shippingAddress: String

ProcessPaymentRequest
├── orderId: Long
├── paymentMethod: String
├── amount: BigDecimal
└── paymentDetails: Map<String, String> (e.g., card details)

CreateReturnRequest
├── orderId: Long
└── returnReason: String

ApproveReturnRequest
├── comments: String (optional)
└── managerId: Long

UpdateOrderStatusRequest
├── newStatus: OrderStatus
└── reason: String (optional)
```

### 7.2 Response DTOs

```
OrderResponse
├── id: Long
├── orderNumber: String
├── customerId: Long
├── status: OrderStatus
├── totalAmount: BigDecimal
├── shippingAddress: String
├── orderItems: List<OrderItemResponse>
├── createdAt: LocalDateTime
└── updatedAt: LocalDateTime

PaymentResponse
├── id: Long
├── orderId: Long
├── paymentMethod: String
├── amount: BigDecimal
├── status: PaymentStatus
├── transactionId: String
└── processedAt: LocalDateTime

ReturnResponse
├── id: Long
├── orderId: Long
├── returnReason: String
├── status: ReturnStatus
├── requestedAt: LocalDateTime
├── approvedBy: Long
├── approvedAt: LocalDateTime
├── shippedAt: LocalDateTime
├── receivedAt: LocalDateTime
└── comments: String

RefundResponse
├── id: Long
├── returnId: Long
├── amount: BigDecimal
├── status: RefundStatus
├── refundMethod: String
├── transactionId: String
└── processedAt: LocalDateTime

StateHistoryResponse
├── id: Long
├── entityType: EntityType
├── entityId: Long
├── fromState: String
├── toState: String
├── triggeredBy: Long
├── transitionReason: String
└── timestamp: LocalDateTime
```

## 8. Integration Layer

### 8.1 PaymentGatewayClient

```
PaymentGatewayClient
├── Methods:
│   ├── processPayment(PaymentRequest request): PaymentResponse
│   ├── processRefund(RefundRequest request): RefundResponse
│   ├── getPaymentStatus(String transactionId): PaymentStatus
│   └── validateWebhookSignature(String payload, String signature): boolean
└── Configuration:
    ├── Base URL
    ├── API Key
    └── Timeout settings
```

### 8.2 EmailClient

```
EmailClient
├── Methods:
│   ├── sendEmail(String to, String subject, String body): void
│   └── sendEmailWithTemplate(String to, String template, Map<String, Object> variables): void
└── Configuration:
    ├── SMTP Host
    ├── SMTP Port
    ├── Username
    └── Password
```

## 9. Scheduler Jobs

### 9.1 PaymentStatusCheckerJob

```
PaymentStatusCheckerJob
├── @Scheduled(fixedRate = 600000) // Every 10 minutes
├── Methods:
│   └── checkPendingPayments(): void
│       ├── Find all payments with status PENDING
│       ├── Query payment gateway for status
│       ├── Update payment status if changed
│       └── Trigger order state transition if payment successful
└── Dependencies:
    ├── PaymentRepository
    ├── PaymentGatewayClient
    └── StateMachineService
```

### 9.2 RefundProcessorJob

```
RefundProcessorJob
├── @Scheduled(fixedRate = 900000) // Every 15 minutes
├── Methods:
│   └── processPendingRefunds(): void
│       ├── Find all refunds with status PENDING
│       ├── Process refund via payment gateway
│       ├── Update refund status
│       └── Update return status to COMPLETED if refund successful
└── Dependencies:
    ├── RefundRepository
    ├── PaymentGatewayClient
    └── StateMachineService
```

## 10. Configuration Classes

### 10.1 StateMachineConfig

```
StateMachineConfig
├── @Configuration
├── Methods:
│   ├── orderStateMachine(): StateMachine<OrderStatus, OrderEvent>
│   │   ├── Configure states
│   │   ├── Configure transitions
│   │   ├── Configure guards (validation)
│   │   └── Configure actions (state change handlers)
│   └── returnStateMachine(): StateMachine<ReturnStatus, ReturnEvent>
│       ├── Configure states
│       ├── Configure transitions
│       ├── Configure guards (validation)
│       └── Configure actions (state change handlers)
└── State Machine Events:
    ├── OrderEvent: PAYMENT_RECEIVED, PAYMENT_FAILED, PROCESS_STARTED, SHIPPED, DELIVERED, CANCELLED
    └── ReturnEvent: APPROVED, REJECTED, SHIPPED_BACK, RECEIVED, REFUNDED
```

### 10.2 AsyncConfig

```
AsyncConfig
├── @Configuration
├── @EnableAsync
├── Methods:
│   └── taskExecutor(): Executor
│       ├── Configure thread pool
│       ├── Set core pool size
│       ├── Set max pool size
│       └── Set queue capacity
```

### 10.3 WebConfig

```
WebConfig
├── @Configuration
├── Methods:
│   └── corsConfigurer(): WebMvcConfigurer
│       └── Configure CORS settings
```

## 11. Exception Handling

### 11.1 Custom Exceptions

```
OrderNotFoundException extends RuntimeException
├── Fields:
│   └── orderId: Long

InvalidStateTransitionException extends RuntimeException
├── Fields:
│   ├── currentState: String
│   ├── targetState: String
│   └── entityType: EntityType

PaymentProcessingException extends RuntimeException
├── Fields:
│   ├── paymentId: Long
│   └── errorMessage: String

ReturnNotAllowedException extends RuntimeException
├── Fields:
│   ├── orderId: Long
│   └── reason: String
```

### 11.2 GlobalExceptionHandler

```
GlobalExceptionHandler
├── @ControllerAdvice
├── Methods:
│   ├── handleOrderNotFound(OrderNotFoundException): ResponseEntity<ErrorResponse>
│   ├── handleInvalidStateTransition(InvalidStateTransitionException): ResponseEntity<ErrorResponse>
│   ├── handlePaymentProcessing(PaymentProcessingException): ResponseEntity<ErrorResponse>
│   ├── handleReturnNotAllowed(ReturnNotAllowedException): ResponseEntity<ErrorResponse>
│   └── handleGenericException(Exception): ResponseEntity<ErrorResponse>
└── ErrorResponse Structure:
    ├── timestamp: LocalDateTime
    ├── status: HttpStatus
    ├── error: String
    ├── message: String
    └── path: String
```

## 12. Database Schema Details

### 12.1 Table Relationships

```
orders (1) ────< (N) order_items
orders (1) ────< (N) payments
orders (1) ────< (N) returns
returns (1) ────< (1) refunds
orders (1) ────< (N) state_transitions (via entityId where entityType='ORDER')
returns (1) ────< (N) state_transitions (via entityId where entityType='RETURN')
```

### 12.2 Indexes

```
orders
├── idx_order_number (orderNumber) - UNIQUE
├── idx_customer_id (customerId)
└── idx_status (status)

payments
├── idx_order_id (orderId)
├── idx_transaction_id (transactionId) - UNIQUE
└── idx_status (status)

returns
├── idx_order_id (orderId)
└── idx_status (status)

state_transitions
├── idx_entity_type_id (entityType, entityId)
└── idx_timestamp (timestamp)
```

### 12.3 Constraints

```
orders
├── CHECK: totalAmount > 0
└── CHECK: status IN (valid OrderStatus values)

order_items
├── CHECK: quantity > 0
└── CHECK: price >= 0

payments
├── CHECK: amount > 0
└── CHECK: status IN (valid PaymentStatus values)

returns
└── CHECK: status IN (valid ReturnStatus values)

refunds
├── CHECK: amount > 0
└── CHECK: status IN (valid RefundStatus values)
```

## 13. Transaction Management

### 13.1 Transaction Boundaries

```
OrderService.createOrder()
├── @Transactional
└── Operations:
    ├── Create Order entity
    ├── Create OrderItem entities
    ├── Initialize order state (PENDING_PAYMENT)
    └── Log initial state transition

PaymentService.processPayment()
├── @Transactional
└── Operations:
    ├── Create Payment entity
    ├── Call PaymentGatewayClient
    ├── Update Payment status
    ├── Transition Order state (PENDING_PAYMENT → PAID)
    └── Log state transition

ReturnService.approveReturn()
├── @Transactional
└── Operations:
    ├── Update Return status (REQUESTED → APPROVED)
    ├── Update Return.approvedBy and approvedAt
    └── Log state transition

RefundService.processRefund()
├── @Transactional
└── Operations:
    ├── Update Refund status (PENDING → PROCESSING)
    ├── Call PaymentGatewayClient
    ├── Update Refund status (PROCESSING → COMPLETED)
    ├── Transition Return state (RECEIVED → COMPLETED)
    └── Log state transitions
```

## 14. Validation Rules

### 14.1 Order Validation

```
Order Creation:
├── Customer ID must exist
├── At least one order item required
├── All order items must have quantity > 0
├── All order items must have price >= 0
└── Shipping address must be provided

Order Cancellation:
├── Order must be in PENDING_PAYMENT or PAID state
├── If PAID, order must not be in PROCESSING_IN_WAREHOUSE
└── No active returns for the order
```

### 14.2 Return Validation

```
Return Request:
├── Order must exist
├── Order must be in DELIVERED state
├── Return reason must be provided
└── No existing active return for the order

Return Approval:
├── Return must be in REQUESTED state
├── Manager ID must be provided
└── Comments optional but recommended
```

### 14.3 State Transition Validation

```
Order State Transitions:
├── PENDING_PAYMENT → PAID: Payment must be successful
├── PENDING_PAYMENT → CANCELLED: Always allowed
├── PAID → PROCESSING_IN_WAREHOUSE: Always allowed
├── PAID → CANCELLED: Only if not in PROCESSING_IN_WAREHOUSE
├── PROCESSING_IN_WAREHOUSE → SHIPPED: Always allowed
└── SHIPPED → DELIVERED: Always allowed

Return State Transitions:
├── REQUESTED → APPROVED: Manager approval required
├── REQUESTED → REJECTED: Manager rejection
├── APPROVED → IN_TRANSIT: Customer action
├── IN_TRANSIT → RECEIVED: Warehouse confirmation
└── RECEIVED → COMPLETED: Refund must be processed
```

## 15. API Endpoint Summary

```
Order Management:
├── POST   /api/orders
├── GET    /api/orders/{orderId}
├── GET    /api/orders/order-number/{orderNumber}
├── GET    /api/orders/customer/{customerId}
├── PUT    /api/orders/{orderId}/status
├── POST   /api/orders/{orderId}/cancel
└── GET    /api/orders/{orderId}/history

Payment Management:
├── POST   /api/payments
├── GET    /api/payments/{paymentId}
├── GET    /api/payments/order/{orderId}
└── POST   /api/payments/webhook

Return Management:
├── POST   /api/returns
├── GET    /api/returns/{returnId}
├── GET    /api/returns/order/{orderId}
├── POST   /api/returns/{returnId}/approve
├── POST   /api/returns/{returnId}/reject
├── PUT    /api/returns/{returnId}/in-transit
├── PUT    /api/returns/{returnId}/received
└── GET    /api/returns/{returnId}/history

Refund Management:
├── POST   /api/refunds/return/{returnId}
├── GET    /api/refunds/{refundId}
├── GET    /api/refunds/return/{returnId}
└── POST   /api/refunds/{refundId}/process
```

## 16. Application Properties Structure

```
application.yml
├── server:
│   └── port: 8080
├── spring:
│   ├── datasource:
│   │   ├── url: jdbc:postgresql://localhost:5432/articurated
│   │   ├── username: ${DB_USERNAME}
│   │   └── password: ${DB_PASSWORD}
│   ├── jpa:
│   │   ├── hibernate.ddl-auto: validate
│   │   └── show-sql: false
│   └── flyway:
│       ├── enabled: true
│       └── locations: classpath:db/migration
├── payment:
│   ├── gateway:
│   │   ├── base-url: ${PAYMENT_GATEWAY_URL}
│   │   └── api-key: ${PAYMENT_GATEWAY_API_KEY}
│   └── webhook:
│       └── secret: ${PAYMENT_WEBHOOK_SECRET}
└── email:
    ├── smtp:
    │   ├── host: ${SMTP_HOST}
    │   ├── port: ${SMTP_PORT}
    │   ├── username: ${SMTP_USERNAME}
    │   └── password: ${SMTP_PASSWORD}
```

---

## Summary

This low-level design provides:
- ✅ Complete package structure
- ✅ Detailed entity models with relationships
- ✅ Repository interfaces with query methods
- ✅ Service layer with method signatures
- ✅ REST controller endpoints
- ✅ DTO structures for requests/responses
- ✅ State machine configuration details
- ✅ Integration client interfaces
- ✅ Scheduled job specifications
- ✅ Exception handling structure
- ✅ Database schema with indexes and constraints
- ✅ Transaction boundaries
- ✅ Validation rules
- ✅ Complete API endpoint listing

The design is ready for implementation with clear separation of concerns and well-defined interfaces between layers.
