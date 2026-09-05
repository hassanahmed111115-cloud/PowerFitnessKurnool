package com.powerfitness.dto;

import java.time.LocalDate;

public class MealLogRequest {
    private Long foodId;
    private String mealCategory; // Breakfast, Lunch, Snacks, Dinner
    private int quantity = 1;
    private LocalDate logDate;

    public MealLogRequest() {}

    public Long getFoodId() { return foodId; }
    public void setFoodId(Long foodId) { this.foodId = foodId; }

    public String getMealCategory() { return mealCategory; }
    public void setMealCategory(String mealCategory) { this.mealCategory = mealCategory; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }
}
