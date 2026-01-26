# Unit Testing Documentation

## Overview

Comprehensive unit tests have been implemented for all service and controller classes using JUnit 5 and Mockito. The tests follow best practices and provide good coverage of business logic, edge cases, and error scenarios.

## Test Structure

### Service Tests

All service tests use:
- `@ExtendWith(MockitoExtension.class)` for Mockito integration
- `@Mock` for mocking dependencies
- `@InjectMocks` for injecting mocks into the service under test
- `@BeforeEach` for test setup

#### OrderServiceTest
- ✅ `testCreateOrder_Success` - Creates order successfully
- ✅ `testGetOrderById_Success` - Retrieves order by ID
- ✅ `testGetOrderById_NotFound` - Handles order not found
- ✅ `testGetOrderByOrderNumber_Success` - Retrieves order by order number
- ✅ `testGetOrdersByCustomerId_Success` - Retrieves orders by customer
- ✅ `testUpdateOrderStatus_Success` - Updates order status
- ✅ `testCancelOrder_Success` - Cancels order successfully
- ✅ `testCancelOrder_CannotBeCancelled` - Handles cancellation when not allowed
- ✅ `testGetOrderStateHistory_Success` - Retrieves state history

#### PaymentServiceTest
- ✅ `testProcessPayment_Success` - Processes payment successfully
- ✅ `testProcessPayment_OrderNotFound` - Handles order not found
- ✅ `testProcessPayment_OrderNotInPendingPaymentState` - Validates order state
- ✅ `testProcessPayment_GatewayFailure` - Handles gateway failure
- ✅ `testProcessPayment_GatewayException` - Handles gateway exceptions
- ✅ `testGetPaymentById_Success` - Retrieves payment by ID
- ✅ `testGetPaymentById_NotFound` - Handles payment not found
- ✅ `testGetPaymentsByOrderId_Success` - Retrieves payments by order
- ✅ `testCheckPaymentStatus_Success` - Checks payment status

#### ReturnServiceTest
- ✅ `testCreateReturnRequest_Success` - Creates return request
- ✅ `testCreateReturnRequest_OrderNotFound` - Handles order not found
- ✅ `testCreateReturnRequest_OrderNotDelivered` - Validates order state
- ✅ `testCreateReturnRequest_ActiveReturnExists` - Prevents duplicate returns
- ✅ `testGetReturnById_Success` - Retrieves return by ID
- ✅ `testGetReturnById_NotFound` - Handles return not found
- ✅ `testApproveReturn_Success` - Approves return
- ✅ `testApproveReturn_NotFound` - Handles return not found
- ✅ `testApproveReturn_CannotBeApproved` - Validates approval state
- ✅ `testRejectReturn_Success` - Rejects return
- ✅ `testRejectReturn_NotInRequestedState` - Validates rejection state
- ✅ `testMarkReturnInTransit_Success` - Marks return in transit
- ✅ `testMarkReturnReceived_Success` - Marks return received
- ✅ `testGetReturnsByOrderId_Success` - Retrieves returns by order

#### RefundServiceTest
- ✅ `testCreateRefund_Success` - Creates refund
- ✅ `testCreateRefund_ReturnNotFound` - Handles return not found
- ✅ `testCreateRefund_ReturnNotInReceivedState` - Validates return state
- ✅ `testCreateRefund_RefundAlreadyExists` - Prevents duplicate refunds
- ✅ `testGetRefundById_Success` - Retrieves refund by ID
- ✅ `testGetRefundById_NotFound` - Handles refund not found
- ✅ `testGetRefundByReturnId_Success` - Retrieves refund by return ID
- ✅ `testProcessRefund_Success` - Processes refund successfully
- ✅ `testProcessRefund_NotFound` - Handles refund not found
- ✅ `testProcessRefund_NotInPendingState` - Validates refund state
- ✅ `testProcessRefund_GatewayFailure` - Handles gateway failure
- ✅ `testProcessRefund_GatewayException` - Handles gateway exceptions
- ✅ `testGetPendingRefunds_Success` - Retrieves pending refunds

#### StateMachineServiceTest
- ✅ `testTransitionOrderState_Success` - Transitions order state
- ✅ `testTransitionOrderState_InvalidTransition` - Handles invalid transitions
- ✅ `testTransitionReturnState_Success` - Transitions return state
- ✅ `testTransitionReturnState_InvalidTransition` - Handles invalid transitions
- ✅ `testValidateOrderTransition_ValidTransitions` - Validates order transitions
- ✅ `testValidateOrderTransition_InvalidTransitions` - Rejects invalid transitions
- ✅ `testValidateReturnTransition_ValidTransitions` - Validates return transitions
- ✅ `testValidateReturnTransition_InvalidTransitions` - Rejects invalid transitions
- ✅ `testGetAllowedOrderTransitions` - Gets allowed order transitions
- ✅ `testGetAllowedReturnTransitions` - Gets allowed return transitions

#### StateHistoryServiceTest
- ✅ `testLogStateTransition_Success` - Logs state transition
- ✅ `testLogStateTransition_WithSystemUser` - Logs system transitions
- ✅ `testGetStateHistory_Success` - Retrieves state history
- ✅ `testGetStateHistoryByDateRange_Success` - Retrieves history by date range
- ✅ `testGetLatestStateTransition_Success` - Gets latest transition
- ✅ `testGetLatestStateTransition_Empty` - Handles empty history

### Controller Tests

All controller tests use:
- `@WebMvcTest` for Spring MVC test slice
- `MockMvc` for HTTP request simulation
- `@MockBean` for mocking services
- `ObjectMapper` for JSON serialization/deserialization

#### OrderControllerTest
- ✅ `testCreateOrder_Success` - Creates order via API
- ✅ `testCreateOrder_ValidationError` - Handles validation errors
- ✅ `testGetOrderById_Success` - Gets order by ID
- ✅ `testGetOrderByOrderNumber_Success` - Gets order by order number
- ✅ `testGetOrdersByCustomerId_Success` - Gets orders by customer
- ✅ `testUpdateOrderStatus_Success` - Updates order status
- ✅ `testCancelOrder_Success` - Cancels order
- ✅ `testGetOrderStateHistory_Success` - Gets state history

#### PaymentControllerTest
- ✅ `testProcessPayment_Success` - Processes payment via API
- ✅ `testProcessPayment_ValidationError` - Handles validation errors
- ✅ `testGetPaymentById_Success` - Gets payment by ID
- ✅ `testGetPaymentsByOrderId_Success` - Gets payments by order
- ✅ `testHandlePaymentWebhook_Success` - Handles webhook

#### ReturnControllerTest
- ✅ `testCreateReturnRequest_Success` - Creates return via API
- ✅ `testCreateReturnRequest_ValidationError` - Handles validation errors
- ✅ `testGetReturnById_Success` - Gets return by ID
- ✅ `testGetReturnsByOrderId_Success` - Gets returns by order
- ✅ `testApproveReturn_Success` - Approves return
- ✅ `testRejectReturn_Success` - Rejects return
- ✅ `testMarkReturnInTransit_Success` - Marks in transit
- ✅ `testMarkReturnReceived_Success` - Marks received
- ✅ `testGetReturnStateHistory_Success` - Gets state history

#### RefundControllerTest
- ✅ `testCreateRefund_Success` - Creates refund via API
- ✅ `testGetRefundById_Success` - Gets refund by ID
- ✅ `testGetRefundByReturnId_Success` - Gets refund by return ID
- ✅ `testProcessRefund_Success` - Processes refund

## Running Tests

### Run All Tests
```bash
mvn test
```

### Run Specific Test Class
```bash
mvn test -Dtest=OrderServiceTest
```

### Run Specific Test Method
```bash
mvn test -Dtest=OrderServiceTest#testCreateOrder_Success
```

### Run Tests with Coverage
```bash
mvn test jacoco:report
```

## Test Coverage

The test suite covers:
- ✅ **Success scenarios** - Happy path for all operations
- ✅ **Error scenarios** - Exception handling and validation
- ✅ **Edge cases** - Boundary conditions and state validations
- ✅ **Business rules** - State machine transitions and validations
- ✅ **Integration points** - Mocked external services

## Best Practices Followed

1. **Arrange-Act-Assert Pattern**: All tests follow AAA pattern
2. **Descriptive Test Names**: Test names clearly describe what they test
3. **Isolated Tests**: Each test is independent and doesn't rely on others
4. **Mocking Dependencies**: External dependencies are properly mocked
5. **Verification**: Important interactions are verified using Mockito
6. **Exception Testing**: Both success and failure paths are tested
7. **Setup Methods**: Common test data is set up in `@BeforeEach`

## Test Data Management

- Test data is created in `@BeforeEach` methods
- Each test uses isolated test data
- Mock objects are configured per test when needed
- Realistic test data that matches production scenarios

## Mocking Strategy

- **Repositories**: Mocked to return test data or empty results
- **External Services**: Payment gateway and email services are mocked
- **State Services**: State machine and history services are mocked
- **Verification**: Important method calls are verified

## Example Test Structure

```java
@Test
void testMethodName_Scenario() {
    // Given - Arrange test data and mocks
    when(mockService.method(any())).thenReturn(result);
    
    // When - Act on the method under test
    Response response = serviceUnderTest.method(request);
    
    // Then - Assert the results
    assertNotNull(response);
    assertEquals(expected, response.getValue());
    verify(mockService, times(1)).method(any());
}
```

## Notes

- All tests use JUnit 5 (Jupiter)
- Mockito is used for mocking dependencies
- Spring Boot Test slice (`@WebMvcTest`) is used for controller tests
- Tests are fast and don't require database or external services
- Test coverage focuses on business logic and error handling
