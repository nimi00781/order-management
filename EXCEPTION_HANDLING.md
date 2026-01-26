# Exception Handling Documentation

## Overview

The application implements comprehensive global exception handling at the controller level using Spring's `@RestControllerAdvice`. All exceptions are caught, logged, and converted to standardized error responses.

## Custom Exceptions

### Business Exceptions

1. **OrderNotFoundException**
   - Thrown when an order is not found
   - HTTP Status: 404 NOT FOUND

2. **ReturnNotFoundException**
   - Thrown when a return is not found
   - HTTP Status: 404 NOT FOUND

3. **RefundNotFoundException**
   - Thrown when a refund is not found
   - HTTP Status: 404 NOT FOUND

4. **PaymentNotFoundException**
   - Thrown when a payment is not found
   - HTTP Status: 404 NOT FOUND

5. **InvalidStateTransitionException**
   - Thrown when an invalid state transition is attempted
   - HTTP Status: 400 BAD REQUEST

6. **PaymentProcessingException**
   - Thrown when payment processing fails
   - HTTP Status: 500 INTERNAL SERVER ERROR

7. **ReturnNotAllowedException**
   - Thrown when a return request is not allowed
   - HTTP Status: 400 BAD REQUEST

8. **BusinessRuleViolationException**
   - Thrown when business rules are violated
   - HTTP Status: 400 BAD REQUEST

## Exception Handlers

### Custom Business Exceptions

All custom business exceptions are handled with appropriate HTTP status codes and error messages:

- `OrderNotFoundException` → 404
- `ReturnNotFoundException` → 404
- `RefundNotFoundException` → 404
- `PaymentNotFoundException` → 404
- `InvalidStateTransitionException` → 400
- `PaymentProcessingException` → 500
- `ReturnNotAllowedException` → 400
- `BusinessRuleViolationException` → 400

### JPA and Database Exceptions

1. **EntityNotFoundException**
   - Handles JPA entity not found scenarios
   - HTTP Status: 404 NOT FOUND

2. **DataIntegrityViolationException**
   - Handles database constraint violations
   - Detects duplicate entries and foreign key violations
   - HTTP Status: 409 CONFLICT
   - Provides user-friendly messages

### Validation Exceptions

1. **MethodArgumentNotValidException**
   - Handles `@Valid` annotation validation failures on request bodies
   - Returns detailed field-level error messages
   - HTTP Status: 400 BAD REQUEST
   - Response includes all validation errors

2. **ConstraintViolationException**
   - Handles validation failures on path/query parameters
   - Returns violation details
   - HTTP Status: 400 BAD REQUEST

### HTTP Request Exceptions

1. **HttpRequestMethodNotSupportedException**
   - Handles unsupported HTTP methods
   - HTTP Status: 405 METHOD NOT ALLOWED

2. **HttpMediaTypeNotSupportedException**
   - Handles unsupported content types
   - HTTP Status: 415 UNSUPPORTED MEDIA TYPE

3. **MissingServletRequestParameterException**
   - Handles missing required request parameters
   - HTTP Status: 400 BAD REQUEST

4. **MethodArgumentTypeMismatchException**
   - Handles type mismatches in path/query parameters
   - HTTP Status: 400 BAD REQUEST

5. **HttpMessageNotReadableException**
   - Handles malformed request bodies
   - HTTP Status: 400 BAD REQUEST

6. **NoHandlerFoundException**
   - Handles requests to non-existent endpoints
   - HTTP Status: 404 NOT FOUND

### Java Standard Exceptions

1. **IllegalStateException**
   - Handles illegal state scenarios
   - HTTP Status: 400 BAD REQUEST

2. **IllegalArgumentException**
   - Handles invalid arguments
   - HTTP Status: 400 BAD REQUEST

3. **NullPointerException**
   - Handles null pointer exceptions (should not occur in production)
   - HTTP Status: 500 INTERNAL SERVER ERROR
   - Generic message to avoid exposing internal details

### Generic Exception Handler

- **Exception** (catch-all)
  - Handles any unhandled exceptions
  - HTTP Status: 500 INTERNAL SERVER ERROR
  - Generic message for security

## Error Response Format

### Standard Error Response

```json
{
  "timestamp": "2024-01-15T10:30:00",
  "status": 404,
  "error": "Order Not Found",
  "message": "Order not found with id: 123",
  "path": "/api/orders/123"
}
```

### Validation Error Response

```json
{
  "customerId": "Customer ID is required",
  "orderItems": "At least one order item is required",
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Validation Failed",
  "path": "/api/orders"
}
```

### Constraint Violation Response

```json
{
  "violations": [
    {
      "field": "orderId",
      "message": "must not be null"
    }
  ],
  "timestamp": "2024-01-15T10:30:00",
  "status": 400,
  "error": "Constraint Violation",
  "path": "/api/returns"
}
```

## Logging

All exceptions are logged with appropriate log levels:

- **WARN**: Business rule violations, validation failures, not found exceptions
- **ERROR**: Payment processing failures, data integrity violations, unexpected errors

Logging includes:
- Exception type and message
- Relevant entity IDs
- Request path
- Stack trace for errors

## Best Practices

1. **Use Custom Exceptions**: Always use custom exceptions for business logic errors
2. **Provide Context**: Include relevant IDs and context in exception messages
3. **Log Appropriately**: Use WARN for expected errors, ERROR for unexpected ones
4. **Don't Expose Internals**: Generic exception handler hides internal details
5. **Consistent Format**: All errors follow the same response structure

## Example Usage

### Throwing Custom Exceptions

```java
// In service layer
if (order == null) {
    throw new OrderNotFoundException(orderId);
}

if (!order.canBeCancelled()) {
    throw new BusinessRuleViolationException(
        "ORDER", "CANCELLATION",
        "Order cannot be cancelled in current state: " + order.getStatus());
}
```

### Exception Handling Flow

1. Exception thrown in service/controller
2. Caught by `GlobalExceptionHandler`
3. Logged with appropriate level
4. Converted to `ErrorResponse` DTO
5. Returned with appropriate HTTP status

## Testing Exception Handling

To test exception handling:

1. **Not Found**: Request non-existent resource
   ```bash
   GET /api/orders/99999
   ```

2. **Validation**: Send invalid request body
   ```bash
   POST /api/orders
   {
     "customerId": null,
     "orderItems": []
   }
   ```

3. **Invalid State**: Attempt invalid state transition
   ```bash
   PUT /api/orders/1/status
   {
     "newStatus": "DELIVERED"
   }
   ```
   (When order is in PENDING_PAYMENT state)

4. **Method Not Allowed**: Use wrong HTTP method
   ```bash
   DELETE /api/orders/1
   ```

## Summary

The global exception handler provides:
- ✅ Comprehensive coverage of all exception types
- ✅ Consistent error response format
- ✅ Appropriate HTTP status codes
- ✅ Detailed logging for debugging
- ✅ User-friendly error messages
- ✅ Security (no internal details exposed)
