package com.articurated.ordermanagement.model.exception;

import com.articurated.ordermanagement.model.dto.response.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    // ========== Custom Business Exceptions ==========

    @ExceptionHandler(OrderNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleOrderNotFound(OrderNotFoundException ex, WebRequest request) {
        log.warn("Order not found: {}", ex.getOrderId());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Order Not Found",
                ex.getMessage(),
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ReturnNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleReturnNotFound(ReturnNotFoundException ex, WebRequest request) {
        log.warn("Return not found: {}", ex.getReturnId());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Return Not Found",
                ex.getMessage(),
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(RefundNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleRefundNotFound(RefundNotFoundException ex, WebRequest request) {
        log.warn("Refund not found: {}", ex.getRefundId());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Refund Not Found",
                ex.getMessage(),
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(PaymentNotFoundException.class)
    public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex, WebRequest request) {
        log.warn("Payment not found: {}", ex.getPaymentId());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Payment Not Found",
                ex.getMessage(),
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(InvalidStateTransitionException ex, WebRequest request) {
        log.warn("Invalid state transition: {} -> {} for {}", ex.getCurrentState(), ex.getTargetState(), ex.getEntityType());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid State Transition",
                ex.getMessage(),
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(PaymentProcessingException.class)
    public ResponseEntity<ErrorResponse> handlePaymentProcessing(PaymentProcessingException ex, WebRequest request) {
        log.error("Payment processing failed for payment ID: {}, Error: {}", ex.getPaymentId(), ex.getErrorMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Payment Processing Failed",
                ex.getMessage(),
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(ReturnNotAllowedException.class)
    public ResponseEntity<ErrorResponse> handleReturnNotAllowed(ReturnNotAllowedException ex, WebRequest request) {
        log.warn("Return not allowed for order ID: {}, Reason: {}", ex.getOrderId(), ex.getReason());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Return Not Allowed",
                ex.getMessage(),
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(BusinessRuleViolationException.class)
    public ResponseEntity<ErrorResponse> handleBusinessRuleViolation(BusinessRuleViolationException ex, WebRequest request) {
        log.warn("Business rule violation: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Business Rule Violation",
                ex.getMessage(),
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    // ========== JPA and Database Exceptions ==========

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException ex, WebRequest request) {
        log.warn("Entity not found: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Entity Not Found",
                ex.getMessage(),
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, WebRequest request) {
        log.error("Data integrity violation: {}", ex.getMessage());
        String message = "Data integrity violation. This may be due to duplicate entry or constraint violation.";
        if (ex.getCause() != null && ex.getCause().getMessage() != null) {
            String causeMessage = ex.getCause().getMessage();
            if (causeMessage.contains("unique constraint") || causeMessage.contains("duplicate key")) {
                message = "Duplicate entry detected. Please check your input.";
            } else if (causeMessage.contains("foreign key constraint")) {
                message = "Referenced entity does not exist. Please check your input.";
            }
        }
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.CONFLICT,
                "Data Integrity Violation",
                message,
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }

    // ========== Validation Exceptions ==========

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("Validation failed: {}", ex.getBindingResult().getAllErrors());
        Map<String, Object> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = error instanceof FieldError ? ((FieldError) error).getField() : error.getObjectName();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("error", "Validation Failed");
        errors.put("path", getPath(request));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex, WebRequest request) {
        log.warn("Constraint violation: {}", ex.getConstraintViolations());
        Map<String, Object> errors = new HashMap<>();
        errors.put("violations", ex.getConstraintViolations().stream()
                .map(violation -> {
                    Map<String, String> violationMap = new HashMap<>();
                    violationMap.put("field", violation.getPropertyPath().toString());
                    violationMap.put("message", violation.getMessage());
                    return violationMap;
                })
                .collect(Collectors.toList()));
        errors.put("timestamp", LocalDateTime.now());
        errors.put("status", HttpStatus.BAD_REQUEST.value());
        errors.put("error", "Constraint Violation");
        errors.put("path", getPath(request));
        return new ResponseEntity<>(errors, HttpStatus.BAD_REQUEST);
    }

    // ========== HTTP Request Exceptions ==========

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, WebRequest request) {
        log.warn("HTTP method not supported: {}", ex.getMethod());
        String message = String.format("Method '%s' is not supported for this endpoint. Supported methods: %s",
                ex.getMethod(), ex.getSupportedHttpMethods());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.METHOD_NOT_ALLOWED,
                "Method Not Allowed",
                message,
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.METHOD_NOT_ALLOWED);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex, WebRequest request) {
        log.warn("Media type not supported: {}", ex.getContentType());
        String message = String.format("Media type '%s' is not supported. Supported types: %s",
                ex.getContentType(), ex.getSupportedMediaTypes());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "Unsupported Media Type",
                message,
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.UNSUPPORTED_MEDIA_TYPE);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException ex, WebRequest request) {
        log.warn("Missing request parameter: {}", ex.getParameterName());
        String message = String.format("Required parameter '%s' is missing", ex.getParameterName());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Missing Request Parameter",
                message,
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex, WebRequest request) {
        log.warn("Argument type mismatch: {} should be of type {}", ex.getName(), ex.getRequiredType());
        String message = String.format("Parameter '%s' should be of type %s", ex.getName(),
                ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown");
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Type Mismatch",
                message,
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleMessageNotReadable(HttpMessageNotReadableException ex, WebRequest request) {
        log.warn("Message not readable: {}", ex.getMessage());
        String message = "Request body is malformed or invalid. Please check your JSON format.";
        if (ex.getCause() != null) {
            message = ex.getCause().getMessage();
        }
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Malformed Request",
                message,
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ErrorResponse> handleNoHandlerFound(NoHandlerFoundException ex, WebRequest request) {
        log.warn("No handler found for: {} {}", ex.getHttpMethod(), ex.getRequestURL());
        String message = String.format("No handler found for %s %s", ex.getHttpMethod(), ex.getRequestURL());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.NOT_FOUND,
                "Endpoint Not Found",
                message,
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    // ========== Java Standard Exceptions ==========

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex, WebRequest request) {
        log.warn("Illegal state: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Illegal State",
                ex.getMessage(),
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex, WebRequest request) {
        log.warn("Illegal argument: {}", ex.getMessage());
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.BAD_REQUEST,
                "Invalid Argument",
                ex.getMessage(),
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(NullPointerException.class)
    public ResponseEntity<ErrorResponse> handleNullPointer(NullPointerException ex, WebRequest request) {
        log.error("Null pointer exception", ex);
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "A null pointer exception occurred. Please contact support.",
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ========== Generic Exception Handler ==========

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(Exception ex, WebRequest request) {
        log.error("Unexpected error occurred", ex);
        ErrorResponse errorResponse = buildErrorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Internal Server Error",
                "An unexpected error occurred. Please try again later.",
                request
        );
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // ========== Helper Methods ==========

    private ErrorResponse buildErrorResponse(HttpStatus status, String error, String message, WebRequest request) {
        return ErrorResponse.builder()
                .timestamp(LocalDateTime.now())
                .status(status.value())
                .error(error)
                .message(message)
                .path(getPath(request))
                .build();
    }

    private String getPath(WebRequest request) {
        String path = request.getDescription(false);
        return path.replace("uri=", "");
    }
}
