package com.powerfitness.controller;

import com.powerfitness.entity.Role;
import com.powerfitness.entity.Supplement;
import com.powerfitness.entity.User;
import com.powerfitness.repository.SupplementRepository;
import com.powerfitness.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/supplements")
@CrossOrigin(origins = "*")
public class SupplementController {

    @Autowired
    private SupplementRepository supplementRepository;

    @Autowired
    private AuthService authService;

    // --- View supplements (Both User and Admin) ---
    @GetMapping
    public ResponseEntity<?> getSupplements(
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String q) {

        List<Supplement> list = supplementRepository.findAllByOrderByCreatedAtDesc();

        // Refresh prices based on active dates
        for (Supplement s : list) {
            s.recalculatePrices();
        }

        if (category != null && !category.trim().isEmpty() && !"All".equalsIgnoreCase(category)) {
            list = list.stream().filter(s -> category.equalsIgnoreCase(s.getCategory())).collect(Collectors.toList());
        }

        if (q != null && !q.trim().isEmpty()) {
            String query = q.trim().toLowerCase();
            list = list.stream().filter(s ->
                s.getName().toLowerCase().contains(query) ||
                (s.getDescription() != null && s.getDescription().toLowerCase().contains(query))
            ).collect(Collectors.toList());
        }

        return ResponseEntity.ok(list);
    }

    // --- Admin: Add Supplement ---
    @PostMapping
    public ResponseEntity<?> createSupplement(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Supplement supplement) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        if (supplement.getName() == null || supplement.getName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Supplement name is required"));
        }

        // Validate and apply offer
        try {
            validateAndApplyOffer(supplement);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        if (supplement.getCategory() == null) {
            supplement.setCategory("Other");
        }
        if (supplement.getStockStatus() == null) {
            supplement.setStockStatus("AVAILABLE");
        }

        Supplement saved = supplementRepository.save(supplement);
        return ResponseEntity.ok(saved);
    }

    // --- Admin: Edit Supplement ---
    @PutMapping("/{id}")
    public ResponseEntity<?> updateSupplement(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Supplement updated) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<Supplement> sOpt = supplementRepository.findById(id);
        if (sOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Supplement s = sOpt.get();
        if (updated.getName() != null) s.setName(updated.getName().trim());
        if (updated.getCategory() != null) s.setCategory(updated.getCategory().trim());

        if (updated.getOriginalPrice() > 0) {
            s.setOriginalPrice(updated.getOriginalPrice());
        } else if (updated.getPrice() > 0 && s.getOriginalPrice() <= 0) {
            s.setOriginalPrice(updated.getPrice());
        }

        s.setHasOffer(updated.isHasOffer());
        s.setOfferType(updated.getOfferType());
        s.setDiscountValue(updated.getDiscountValue());
        s.setOfferStartDate(updated.getOfferStartDate());
        s.setOfferEndDate(updated.getOfferEndDate());

        try {
            validateAndApplyOffer(s);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }

        if (updated.getDescription() != null) s.setDescription(updated.getDescription());
        if (updated.getImageUrl() != null && !updated.getImageUrl().trim().isEmpty()) {
            s.setImageUrl(updated.getImageUrl());
        }
        if (updated.getStockQuantity() != null) {
            s.setStockQuantity(updated.getStockQuantity());
        }
        if (updated.getStockStatus() != null) s.setStockStatus(updated.getStockStatus());
        s.setUpdatedAt(LocalDateTime.now());

        Supplement saved = supplementRepository.save(s);
        return ResponseEntity.ok(saved);
    }

    // --- Admin: Toggle Stock ---
    @PatchMapping("/{id}/toggle-stock")
    public ResponseEntity<?> toggleStock(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<Supplement> sOpt = supplementRepository.findById(id);
        if (sOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Supplement s = sOpt.get();
        if ("AVAILABLE".equals(s.getStockStatus())) {
            s.setStockStatus("OUT_OF_STOCK");
        } else {
            s.setStockStatus("AVAILABLE");
        }
        s.setUpdatedAt(LocalDateTime.now());
        Supplement saved = supplementRepository.save(s);
        return ResponseEntity.ok(saved);
    }

    // --- Admin: Toggle Offer ---
    @PatchMapping("/{id}/toggle-offer")
    public ResponseEntity<?> toggleOffer(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<Supplement> sOpt = supplementRepository.findById(id);
        if (sOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Supplement s = sOpt.get();
        s.setHasOffer(!s.isHasOffer());
        s.recalculatePrices();
        s.setUpdatedAt(LocalDateTime.now());
        Supplement saved = supplementRepository.save(s);
        return ResponseEntity.ok(saved);
    }

    // --- Admin: Delete Supplement ---
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSupplement(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        if (!supplementRepository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }

        supplementRepository.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Supplement deleted successfully"));
    }

    // --- Private Helper: Validate and Calculate Offer ---
    private void validateAndApplyOffer(Supplement s) {
        if (s.getOriginalPrice() <= 0) {
            if (s.getPrice() > 0) {
                s.setOriginalPrice(s.getPrice());
            } else {
                throw new IllegalArgumentException("Valid price is required.");
            }
        }

        if (s.isHasOffer()) {
            if (s.getOfferType() == null || s.getOfferType().trim().isEmpty()) {
                s.setOfferType("PERCENTAGE");
            }

            double discount = s.getDiscountValue();
            if (discount <= 0) {
                throw new IllegalArgumentException("Discount value must be greater than zero.");
            }

            double calculatedOfferPrice;
            if ("PERCENTAGE".equalsIgnoreCase(s.getOfferType())) {
                if (discount >= 100) {
                    throw new IllegalArgumentException("Discount percentage must be less than 100%.");
                }
                calculatedOfferPrice = s.getOriginalPrice() * (1.0 - (discount / 100.0));
            } else if ("FIXED".equalsIgnoreCase(s.getOfferType())) {
                if (discount >= s.getOriginalPrice()) {
                    throw new IllegalArgumentException("Discount cannot be greater than the original price.");
                }
                calculatedOfferPrice = s.getOriginalPrice() - discount;
            } else {
                throw new IllegalArgumentException("Invalid offer type. Choose Percentage or Fixed Amount.");
            }

            if (calculatedOfferPrice <= 0) {
                throw new IllegalArgumentException("Discount cannot be greater than the original price.");
            }

            s.setOfferPrice(Math.round(calculatedOfferPrice * 100.0) / 100.0);
            s.setPrice(s.getEffectivePrice());
        } else {
            s.setHasOffer(false);
            s.setDiscountValue(0.0);
            s.setOfferPrice(0.0);
            s.setPrice(s.getOriginalPrice());
        }
    }
}
