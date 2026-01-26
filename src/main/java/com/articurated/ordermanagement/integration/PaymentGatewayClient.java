package com.articurated.ordermanagement.integration;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@Component
public class PaymentGatewayClient {

    public PaymentGatewayResponse processPayment(PaymentRequest request) {
        // Simulated payment gateway call
        // In real implementation, this would make HTTP call to payment gateway API
        try {
            // Simulate API call delay
            Thread.sleep(100);
            
            // Simulate success/failure based on amount (for demo purposes)
            boolean success = request.getAmount().compareTo(new BigDecimal("10000")) < 0;
            
            return PaymentGatewayResponse.builder()
                    .success(success)
                    .transactionId(success ? "TXN-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase() : null)
                    .message(success ? "Payment processed successfully" : "Payment failed")
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return PaymentGatewayResponse.builder()
                    .success(false)
                    .message("Payment processing interrupted")
                    .build();
        }
    }

    public RefundResponse processRefund(RefundRequest request) {
        // Simulated refund processing
        try {
            Thread.sleep(100);
            
            return RefundResponse.builder()
                    .success(true)
                    .transactionId("REF-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                    .message("Refund processed successfully")
                    .build();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return RefundResponse.builder()
                    .success(false)
                    .message("Refund processing interrupted")
                    .build();
        }
    }

    public PaymentStatus getPaymentStatus(String transactionId) {
        // Simulated status check
        return PaymentStatus.SUCCESS;
    }

    public boolean validateWebhookSignature(String payload, String signature) {
        // Simulated webhook validation
        return true;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentRequest {
        private BigDecimal amount;
        private String paymentMethod;
        private Map<String, String> paymentDetails;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PaymentGatewayResponse {
        private boolean success;
        private String transactionId;
        private String message;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefundRequest {
        private BigDecimal amount;
        private String refundMethod;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RefundResponse {
        private boolean success;
        private String transactionId;
        private String message;
    }

    public enum PaymentStatus {
        PENDING, SUCCESS, FAILED
    }
}
