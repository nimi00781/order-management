package com.articurated.ordermanagement.controller;

import com.articurated.ordermanagement.model.dto.request.ApproveReturnRequest;
import com.articurated.ordermanagement.model.dto.request.CreateReturnRequest;
import com.articurated.ordermanagement.model.dto.response.ReturnResponse;
import com.articurated.ordermanagement.model.dto.response.StateHistoryResponse;
import com.articurated.ordermanagement.service.ReturnService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/returns")
@RequiredArgsConstructor
public class ReturnController {
    private final ReturnService returnService;

    @PostMapping
    public ResponseEntity<ReturnResponse> createReturnRequest(@Valid @RequestBody CreateReturnRequest request) {
        ReturnResponse response = returnService.createReturnRequest(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{returnId}")
    public ResponseEntity<ReturnResponse> getReturnById(@PathVariable Long returnId) {
        ReturnResponse response = returnService.getReturnById(returnId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/order/{orderId}")
    public ResponseEntity<List<ReturnResponse>> getReturnsByOrderId(@PathVariable Long orderId) {
        List<ReturnResponse> responses = returnService.getReturnsByOrderId(orderId);
        return ResponseEntity.ok(responses);
    }

    @PostMapping("/{returnId}/approve")
    public ResponseEntity<ReturnResponse> approveReturn(
            @PathVariable Long returnId,
            @Valid @RequestBody ApproveReturnRequest request) {
        ReturnResponse response = returnService.approveReturn(returnId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{returnId}/reject")
    public ResponseEntity<ReturnResponse> rejectReturn(
            @PathVariable Long returnId,
            @RequestParam String reason,
            @RequestHeader("userId") Long managerId) {
        ReturnResponse response = returnService.rejectReturn(returnId, reason, managerId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{returnId}/in-transit")
    public ResponseEntity<ReturnResponse> markReturnInTransit(@PathVariable Long returnId) {
        ReturnResponse response = returnService.markReturnInTransit(returnId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{returnId}/received")
    public ResponseEntity<ReturnResponse> markReturnReceived(@PathVariable Long returnId) {
        ReturnResponse response = returnService.markReturnReceived(returnId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{returnId}/history")
    public ResponseEntity<List<StateHistoryResponse>> getReturnStateHistory(@PathVariable Long returnId) {
        List<StateHistoryResponse> history = returnService.getReturnStateHistory(returnId);
        return ResponseEntity.ok(history);
    }
}
