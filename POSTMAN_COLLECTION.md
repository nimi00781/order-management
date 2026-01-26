# API Integration Testing Documentation

## Overview

Comprehensive integration tests have been created for all REST API endpoints. These tests verify the complete request-response cycle including validation, business logic, state transitions, and error handling.

## Test Structure

All integration tests use:
- `@SpringBootTest` - Full Spring Boot application context
- `@AutoConfigureMockMvc` - MockMvc for HTTP testing
- `@ActiveProfiles("test")` - Test profile configuration
- `@Transactional` - Rollback after each test
- H2 in-memory database for testing

## Test Coverage

### Order API Tests (`OrderApiIntegrationTest`)

#### ✅ Create Order
- `testCreateOrder_API` - Successfully creates order
- `testCreateOrder_ValidationError_MissingCustomerId` - Validates required fields
- `testCreateOrder_ValidationError_EmptyOrderItems` - Validates order items
- `testCreateOrder_InvalidPrice` - Validates price constraints
- `testCreateOrder_InvalidQuantity` - Validates quantity constraints

#### ✅ Get Order
- `testGetOrderById_API` - Retrieves order by ID
- `testGetOrderById_NotFound` - Handles not found scenario
- `testGetOrderByOrderNumber_API` - Retrieves order by order number
- `testGetOrdersByCustomerId_API` - Retrieves orders by customer

#### ✅ Update Order
- `testUpdateOrderStatus_API` - Updates order status
- `testCancelOrder_API` - Cancels order

#### ✅ Order History
- `testGetOrderStateHistory_API` - Retrieves state transition history

### Payment API Tests (`PaymentApiIntegrationTest`)

#### ✅ Process Payment
- `testProcessPayment_API` - Successfully processes payment
- `testProcessPayment_ValidationError_MissingOrderId` - Validates required fields
- `testProcessPayment_ValidationError_InvalidAmount` - Validates amount constraints
- `testProcessPayment_OrderNotFound` - Handles order not found
- `testProcessPayment_OrderNotInPendingPaymentState` - Validates order state

#### ✅ Get Payment
- `testGetPaymentById_API` - Retrieves payment by ID
- `testGetPaymentById_NotFound` - Handles not found scenario
- `testGetPaymentsByOrderId_API` - Retrieves payments by order

#### ✅ Webhook
- `testHandlePaymentWebhook_API` - Handles payment webhook

### Return API Tests (`ReturnApiIntegrationTest`)

#### ✅ Create Return
- `testCreateReturnRequest_API` - Successfully creates return request
- `testCreateReturnRequest_ValidationError_MissingOrderId` - Validates required fields
- `testCreateReturnRequest_ValidationError_MissingReturnReason` - Validates return reason
- `testCreateReturnRequest_OrderNotFound` - Handles order not found
- `testCreateReturnRequest_OrderNotDelivered` - Validates order must be DELIVERED

#### ✅ Get Return
- `testGetReturnById_API` - Retrieves return by ID
- `testGetReturnById_NotFound` - Handles not found scenario
- `testGetReturnsByOrderId_API` - Retrieves returns by order

#### ✅ Return Workflow
- `testApproveReturn_API` - Approves return request
- `testRejectReturn_API` - Rejects return request
- `testMarkReturnInTransit_API` - Marks return as in transit
- `testMarkReturnReceived_API` - Marks return as received

#### ✅ Return History
- `testGetReturnStateHistory_API` - Retrieves state transition history

### Refund API Tests (`RefundApiIntegrationTest`)

#### ✅ Create Refund
- `testCreateRefund_API` - Successfully creates refund
- `testCreateRefund_ReturnNotFound` - Handles return not found
- `testCreateRefund_ReturnNotInReceivedState` - Validates return state
- `testCreateRefund_DuplicateRefund` - Prevents duplicate refunds

#### ✅ Get Refund
- `testGetRefundById_API` - Retrieves refund by ID
- `testGetRefundById_NotFound` - Handles not found scenario
- `testGetRefundByReturnId_API` - Retrieves refund by return ID

#### ✅ Process Refund
- `testProcessRefund_API` - Successfully processes refund
- `testProcessRefund_NotFound` - Handles refund not found

## Running API Tests

### Run All Integration Tests
```bash
mvn test -Dtest=*IntegrationTest
```

### Run Specific Test Class
```bash
mvn test -Dtest=OrderApiIntegrationTest
```

### Run with Coverage
```bash
mvn test jacoco:report
```

## Test Configuration

### Test Profile (`application-test.yml`)
- Uses H2 in-memory database
- Disables Flyway migrations
- Configures logging levels

### Test Data Setup
- Each test sets up required data in `@BeforeEach`
- Tests are isolated and transactional
- Data is rolled back after each test

## API Endpoints Tested

### Order Management
- `POST /api/orders` - Create order
- `GET /api/orders/{orderId}` - Get order by ID
- `GET /api/orders/order-number/{orderNumber}` - Get order by order number
- `GET /api/orders/customer/{customerId}` - Get orders by customer
- `PUT /api/orders/{orderId}/status` - Update order status
- `POST /api/orders/{orderId}/cancel` - Cancel order
- `GET /api/orders/{orderId}/history` - Get order state history

### Payment Management
- `POST /api/payments` - Process payment
- `GET /api/payments/{paymentId}` - Get payment by ID
- `GET /api/payments/order/{orderId}` - Get payments by order
- `POST /api/payments/webhook` - Handle payment webhook

### Return Management
- `POST /api/returns` - Create return request
- `GET /api/returns/{returnId}` - Get return by ID
- `GET /api/returns/order/{orderId}` - Get returns by order
- `POST /api/returns/{returnId}/approve` - Approve return
- `POST /api/returns/{returnId}/reject` - Reject return
- `PUT /api/returns/{returnId}/in-transit` - Mark return in transit
- `PUT /api/returns/{returnId}/received` - Mark return received
- `GET /api/returns/{returnId}/history` - Get return state history

### Refund Management
- `POST /api/refunds/return/{returnId}` - Create refund
- `GET /api/refunds/{refundId}` - Get refund by ID
- `GET /api/refunds/return/{returnId}` - Get refund by return ID
- `POST /api/refunds/{refundId}/process` - Process refund

## Test Scenarios Covered

### Success Scenarios
- ✅ All CRUD operations
- ✅ State transitions
- ✅ Business workflows
- ✅ Data retrieval

### Validation Scenarios
- ✅ Missing required fields
- ✅ Invalid data types
- ✅ Constraint violations
- ✅ Business rule violations

### Error Scenarios
- ✅ Resource not found (404)
- ✅ Bad request (400)
- ✅ Business rule violations
- ✅ Invalid state transitions

## State Machine Testing

The integration tests verify state machine transitions:
- **Order States**: PENDING_PAYMENT → PAID → PROCESSING_IN_WAREHOUSE → SHIPPED → DELIVERED
- **Return States**: REQUESTED → APPROVED → IN_TRANSIT → RECEIVED → COMPLETED

## Best Practices

1. **Isolated Tests**: Each test is independent
2. **Transactional**: Data is rolled back after each test
3. **Realistic Data**: Tests use realistic test data
4. **Full Stack**: Tests the complete request-response cycle
5. **Error Handling**: Tests both success and failure paths
6. **State Validation**: Verifies state transitions are enforced

## Test Execution Results

When running the tests, you should see:
- All API endpoints tested
- Success and error scenarios covered
- State transitions validated
- Business rules enforced
- Proper HTTP status codes returned

## Notes

- Tests use H2 in-memory database (no PostgreSQL required for testing)
- All tests are transactional and rollback after execution
- MockMvc is used for HTTP request simulation
- ObjectMapper is used for JSON serialization/deserialization
- Test profile is automatically activated during test execution
