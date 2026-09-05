package com.powerfitness.dto;

public class GoalRequest {
    private String category; // CARDIO_WEIGHT_LOSS, STRENGTH_TRAINING
    private String goalType; // Weight Loss, Weight Loss + Strength Training, Bulking, Lean Bulking, Dirty Bulking, Fat Cutting

    public GoalRequest() {}

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getGoalType() { return goalType; }
    public void setGoalType(String goalType) { this.goalType = goalType; }
}
