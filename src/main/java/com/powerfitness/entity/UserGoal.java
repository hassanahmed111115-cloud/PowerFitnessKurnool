package com.powerfitness.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "user_goals")
public class UserGoal {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String category; // CARDIO_WEIGHT_LOSS, STRENGTH_TRAINING

    @Column(nullable = false)
    private String goalType; // Weight Loss, Weight Loss + Strength Training, Bulking, Lean Bulking, Dirty Bulking, Fat Cutting

    private double targetCalories;
    private double targetProtein;
    private double targetCarbs;
    private double targetFat;

    public UserGoal() {}

    public UserGoal(Long userId, String category, String goalType, double targetCalories, double targetProtein, double targetCarbs, double targetFat) {
        this.userId = userId;
        this.category = category;
        this.goalType = goalType;
        this.targetCalories = targetCalories;
        this.targetProtein = targetProtein;
        this.targetCarbs = targetCarbs;
        this.targetFat = targetFat;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getGoalType() { return goalType; }
    public void setGoalType(String goalType) { this.goalType = goalType; }

    public double getTargetCalories() { return targetCalories; }
    public void setTargetCalories(double targetCalories) { this.targetCalories = targetCalories; }

    public double getTargetProtein() { return targetProtein; }
    public void setTargetProtein(double targetProtein) { this.targetProtein = targetProtein; }

    public double getTargetCarbs() { return targetCarbs; }
    public void setTargetCarbs(double targetCarbs) { this.targetCarbs = targetCarbs; }

    public double getTargetFat() { return targetFat; }
    public void setTargetFat(double targetFat) { this.targetFat = targetFat; }
}
