package com.articurated.ordermanagement.scheduler;

import com.articurated.ordermanagement.model.entity.Refund;
import com.articurated.ordermanagement.model.enums.RefundStatus;
import com.articurated.ordermanagement.repository.RefundRepository;
import com.articurated.ordermanagement.service.RefundService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class RefundProcessorJob {
    private final RefundRepository refundRepository;
    private final RefundService refundService;

    @Scheduled(fixedRate = 900000) // Every 15 minutes
    public void processPendingRefunds() {
        log.info("Processing pending refunds...");
        
        List<Refund> pendingRefunds = refundRepository.findByStatus(RefundStatus.PENDING);
        
        for (Refund refund : pendingRefunds) {
            try {
                log.info("Processing refund ID: {}", refund.getId());
                refundService.processRefund(refund.getId());
            } catch (Exception e) {
                log.error("Error processing refund ID: {}", refund.getId(), e);
            }
        }
    }
}
