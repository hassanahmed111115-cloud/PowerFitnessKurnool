package com.powerfitness.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_meal_selections")
public class UserMealSelection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private LocalDate logDate;

    @Column(nullable = false)
    private String mealCategory; // Breakfast, Lunch, Snacks, Dinner

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "meal_food_id", nullable = false)
    private MealFood mealFood;

    private int quantity = 1;

    private double calories;
    private double protein;
    private double carbs;
    private double fat;

    private LocalDateTime createdAt = LocalDateTime.now();

    public UserMealSelection() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    public String getMealCategory() { return mealCategory; }
    public void setMealCategory(String mealCategory) { this.mealCategory = mealCategory; }

    public MealFood getMealFood() { return mealFood; }
    public void setMealFood(MealFood mealFood) { this.mealFood = mealFood; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }

    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }

    public double getCarbs() { return carbs; }
    public void setCarbs(double carbs) { this.carbs = carbs; }

    public double getFat() { return fat; }
    public void setFat(double fat) { this.fat = fat; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
