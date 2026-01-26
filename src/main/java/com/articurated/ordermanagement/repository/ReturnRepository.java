package com.articurated.ordermanagement.repository;

import com.articurated.ordermanagement.model.entity.Return;
import com.articurated.ordermanagement.model.enums.ReturnStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnRepository extends JpaRepository<Return, Long> {
    List<Return> findByOrderId(Long orderId);
    List<Return> findByStatus(ReturnStatus status);
    List<Return> findByOrderIdAndStatus(Long orderId, ReturnStatus status);
}
