package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.integration.EmailClient;
import com.articurated.ordermanagement.model.entity.Order;
import com.articurated.ordermanagement.model.entity.Refund;
import com.articurated.ordermanagement.model.entity.Return;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.enums.ReturnStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {
    private final EmailClient emailClient;

    @Async
    public void sendOrderConfirmation(Order order) {
        String subject = "Order Confirmation - " + order.getOrderNumber();
        String body = String.format(
                "Dear Customer,\n\nYour order %s has been confirmed.\n\nTotal Amount: %s\n\nThank you for your purchase!",
                order.getOrderNumber(),
                order.getTotalAmount()
        );
        emailClient.sendEmail("customer@example.com", subject, body); // In real app, get from order
    }

    @Async
    public void sendOrderStatusUpdate(Order order, OrderStatus newStatus) {
        String subject = "Order Status Update - " + order.getOrderNumber();
        String body = String.format(
                "Dear Customer,\n\nYour order %s status has been updated to: %s\n\nThank you!",
                order.getOrderNumber(),
                newStatus
        );
        emailClient.sendEmail("customer@example.com", subject, body);
    }

    @Async
    public void sendReturnStatusUpdate(Return returnEntity, ReturnStatus newStatus) {
        String subject = "Return Status Update - Return #" + returnEntity.getId();
        String body = String.format(
                "Dear Customer,\n\nYour return request status has been updated to: %s\n\nThank you!",
                newStatus
        );
        emailClient.sendEmail("customer@example.com", subject, body);
    }

    @Async
    public void sendRefundConfirmation(Refund refund) {
        String subject = "Refund Confirmation - Refund #" + refund.getId();
        String body = String.format(
                "Dear Customer,\n\nYour refund of %s has been processed successfully.\n\nTransaction ID: %s\n\nThank you!",
                refund.getAmount(),
                refund.getTransactionId()
        );
        emailClient.sendEmail("customer@example.com", subject, body);
    }
}
