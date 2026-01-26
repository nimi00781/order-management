package com.articurated.ordermanagement.controller;

import com.articurated.ordermanagement.model.dto.request.CreateOrderRequest;
import com.articurated.ordermanagement.model.dto.request.UpdateOrderStatusRequest;
import com.articurated.ordermanagement.model.dto.response.OrderResponse;
import com.articurated.ordermanagement.model.dto.response.StateHistoryResponse;
import com.articurated.ordermanagement.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {
    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = orderService.createOrder(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{orderId}")
    public ResponseEntity<OrderResponse> getOrderById(@PathVariable Long orderId) {
        OrderResponse response = orderService.getOrderById(orderId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order-number/{orderNumber}")
    public ResponseEntity<OrderResponse> getOrderByOrderNumber(@PathVariable String orderNumber) {
        OrderResponse response = orderService.getOrderByOrderNumber(orderNumber);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/customer/{customerId}")
    public ResponseEntity<List<OrderResponse>> getOrdersByCustomerId(@PathVariable Long customerId) {
        List<OrderResponse> responses = orderService.getOrdersByCustomerId(customerId);
        return ResponseEntity.ok(responses);
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<OrderResponse> updateOrderStatus(
            @PathVariable Long orderId,
            @Valid @RequestBody UpdateOrderStatusRequest request,
            @RequestHeader(value = "userId", required = false) Long userId) {
        OrderResponse response = orderService.updateOrderStatus(orderId, request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<OrderResponse> cancelOrder(
            @PathVariable Long orderId,
            @RequestHeader(value = "userId", required = false) Long userId) {
        OrderResponse response = orderService.cancelOrder(orderId, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{orderId}/history")
    public ResponseEntity<List<StateHistoryResponse>> getOrderStateHistory(@PathVariable Long orderId) {
        List<StateHistoryResponse> history = orderService.getOrderStateHistory(orderId);
        return ResponseEntity.ok(history);
    }
}
