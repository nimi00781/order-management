package com.articurated.ordermanagement.service;

import com.articurated.ordermanagement.model.dto.request.ApproveReturnRequest;
import com.articurated.ordermanagement.model.dto.request.CreateReturnRequest;
import com.articurated.ordermanagement.model.dto.response.ReturnResponse;
import com.articurated.ordermanagement.model.dto.response.StateHistoryResponse;
import com.articurated.ordermanagement.model.entity.Order;
import com.articurated.ordermanagement.model.entity.Return;
import com.articurated.ordermanagement.model.enums.OrderStatus;
import com.articurated.ordermanagement.model.enums.ReturnStatus;
import com.articurated.ordermanagement.model.exception.OrderNotFoundException;
import com.articurated.ordermanagement.model.exception.ReturnNotAllowedException;
import com.articurated.ordermanagement.repository.OrderRepository;
import com.articurated.ordermanagement.repository.ReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReturnService {
    private final ReturnRepository returnRepository;
    private final OrderRepository orderRepository;
    private final StateMachineService stateMachineService;
    private final StateHistoryService stateHistoryService;
    private final EmailService emailService;

    @Transactional
    public ReturnResponse createReturnRequest(CreateReturnRequest request) {
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new OrderNotFoundException(request.getOrderId()));

        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw new ReturnNotAllowedException(request.getOrderId(), 
                    "Order must be in DELIVERED state to initiate return");
        }

        // Check if there's already an active return
        List<Return> activeReturns = returnRepository.findByOrderIdAndStatus(
                request.getOrderId(), ReturnStatus.REQUESTED);
        if (!activeReturns.isEmpty()) {
            throw new ReturnNotAllowedException(request.getOrderId(), 
                    "Return request already exists for this order");
        }

        Return returnEntity = Return.builder()
                .order(order)
                .returnReason(request.getReturnReason())
                .status(ReturnStatus.REQUESTED)
                .build();

        Return savedReturn = returnRepository.save(returnEntity);

        stateHistoryService.logStateTransition(
                com.articurated.ordermanagement.model.enums.EntityType.RETURN,
                savedReturn.getId(),
                null,
                ReturnStatus.REQUESTED.name(),
                null,
                "Return request created"
        );

        return mapToReturnResponse(savedReturn);
    }

    public ReturnResponse getReturnById(Long returnId) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new com.articurated.ordermanagement.model.exception.ReturnNotFoundException(returnId));
        return mapToReturnResponse(returnEntity);
    }

    public List<ReturnResponse> getReturnsByOrderId(Long orderId) {
        return returnRepository.findByOrderId(orderId).stream()
                .map(this::mapToReturnResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public ReturnResponse approveReturn(Long returnId, ApproveReturnRequest request) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new com.articurated.ordermanagement.model.exception.ReturnNotFoundException(returnId));

        if (!returnEntity.canBeApproved()) {
            throw new com.articurated.ordermanagement.model.exception.BusinessRuleViolationException(
                    "RETURN", "APPROVAL", 
                    "Return cannot be approved in current state: " + returnEntity.getStatus());
        }

        returnEntity.setComments(request.getComments());
        stateMachineService.transitionReturnState(returnEntity, ReturnStatus.APPROVED, 
                request.getManagerId(), "Return approved by manager");
        
        Return savedReturn = returnRepository.save(returnEntity);
        emailService.sendReturnStatusUpdate(savedReturn, ReturnStatus.APPROVED);

        return mapToReturnResponse(savedReturn);
    }

    @Transactional
    public ReturnResponse rejectReturn(Long returnId, String reason, Long managerId) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new com.articurated.ordermanagement.model.exception.ReturnNotFoundException(returnId));

        if (returnEntity.getStatus() != ReturnStatus.REQUESTED) {
            throw new com.articurated.ordermanagement.model.exception.BusinessRuleViolationException(
                    "RETURN", "REJECTION",
                    "Return can only be rejected from REQUESTED state");
        }

        returnEntity.setComments(reason);
        stateMachineService.transitionReturnState(returnEntity, ReturnStatus.REJECTED, 
                managerId, "Return rejected: " + reason);
        
        Return savedReturn = returnRepository.save(returnEntity);
        emailService.sendReturnStatusUpdate(savedReturn, ReturnStatus.REJECTED);

        return mapToReturnResponse(savedReturn);
    }

    @Transactional
    public ReturnResponse markReturnInTransit(Long returnId) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new com.articurated.ordermanagement.model.exception.ReturnNotFoundException(returnId));

        stateMachineService.transitionReturnState(returnEntity, ReturnStatus.IN_TRANSIT, 
                null, "Customer shipped item back");
        
        Return savedReturn = returnRepository.save(returnEntity);
        return mapToReturnResponse(savedReturn);
    }

    @Transactional
    public ReturnResponse markReturnReceived(Long returnId) {
        Return returnEntity = returnRepository.findById(returnId)
                .orElseThrow(() -> new com.articurated.ordermanagement.model.exception.ReturnNotFoundException(returnId));

        stateMachineService.transitionReturnState(returnEntity, ReturnStatus.RECEIVED, 
                null, "Warehouse confirmed receipt");
        
        Return savedReturn = returnRepository.save(returnEntity);
        return mapToReturnResponse(savedReturn);
    }

    public List<StateHistoryResponse> getReturnStateHistory(Long returnId) {
        return stateHistoryService.getStateHistory(
                com.articurated.ordermanagement.model.enums.EntityType.RETURN, returnId).stream()
                .map(transition -> StateHistoryResponse.builder()
                        .id(transition.getId())
                        .entityType(transition.getEntityType())
                        .entityId(transition.getEntityId())
                        .fromState(transition.getFromState())
                        .toState(transition.getToState())
                        .triggeredBy(transition.getTriggeredBy())
                        .transitionReason(transition.getTransitionReason())
                        .timestamp(transition.getTimestamp())
                        .build())
                .collect(Collectors.toList());
    }

    private ReturnResponse mapToReturnResponse(Return returnEntity) {
        return ReturnResponse.builder()
                .id(returnEntity.getId())
                .orderId(returnEntity.getOrder().getId())
                .returnReason(returnEntity.getReturnReason())
                .status(returnEntity.getStatus())
                .requestedAt(returnEntity.getRequestedAt())
                .approvedBy(returnEntity.getApprovedBy())
                .approvedAt(returnEntity.getApprovedAt())
                .shippedAt(returnEntity.getShippedAt())
                .receivedAt(returnEntity.getReceivedAt())
                .comments(returnEntity.getComments())
                .build();
    }
}
