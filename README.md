# ArtiCurated Order Management System

A robust Spring Boot application for managing orders, payments, returns, and refunds with complex state management and multi-step workflows.

## Features

- **Complex Order State Management**: State machine enforcing valid transitions
  - PENDING_PAYMENT → PAID → PROCESSING_IN_WAREHOUSE → SHIPPED → DELIVERED
  - Cancellation allowed from PENDING_PAYMENT or PAID (if not yet processed)

- **Multi-Step Returns Workflow**: Complete return lifecycle management
  - REQUESTED → APPROVED/REJECTED → IN_TRANSIT → RECEIVED → COMPLETED
  - Returns only allowed for DELIVERED orders

- **State Change History**: Complete audit trail of all state transitions

- **Background Job Processing**: Scheduled tasks for payment status checks and refund processing

- **RESTful API**: Comprehensive REST endpoints for all operations

## Technology Stack

- Java 21
- Spring Boot 3.2.0
- Spring Data JPA
- PostgreSQL
- Maven
- Flyway (Database migrations)
- Lombok

## Prerequisites

- Java 21 or higher
- Maven 3.6+
- PostgreSQL 15+
- IDE (IntelliJ IDEA, Eclipse, or VS Code)

## Setup Instructions

### 1. Database Setup

Create a PostgreSQL database:

```sql
CREATE DATABASE articurated;
```

### 2. Configure Application

Update `src/main/resources/application.yml` with your database credentials:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/articurated
    username: your_username
    password: your_password
```

### 3. Build the Project

```bash
mvn clean install
```

### 4. Run the Application

```bash
mvn spring-boot:run
```

Or run the main class:
```bash
java -jar target/order-management-1.0.0.jar
```

The application will start on `http://localhost:8080`

## API Endpoints

### Order Management

- `POST /api/orders` - Create a new order
- `GET /api/orders/{orderId}` - Get order by ID
- `GET /api/orders/order-number/{orderNumber}` - Get order by order number
- `GET /api/orders/customer/{customerId}` - Get orders by customer ID
- `PUT /api/orders/{orderId}/status` - Update order status
- `POST /api/orders/{orderId}/cancel` - Cancel an order
- `GET /api/orders/{orderId}/history` - Get order state history

### Payment Management

- `POST /api/payments` - Process a payment
- `GET /api/payments/{paymentId}` - Get payment by ID
- `GET /api/payments/order/{orderId}` - Get payments by order ID
- `POST /api/payments/webhook` - Handle payment webhook

### Return Management

- `POST /api/returns` - Create a return request
- `GET /api/returns/{returnId}` - Get return by ID
- `GET /api/returns/order/{orderId}` - Get returns by order ID
- `POST /api/returns/{returnId}/approve` - Approve a return
- `POST /api/returns/{returnId}/reject` - Reject a return
- `PUT /api/returns/{returnId}/in-transit` - Mark return as in transit
- `PUT /api/returns/{returnId}/received` - Mark return as received
- `GET /api/returns/{returnId}/history` - Get return state history

### Refund Management

- `POST /api/refunds/return/{returnId}` - Create a refund
- `GET /api/refunds/{refundId}` - Get refund by ID
- `GET /api/refunds/return/{returnId}` - Get refund by return ID
- `POST /api/refunds/{refundId}/process` - Process a refund

## Example API Calls

### Create Order

```bash
curl -X POST http://localhost:8080/api/orders \
  -H "Content-Type: application/json" \
  -d '{
    "customerId": 1,
    "orderItems": [
      {
        "productId": 101,
        "productName": "Artisan Vase",
        "quantity": 2,
        "price": 150.00
      }
    ],
    "shippingAddress": "123 Main St, City, Country"
  }'
```

### Process Payment

```bash
curl -X POST http://localhost:8080/api/payments \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "paymentMethod": "CREDIT_CARD",
    "amount": 300.00,
    "paymentDetails": {}
  }'
```

### Create Return Request

```bash
curl -X POST http://localhost:8080/api/returns \
  -H "Content-Type: application/json" \
  -d '{
    "orderId": 1,
    "returnReason": "Item damaged during shipping"
  }'
```

## Project Structure

```
src/main/java/com/articurated/ordermanagement/
├── config/              # Configuration classes
├── controller/          # REST controllers
├── service/             # Business logic services
├── repository/          # Data access repositories
├── model/
│   ├── entity/         # JPA entities
│   ├── enums/          # Enum classes
│   ├── dto/            # Data transfer objects
│   └── exception/      # Custom exceptions
├── integration/        # Third-party integrations
└── scheduler/          # Background jobs
```

## State Machine Rules

### Order States

- **PENDING_PAYMENT**: Initial state when order is created
- **PAID**: Payment successfully processed
- **PROCESSING_IN_WAREHOUSE**: Order is being prepared
- **SHIPPED**: Order has been shipped
- **DELIVERED**: Order has been delivered (terminal state)
- **CANCELLED**: Order cancelled (terminal state)

### Return States

- **REQUESTED**: Customer initiated return
- **APPROVED**: Manager approved the return
- **REJECTED**: Manager rejected the return (terminal state)
- **IN_TRANSIT**: Customer shipped item back
- **RECEIVED**: Warehouse confirmed receipt
- **COMPLETED**: Refund processed (terminal state)

## Background Jobs

- **Payment Status Checker**: Runs every 10 minutes to check pending payments
- **Refund Processor**: Runs every 15 minutes to process pending refunds

## Database Schema

The application uses Flyway for database migrations. The initial schema includes:

- `orders` - Order information
- `order_items` - Order line items
- `payments` - Payment records
- `returns` - Return requests
- `refunds` - Refund records
- `state_transitions` - Audit log of all state changes

## Testing

Run tests with:

```bash
mvn test
```

## Notes

- Payment gateway and email integrations are simulated. Replace with actual implementations in production.
- State machine logic is implemented using switch expressions for simplicity.
- All state transitions are logged in the `state_transitions` table for auditing.

## License

This project is for demonstration purposes.
