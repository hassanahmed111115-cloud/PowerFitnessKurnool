package com.powerfitness.controller;

import com.powerfitness.dto.GoalRequest;
import com.powerfitness.dto.MealLogRequest;
import com.powerfitness.entity.*;
import com.powerfitness.repository.MealFoodRepository;
import com.powerfitness.repository.UserGoalRepository;
import com.powerfitness.repository.UserMealSelectionRepository;
import com.powerfitness.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/nutrition")
@CrossOrigin(origins = "*")
public class NutritionController {

    @Autowired
    private MealFoodRepository mealFoodRepository;

    @Autowired
    private UserMealSelectionRepository userMealSelectionRepository;

    @Autowired
    private UserGoalRepository userGoalRepository;

    @Autowired
    private AuthService authService;

    // --- Get Indian / Andhra Meal Database ---
    @GetMapping("/foods")
    public ResponseEntity<?> getFoods(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean andhraSpecial,
            @RequestParam(required = false) String q) {

        List<MealFood> foods = mealFoodRepository.findAll();

        if (category != null && !category.trim().isEmpty() && !"All".equalsIgnoreCase(category)) {
            foods = foods.stream().filter(f -> category.equalsIgnoreCase(f.getMealCategory())).collect(Collectors.toList());
        }

        if (andhraSpecial != null && andhraSpecial) {
            foods = foods.stream().filter(MealFood::isAndhraSpecial).collect(Collectors.toList());
        }

        if (q != null && !q.trim().isEmpty()) {
            String query = q.trim().toLowerCase();
            foods = foods.stream().filter(f ->
                f.getName().toLowerCase().contains(query) ||
                (f.getVitamins() != null && f.getVitamins().toLowerCase().contains(query)) ||
                (f.getMinerals() != null && f.getMinerals().toLowerCase().contains(query))
            ).collect(Collectors.toList());
        }

        return ResponseEntity.ok(foods);
    }

    // --- Get User Goal ---
    @GetMapping("/goal")
    public ResponseEntity<?> getUserGoal(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<UserGoal> goalOpt = userGoalRepository.findByUserId(user.getId());
        if (goalOpt.isPresent()) {
            return ResponseEntity.ok(goalOpt.get());
        }

        // Default to Lean Bulking
        UserGoal defaultGoal = new UserGoal(user.getId(), "STRENGTH_TRAINING", "Lean Bulking", 2500, 160, 280, 65);
        userGoalRepository.save(defaultGoal);
        return ResponseEntity.ok(defaultGoal);
    }

    // --- Set/Update User Goal ---
    @PostMapping("/goal")
    public ResponseEntity<?> setUserGoal(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody GoalRequest req) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String cat = req.getCategory();
        String type = req.getGoalType();
        if (type == null || type.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Goal type is required"));
        }

        double cals = 2500, protein = 160, carbs = 280, fat = 65;

        // Calculate based on exact user specification
        if ("Weight Loss".equalsIgnoreCase(type)) {
            cat = "CARDIO_WEIGHT_LOSS";
            cals = 1750; protein = 120; carbs = 180; fat = 45;
        } else if ("Weight Loss + Strength Training".equalsIgnoreCase(type)) {
            cat = "CARDIO_WEIGHT_LOSS";
            cals = 2050; protein = 145; carbs = 210; fat = 50;
        } else if ("Bulking".equalsIgnoreCase(type)) {
            cat = "STRENGTH_TRAINING";
            cals = 2800; protein = 170; carbs = 340; fat = 75;
        } else if ("Lean Bulking".equalsIgnoreCase(type)) {
            cat = "STRENGTH_TRAINING";
            cals = 2500; protein = 160; carbs = 280; fat = 65;
        } else if ("Dirty Bulking".equalsIgnoreCase(type)) {
            cat = "STRENGTH_TRAINING";
            cals = 3200; protein = 180; carbs = 400; fat = 90;
        } else if ("Fat Cutting".equalsIgnoreCase(type)) {
            cat = "STRENGTH_TRAINING";
            cals = 1950; protein = 165; carbs = 160; fat = 48;
        }

        Optional<UserGoal> existing = userGoalRepository.findByUserId(user.getId());
        UserGoal goal = existing.orElse(new UserGoal());
        goal.setUserId(user.getId());
        goal.setCategory(cat != null ? cat : "STRENGTH_TRAINING");
        goal.setGoalType(type);
        goal.setTargetCalories(cals);
        goal.setTargetProtein(protein);
        goal.setTargetCarbs(carbs);
        goal.setTargetFat(fat);

        UserGoal saved = userGoalRepository.save(goal);
        return ResponseEntity.ok(saved);
    }

    // --- Daily Meal Tracker: Get logged meals for date ---
    @GetMapping("/tracker")
    public ResponseEntity<?> getTracker(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String date) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        LocalDate logDate = date != null ? LocalDate.parse(date) : LocalDate.now();
        List<UserMealSelection> selections = userMealSelectionRepository.findByUserIdAndLogDateOrderByCreatedAtAsc(user.getId(), logDate);

        // Group by meal category
        Map<String, List<UserMealSelection>> byCategory = new LinkedHashMap<>();
        byCategory.put("Breakfast", new ArrayList<>());
        byCategory.put("Lunch", new ArrayList<>());
        byCategory.put("Snacks", new ArrayList<>());
        byCategory.put("Dinner", new ArrayList<>());

        double totalCals = 0, totalProtein = 0, totalCarbs = 0, totalFat = 0, totalFiber = 0;
        Set<String> vitamins = new LinkedHashSet<>();
        Set<String> minerals = new LinkedHashSet<>();

        for (UserMealSelection s : selections) {
            String cat = s.getMealCategory() != null ? s.getMealCategory() : "Lunch";
            byCategory.computeIfAbsent(cat, k -> new ArrayList<>()).add(s);

            totalCals += s.getCalories();
            totalProtein += s.getProtein();
            totalCarbs += s.getCarbs();
            totalFat += s.getFat();

            if (s.getMealFood() != null) {
                totalFiber += (s.getMealFood().getFiber() * s.getQuantity());
                if (s.getMealFood().getVitamins() != null) vitamins.add(s.getMealFood().getVitamins());
                if (s.getMealFood().getMinerals() != null) minerals.add(s.getMealFood().getMinerals());
            }
        }

        // Get user target
        UserGoal goal = userGoalRepository.findByUserId(user.getId())
                .orElse(new UserGoal(user.getId(), "STRENGTH_TRAINING", "Lean Bulking", 2500, 160, 280, 65));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("date", logDate);
        summary.put("sections", byCategory);
        summary.put("totalCalories", Math.round(totalCals));
        summary.put("totalProtein", Math.round(totalProtein * 10.0) / 10.0);
        summary.put("totalCarbs", Math.round(totalCarbs * 10.0) / 10.0);
        summary.put("totalFat", Math.round(totalFat * 10.0) / 10.0);
        summary.put("totalFiber", Math.round(totalFiber * 10.0) / 10.0);
        summary.put("vitamins", String.join(", ", vitamins));
        summary.put("minerals", String.join(", ", minerals));

        // Targets
        summary.put("targetCalories", goal.getTargetCalories());
        summary.put("targetProtein", goal.getTargetProtein());
        summary.put("targetCarbs", goal.getTargetCarbs());
        summary.put("targetFat", goal.getTargetFat());
        summary.put("goalType", goal.getGoalType());

        // Remaining
        summary.put("remainingCalories", Math.max(0, Math.round(goal.getTargetCalories() - totalCals)));
        summary.put("remainingProtein", Math.max(0.0, Math.round((goal.getTargetProtein() - totalProtein) * 10.0) / 10.0));
        summary.put("remainingCarbs", Math.max(0.0, Math.round((goal.getTargetCarbs() - totalCarbs) * 10.0) / 10.0));
        summary.put("remainingFat", Math.max(0.0, Math.round((goal.getTargetFat() - totalFat) * 10.0) / 10.0));

        return ResponseEntity.ok(summary);
    }

    // --- Daily Meal Tracker: Add item ---
    @PostMapping("/tracker")
    public ResponseEntity<?> addMealToTracker(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody MealLogRequest req) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        if (req.getFoodId() == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Food ID is required"));
        }

        Optional<MealFood> fOpt = mealFoodRepository.findById(req.getFoodId());
        if (fOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        MealFood food = fOpt.get();
        int qty = Math.max(1, req.getQuantity());
        LocalDate date = req.getLogDate() != null ? req.getLogDate() : LocalDate.now();
        String cat = req.getMealCategory() != null ? req.getMealCategory() : food.getMealCategory();

        UserMealSelection selection = new UserMealSelection();
        selection.setUserId(user.getId());
        selection.setLogDate(date);
        selection.setMealCategory(cat);
        selection.setMealFood(food);
        selection.setQuantity(qty);
        selection.setCalories(food.getCalories() * qty);
        selection.setProtein(food.getProtein() * qty);
        selection.setCarbs(food.getCarbs() * qty);
        selection.setFat(food.getFat() * qty);

        UserMealSelection saved = userMealSelectionRepository.save(selection);
        return ResponseEntity.ok(saved);
    }

    // --- Daily Meal Tracker: Remove item ---
    @DeleteMapping("/tracker/{id}")
    public ResponseEntity<?> removeMealFromTracker(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<UserMealSelection> sOpt = userMealSelectionRepository.findById(id);
        if (sOpt.isPresent()) {
            if (!sOpt.get().getUserId().equals(user.getId())) {
                return ResponseEntity.status(403).body(Map.of("error", "Forbidden"));
            }
            userMealSelectionRepository.delete(sOpt.get());
            return ResponseEntity.ok(Map.of("message", "Item removed"));
        }

        return ResponseEntity.notFound().build();
    }
}
