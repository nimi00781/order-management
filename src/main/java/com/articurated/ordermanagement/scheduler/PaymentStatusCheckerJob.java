package com.articurated.ordermanagement.scheduler;

import com.articurated.ordermanagement.model.entity.Payment;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.enums.PaymentStatus;
import com.articurated.ordermanagement.repository.PaymentRepository;
import com.articurated.ordermanagement.service.StateMachineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentStatusCheckerJob {
    private final PaymentRepository paymentRepository;
    private final StateMachineService stateMachineService;

    @Scheduled(fixedRate = 600000) // Every 10 minutes
    @Transactional
    public void checkPendingPayments() {
        log.info("Checking pending payments...");
        
        List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);
        
        for (Payment payment : pendingPayments) {
            try {
                // In real implementation, query payment gateway for status
                // For now, simulate status check
                log.info("Checking payment status for payment ID: {}", payment.getId());
                
                // Simulated: Assume payment succeeds after some time
                if (payment.getCreatedAt().isBefore(java.time.LocalDateTime.now().minusMinutes(5))) {
                    payment.setStatus(PaymentStatus.SUCCESS);
                    payment.setProcessedAt(java.time.LocalDateTime.now());
                    paymentRepository.save(payment);
                    
                    // Transition order state if payment successful
                    if (payment.getOrder().getStatus() == OrderStatus.PENDING_PAYMENT) {
                        stateMachineService.transitionOrderState(
                                payment.getOrder(),
                                OrderStatus.PAID,
                                null,
                                "Payment confirmed via status check"
                        );
                    }
                }
            } catch (Exception e) {
                log.error("Error checking payment status for payment ID: {}", payment.getId(), e);
            }
        }
    }
}
