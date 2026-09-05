package com.powerfitness.repository;

import com.powerfitness.entity.SupplementOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplementOrderRepository extends JpaRepository<SupplementOrder, Long> {
    List<SupplementOrder> findAllByOrderByOrderDateDesc();
    List<SupplementOrder> findByUserIdOrderByOrderDateDesc(Long userId);
    List<SupplementOrder> findByOrderStatusOrderByOrderDateDesc(String orderStatus);
    Optional<SupplementOrder> findByOrderNumber(String orderNumber);
}
