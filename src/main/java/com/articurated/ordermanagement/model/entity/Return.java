package com.articurated.ordermanagement.model.entity;

import com.articurated.ordermanagement.model.enums.ReturnStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "returns")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Return {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "return_reason", nullable = false, length = 500)
    private String returnReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReturnStatus status;

    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    @Column(name = "approved_by")
    private Long approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @Column(name = "shipped_at")
    private LocalDateTime shippedAt;

    @Column(name = "received_at")
    private LocalDateTime receivedAt;

    @Column(name = "comments", length = 1000)
    private String comments;

    @OneToOne(mappedBy = "returnEntity", cascade = CascadeType.ALL)
    private Refund refund;

    @PrePersist
    protected void onCreate() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = ReturnStatus.REQUESTED;
        }
    }

    public boolean canBeApproved() {
        return status == ReturnStatus.REQUESTED;
    }

    public boolean canTransitionTo(ReturnStatus targetStatus) {
        return switch (status) {
            case REQUESTED -> targetStatus == ReturnStatus.APPROVED || targetStatus == ReturnStatus.REJECTED;
            case APPROVED -> targetStatus == ReturnStatus.IN_TRANSIT;
            case IN_TRANSIT -> targetStatus == ReturnStatus.RECEIVED;
            case RECEIVED -> targetStatus == ReturnStatus.COMPLETED;
            default -> false;
        };
    }
}
