package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.model.dto.request.CreateOrderRequest;
import com.articurated.ordermanagement.model.dto.request.OrderItemRequest;
import com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest;
import com.articurated.ordermanagement.model.dto.response.OrderItemResponse;
import com.articurated.ordermanagement.model.dto.response.OrderResponse;
import com.articurated.ordermanagement.model.dto.response.StateHistoryResponse;
import com.articurated.ordermanagement.model.entity.Order;
import com.articurated.ordermanagement.model.entity.OrderItem;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.exception.OrderNotFoundException;
import com.articurated.ordermanagement.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {
    private final OrderRepository orderRepository;
    private final StateMachineService stateMachineService;
    private final StateHistoryService stateHistoryService;
    private final EmailService emailService;

    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request) {
        Order order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customerId(request.getCustomerId())
                .status(OrderStatus.PENDING_PAYMENT)
                .shippingAddress(request.getShippingAddress())
                .build();

        List<OrderItem> orderItems = request.getOrderItems().stream()
                .map(itemRequest -> OrderItem.builder()
                        .order(order)
                        .productId(itemRequest.getProductId())
                        .productName(itemRequest.getProductName())
                        .quantity(itemRequest.getQuantity())
                        .price(itemRequest.getPrice())
                        .build())
                .collect(Collectors.toList());

        order.setOrderItems(orderItems);
        order.setTotalAmount(order.calculateTotal());

        Order savedOrder = orderRepository.save(order);

        // Log initial state
        stateHistoryService.logStateTransition(
                com.articurated.ordermanagement.model.enums.EntityType.ORDER,
                savedOrder.getId(),
                null,
                OrderStatus.PENDING_PAYMENT.name(),
                null,
                "Order created"
        );

        return mapToOrderResponse(savedOrder);
    }

    public OrderResponse getOrderById(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        return mapToOrderResponse(order);
    }

    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new OrderNotFoundException(null));
        return mapToOrderResponse(order);
    }

    public List<OrderResponse> getOrdersByCustomerId(Long customerId) {
        return orderRepository.findByCustomerId(customerId).stream()
                .map(this::mapToOrderResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(Long orderId, UpdateOrderStatusRequest request, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        stateMachineService.transitionOrderState(order, request.getNewStatus(), userId, request.getReason());
        Order updatedOrder = orderRepository.save(order);

        emailService.sendOrderStatusUpdate(updatedOrder, request.getNewStatus());

        return mapToOrderResponse(updatedOrder);
    }

    @Transactional
    public OrderResponse cancelOrder(Long orderId, Long userId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));

        if (!order.canBeCancelled()) {
            throw new com.articurated.ordermanagement.model.exception.BusinessRuleViolationException(
                    "ORDER", "CANCELLATION",
                    "Order cannot be cancelled in current state: " + order.getStatus());
        }

        stateMachineService.transitionOrderState(order, OrderStatus.CANCELLED, userId, "Order cancelled by user");
        Order cancelledOrder = orderRepository.save(order);

        return mapToOrderResponse(cancelledOrder);
    }

    public List<StateHistoryResponse> getOrderStateHistory(Long orderId) {
        return stateHistoryService.getStateHistory(
                com.articurated.ordermanagement.model.enums.EntityType.ORDER, orderId).stream()
                .map(this::mapToStateHistoryResponse)
                .collect(Collectors.toList());
    }

    private String generateOrderNumber() {
        return "ORD-" + LocalDateTime.now().getYear() + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    private OrderResponse mapToOrderResponse(Order order) {
        List<OrderItemResponse> itemResponses = order.getOrderItems().stream()
                .map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .productName(item.getProductName())
                        .quantity(item.getQuantity())
                        .price(item.getPrice())
                        .subtotal(item.getSubtotal())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .customerId(order.getCustomerId())
                .status(order.getStatus())
                .totalAmount(order.getTotalAmount())
                .shippingAddress(order.getShippingAddress())
                .orderItems(itemResponses)
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .build();
    }

    private StateHistoryResponse mapToStateHistoryResponse(com.articurated.ordermanagement.model.entity.StateTransition transition) {
        return StateHistoryResponse.builder()
                .id(transition.getId())
                .entityType(transition.getEntityType())
                .entityId(transition.getEntityId())
                .fromState(transition.getFromState())
                .toState(transition.getToState())
                .triggeredBy(transition.getTriggeredBy())
                .transitionReason(transition.getTransitionReason())
                .timestamp(transition.getTimestamp())
                .build();
    }
}
