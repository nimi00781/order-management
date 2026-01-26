package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.integration.PaymentGatewayClient;
import com.articurated.ordermanagement.model.dto.request.ProcessPaymentRequest;
import com.articurated.ordermanagement.model.dto.response.PaymentResponse;
import com.articurated.ordermanagement.model.entity.Order;
import com.articurated.ordermanagement.model.entity.Payment;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.enums.PaymentStatus;
import com.articurated.ordermanagement.model.exception.OrderNotFoundException;
import com.articurated.ordermanagement.model.exception.PaymentProcessingException;
import com.articurated.ordermanagement.repository.OrderRepository;
import com.articurated.ordermanagement.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final StateMachineService stateMachineService;
    private final EmailService emailService;

    @Transactional
    public PaymentResponse processPayment(ProcessPaymentRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new com.articurated.ordermanagement.model.exception.BusinessRuleViolationException(
                    "PAYMENT", "PROCESSING",
                    "Order is not in PENDING_PAYMENT state");
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .amount(request.getAmount())
                .status(PaymentStatus.PENDING)
                .build();

        payment = paymentRepository.save(payment);

        try {
            // Call payment gateway
            PaymentGatewayClient.PaymentGatewayResponse gatewayResponse = 
                    paymentGatewayClient.processPayment(
                            new PaymentGatewayClient.PaymentRequest(
                                    request.getAmount(),
                                    request.getPaymentMethod(),
                                    request.getPaymentDetails()
                            )
                    );

            payment.setTransactionId(gatewayResponse.getTransactionId());
            payment.setStatus(gatewayResponse.isSuccess() ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);
            payment.setProcessedAt(LocalDateTime.now());

            payment = paymentRepository.save(payment);

            if (gatewayResponse.isSuccess()) {
                // Transition order state
                stateMachineService.transitionOrderState(order, OrderStatus.PAID, null, 
                        "Payment processed successfully");
                orderRepository.save(order);
                emailService.sendOrderConfirmation(order);
            }

            return mapToPaymentResponse(payment);

        } catch (Exception e) {
            payment.setStatus(PaymentStatus.FAILED);
            paymentRepository.save(payment);
            throw new PaymentProcessingException(payment.getId(), e.getMessage());
        }
    }

    public PaymentResponse getPaymentById(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new com.articurated.ordermanagement.model.exception.PaymentNotFoundException(paymentId));
        return mapToPaymentResponse(payment);
    }

    public List<PaymentResponse> getPaymentsByOrderId(Long orderId) {
        return paymentRepository.findByOrderId(orderId).stream()
                .map(this::mapToPaymentResponse)
                .collect(Collectors.toList());
    }

    public PaymentStatus checkPaymentStatus(Long paymentId) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new com.articurated.ordermanagement.model.exception.PaymentNotFoundException(paymentId));
        return payment.getStatus();
    }

    private PaymentResponse mapToPaymentResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .paymentMethod(payment.getPaymentMethod())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .transactionId(payment.getTransactionId())
                .processedAt(payment.getProcessedAt())
                .build();
    }
}
