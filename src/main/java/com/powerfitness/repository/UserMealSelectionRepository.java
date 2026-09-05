package com.powerfitness.repository;

import com.powerfitness.entity.UserMealSelection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface UserMealSelectionRepository extends JpaRepository<UserMealSelection, Long> {
    List<UserMealSelection> findByUserIdAndLogDateOrderByCreatedAtAsc(Long userId, LocalDate logDate);
    List<UserMealSelection> findByUserIdAndLogDateAndMealCategory(Long userId, LocalDate logDate, String mealCategory);
}
