package com.articurated.ordermanagement.controller;

import com.articurated.ordermanagement.model.dto.request.ProcessPaymentRequest;
import com.articurated.ordermanagement.model.dto.response.PaymentResponse;
import com.articurated.ordermanagement.model.enums.PaymentStatus;
import com.articurated.ordermanagement.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {
    private final PaymentService paymentService;

    @PostMapping
    public ResponseEntity<PaymentResponse> processPayment(@Valid @RequestBody ProcessPaymentRequest request) {
        PaymentResponse response = paymentService.processPayment(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{paymentId}")
    public ResponseEntity<PaymentResponse> getPaymentById(@PathVariable Long paymentId) {
        PaymentResponse response = paymentService.getPaymentById(paymentId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<PaymentResponse>> getPaymentsByOrderId(@PathVariable Long orderId) {
        List<PaymentResponse> responses = paymentService.getPaymentsByOrderId(orderId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/webhook")
    public ResponseEntity<Void> handlePaymentWebhook(@RequestBody Object webhookRequest) {
        // Implement webhook handling logic
        return ResponseEntity.ok().build();
    }
}
