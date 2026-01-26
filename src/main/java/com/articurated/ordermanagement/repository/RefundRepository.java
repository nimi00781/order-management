package com.articurated.ordermanagement.repository;

import com.articurated.ordermanagement.model.entity.Refund;
import com.articurated.ordermanagement.model.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RefundRepository extends JpaRepository<Refund, Long> {
    Optional<Refund> findByReturnId(Long returnId);
    List<Refund> findByStatus(RefundStatus status);
}
