package com.articurated.ordermanagement.controller;

import com.articurated.ordermanagement.model.dto.response.RefundResponse;
import com.articurated.ordermanagement.service.RefundService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {
    private final RefundService refundService;

    @PostMapping("/return/{returnId}")
    public ResponseEntity<RefundResponse> createRefund(@PathVariable Long returnId) {
        RefundResponse response = refundService.createRefund(returnId);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{refundId}")
    public ResponseEntity<RefundResponse> getRefundById(@PathVariable Long refundId) {
        RefundResponse response = refundService.getRefundById(refundId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/return/{returnId}")
    public ResponseEntity<RefundResponse> getRefundByReturnId(@PathVariable Long returnId) {
        RefundResponse response = refundService.getRefundByReturnId(returnId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{refundId}/process")
    public ResponseEntity<RefundResponse> processRefund(@PathVariable Long refundId) {
        RefundResponse response = refundService.processRefund(refundId);
        return ResponseEntity.ok(response);
    }
}
