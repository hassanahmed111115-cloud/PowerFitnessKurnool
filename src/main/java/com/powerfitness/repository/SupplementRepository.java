package com.powerfitness.repository;

import com.powerfitness.entity.Supplement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SupplementRepository extends JpaRepository<Supplement, Long> {
    List<Supplement> findByCategory(String category);
    List<Supplement> findAllByOrderByCreatedAtDesc();
}
