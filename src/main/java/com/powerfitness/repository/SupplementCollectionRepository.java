package com.powerfitness.repository;

import com.powerfitness.entity.SupplementCollection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SupplementCollectionRepository extends JpaRepository<SupplementCollection, Long> {
    List<SupplementCollection> findAllByOrderByCollectionDateDesc();
    List<SupplementCollection> findByOrderUserIdOrderByCollectionDateDesc(Long userId);
    Optional<SupplementCollection> findByCollectionNumber(String collectionNumber);
    Optional<SupplementCollection> findByOrderId(Long orderId);
}
