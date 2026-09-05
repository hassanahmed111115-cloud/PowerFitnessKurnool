package com.powerfitness.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "meal_foods")
public class MealFood {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String mealCategory; // Breakfast, Lunch, Snacks, Dinner

    private String servingSize; // e.g. "150g" or "3 pcs"

    private double calories;
    private double protein;
    private double carbs;
    private double fat;
    private double fiber;

    private String vitamins;
    private String minerals;

    @Column(length = 1000)
    private String imageUrl;

    private boolean isAndhraSpecial;

    public MealFood() {}

    public MealFood(String name, String mealCategory, String servingSize, double calories, double protein,
                    double carbs, double fat, double fiber, String vitamins, String minerals,
                    String imageUrl, boolean isAndhraSpecial) {
        this.name = name;
        this.mealCategory = mealCategory;
        this.servingSize = servingSize;
        this.calories = calories;
        this.protein = protein;
        this.carbs = carbs;
        this.fat = fat;
        this.fiber = fiber;
        this.vitamins = vitamins;
        this.minerals = minerals;
        this.imageUrl = imageUrl;
        this.isAndhraSpecial = isAndhraSpecial;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getMealCategory() { return mealCategory; }
    public void setMealCategory(String mealCategory) { this.mealCategory = mealCategory; }

    public String getServingSize() { return servingSize; }
    public void setServingSize(String servingSize) { this.servingSize = servingSize; }

    public double getCalories() { return calories; }
    public void setCalories(double calories) { this.calories = calories; }

    public double getProtein() { return protein; }
    public void setProtein(double protein) { this.protein = protein; }

    public double getCarbs() { return carbs; }
    public void setCarbs(double carbs) { this.carbs = carbs; }

    public double getFat() { return fat; }
    public void setFat(double fat) { this.fat = fat; }

    public double getFiber() { return fiber; }
    public void setFiber(double fiber) { this.fiber = fiber; }

    public String getVitamins() { return vitamins; }
    public void setVitamins(String vitamins) { this.vitamins = vitamins; }

    public String getMinerals() { return minerals; }
    public void setMinerals(String minerals) { this.minerals = minerals; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public boolean isAndhraSpecial() { return isAndhraSpecial; }
    public void setAndhraSpecial(boolean andhraSpecial) { isAndhraSpecial = andhraSpecial; }
}
