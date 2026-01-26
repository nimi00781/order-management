package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.integration.PaymentGatewayClient;
import com.articurated.ordermanagement.model.dto.response.RefundResponse;
import com.articurated.ordermanagement.model.entity.Refund;
import com.articurated.ordermanagement.model.entity.Return;
import com.articurated.ordermanagement.model.enums.RefundStatus;
import com.articurated.ordermanagement.model.enums.ReturnStatus;
import com.articurated.ordermanagement.model.exception.PaymentProcessingException;
import com.articurated.ordermanagement.repository.RefundRepository;
import com.articurated.ordermanagement.repository.ReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundService {
    private final RefundRepository refundRepository;
    private final ReturnRepository returnRepository;
    private final PaymentGatewayClient paymentGatewayClient;
    private final StateMachineService stateMachineService;
    private final EmailService emailService;

    @Transactional
    public RefundResponse createRefund(Long returnId) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new com.articurated.ordermanagement.model.exception.ReturnNotFoundException(returnId));

        if (returnEntity.getStatus() != ReturnStatus.RECEIVED) {
            throw new com.articurated.ordermanagement.model.exception.BusinessRuleViolationException(
                    "REFUND", "CREATION",
                    "Return must be in RECEIVED state to create refund");
        }

        if (returnEntity.getRefund() != null) {
            throw new com.articurated.ordermanagement.model.exception.BusinessRuleViolationException(
                    "REFUND", "DUPLICATE",
                    "Refund already exists for this return");
        }

        Refund refund = Refund.builder()
                .returnEntity(returnEntity)
                .amount(returnEntity.getOrder().getTotalAmount()) // Simplified: refund full order amount
                .refundMethod("ORIGINAL_PAYMENT_METHOD")
                .status(RefundStatus.PENDING)
                .build();

        Refund savedRefund = refundRepository.save(refund);
        returnEntity.setRefund(savedRefund);
        returnRepository.save(returnEntity);

        return mapToRefundResponse(savedRefund);
    }

    public RefundResponse getRefundById(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new com.articurated.ordermanagement.model.exception.RefundNotFoundException(refundId));
        return mapToRefundResponse(refund);
    }

    public RefundResponse getRefundByReturnId(Long returnId) {
        Refund refund = refundRepository.findByReturnId(returnId)
                .orElseThrow(() -> new com.articurated.ordermanagement.model.exception.RefundNotFoundException(
                        "Refund not found for return id: " + returnId));
        return mapToRefundResponse(refund);
    }

    @Transactional
    public RefundResponse processRefund(Long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new com.articurated.ordermanagement.model.exception.RefundNotFoundException(refundId));

        if (refund.getStatus() != RefundStatus.PENDING) {
            throw new com.articurated.ordermanagement.model.exception.BusinessRuleViolationException(
                    "REFUND", "PROCESSING",
                    "Refund is not in PENDING state");
        }

        refund.setStatus(RefundStatus.PROCESSING);
        refund = refundRepository.save(refund);

        try {
            // Call payment gateway to process refund
            PaymentGatewayClient.RefundRequest refundRequest = 
                    new PaymentGatewayClient.RefundRequest(
                            refund.getAmount(),
                            refund.getRefundMethod()
                    );

            PaymentGatewayClient.RefundResponse gatewayResponse = 
                    paymentGatewayClient.processRefund(refundRequest);

            refund.setTransactionId(gatewayResponse.getTransactionId());
            refund.setStatus(gatewayResponse.isSuccess() ? RefundStatus.COMPLETED : RefundStatus.FAILED);
            refund.setProcessedAt(LocalDateTime.now());

            refund = refundRepository.save(refund);

            if (gatewayResponse.isSuccess()) {
                // Transition return to COMPLETED
                Return returnEntity = refund.getReturnEntity();
                stateMachineService.transitionReturnState(returnEntity, ReturnStatus.COMPLETED, 
                        null, "Refund processed successfully");
                returnRepository.save(returnEntity);
                emailService.sendRefundConfirmation(refund);
            }

            return mapToRefundResponse(refund);

        } catch (Exception e) {
            refund.setStatus(RefundStatus.FAILED);
            refundRepository.save(refund);
            throw new PaymentProcessingException(refund.getId(), e.getMessage());
        }
    }

    public List<RefundResponse> getPendingRefunds() {
        return refundRepository.findByStatus(RefundStatus.PENDING).stream()
                .map(this::mapToRefundResponse)
                .collect(Collectors.toList());
    }

    private RefundResponse mapToRefundResponse(Refund refund) {
        return RefundResponse.builder()
                .id(refund.getId())
                .returnId(refund.getReturnEntity().getId())
                .amount(refund.getAmount())
                .status(refund.getStatus())
                .refundMethod(refund.getRefundMethod())
                .transactionId(refund.getTransactionId())
                .processedAt(refund.getProcessedAt())
                .build();
    }
}
