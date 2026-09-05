package com.powerfitness.repository;

import com.powerfitness.entity.MealFood;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MealFoodRepository extends JpaRepository<MealFood, Long> {
    List<MealFood> findByMealCategory(String mealCategory);
    List<MealFood> findByIsAndhraSpecialTrue();
    List<MealFood> findByNameContainingIgnoreCase(String query);
}
