package com.powerfitness.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplements")
public class Supplement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String category; // Protein, Creatine, Pre-Workout, Mass Gainer, Vitamins, Electrolytes, Other

    private double price; // Effective / active selling price

    private double originalPrice; // Always preserved original price

    private boolean hasOffer = false;

    private String offerType = "PERCENTAGE"; // PERCENTAGE, FIXED

    private double discountValue = 0.0; // e.g. 20 for 20%, or 500 for Rs.500 off

    private double offerPrice = 0.0; // Discounted offer price

    private LocalDate offerStartDate;

    private LocalDate offerEndDate;

    @Column(length = 2000)
    private String description;

    @Column(length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private String stockStatus = "AVAILABLE"; // AVAILABLE, OUT_OF_STOCK

    @Column(columnDefinition = "int default 25")
    private Integer stockQuantity = 25; // Physical units available at gym front desk

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();

    public Supplement() {}

    public Supplement(String name, String category, double price, String description, String imageUrl, String stockStatus) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.originalPrice = price;
        this.hasOffer = false;
        this.description = description;
        this.imageUrl = imageUrl;
        this.stockStatus = stockStatus;
        this.stockQuantity = 25;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Supplement(String name, String category, double price, String description, String imageUrl, String stockStatus, int stockQuantity) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.originalPrice = price;
        this.hasOffer = false;
        this.description = description;
        this.imageUrl = imageUrl;
        this.stockStatus = stockStatus;
        this.stockQuantity = stockQuantity;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Supplement(String name, String category, double originalPrice, boolean hasOffer, String offerType, double discountValue, String description, String imageUrl, String stockStatus) {
        this.name = name;
        this.category = category;
        this.originalPrice = originalPrice;
        this.hasOffer = hasOffer;
        this.offerType = offerType;
        this.discountValue = discountValue;
        this.description = description;
        this.imageUrl = imageUrl;
        this.stockStatus = stockStatus;
        this.stockQuantity = 25;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        recalculatePrices();
    }

    public Supplement(String name, String category, double originalPrice, boolean hasOffer, String offerType, double discountValue, String description, String imageUrl, String stockStatus, int stockQuantity) {
        this.name = name;
        this.category = category;
        this.originalPrice = originalPrice;
        this.hasOffer = hasOffer;
        this.offerType = offerType;
        this.discountValue = discountValue;
        this.description = description;
        this.imageUrl = imageUrl;
        this.stockStatus = stockStatus;
        this.stockQuantity = stockQuantity;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        recalculatePrices();
    }

    public void recalculatePrices() {
        if (originalPrice <= 0 && price > 0) {
            originalPrice = price;
        }

        if (hasOffer && discountValue > 0) {
            double calc;
            if ("FIXED".equalsIgnoreCase(offerType)) {
                calc = originalPrice - discountValue;
            } else {
                calc = originalPrice * (1.0 - (discountValue / 100.0));
            }
            this.offerPrice = Math.max(0, Math.round(calc * 100.0) / 100.0);
            this.price = isOfferActive() ? this.offerPrice : this.originalPrice;
        } else {
            this.hasOffer = false;
            this.offerPrice = 0.0;
            this.price = this.originalPrice > 0 ? this.originalPrice : this.price;
        }
    }

    public boolean isOfferActive() {
        if (!hasOffer) return false;
        LocalDate today = LocalDate.now();
        if (offerStartDate != null && today.isBefore(offerStartDate)) return false;
        if (offerEndDate != null && today.isAfter(offerEndDate)) return false;
        return true;
    }

    public double getEffectivePrice() {
        return isOfferActive() && offerPrice > 0 ? offerPrice : (originalPrice > 0 ? originalPrice : price);
    }

    public void decrementStock(int qty) {
        int current = (this.stockQuantity != null) ? this.stockQuantity : 0;
        this.stockQuantity = Math.max(0, current - qty);
        if (this.stockQuantity <= 0) {
            this.stockStatus = "OUT_OF_STOCK";
        }
        this.updatedAt = LocalDateTime.now();
    }

    public boolean isInStock() {
        return "AVAILABLE".equalsIgnoreCase(stockStatus) && (stockQuantity == null || stockQuantity > 0);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }

    public double getOriginalPrice() { return originalPrice > 0 ? originalPrice : price; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public boolean isHasOffer() { return hasOffer; }
    public void setHasOffer(boolean hasOffer) { this.hasOffer = hasOffer; }

    public String getOfferType() { return offerType; }
    public void setOfferType(String offerType) { this.offerType = offerType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getOfferPrice() { return offerPrice; }
    public void setOfferPrice(double offerPrice) { this.offerPrice = offerPrice; }

    public LocalDate getOfferStartDate() { return offerStartDate; }
    public void setOfferStartDate(LocalDate offerStartDate) { this.offerStartDate = offerStartDate; }

    public LocalDate getOfferEndDate() { return offerEndDate; }
    public void setOfferEndDate(LocalDate offerEndDate) { this.offerEndDate = offerEndDate; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }

    public String getStockStatus() { return stockStatus; }
    public void setStockStatus(String stockStatus) { this.stockStatus = stockStatus; }

    public Integer getStockQuantity() { return stockQuantity != null ? stockQuantity : 0; }
    public void setStockQuantity(Integer stockQuantity) {
        this.stockQuantity = stockQuantity;
        if (this.stockQuantity != null && this.stockQuantity <= 0) {
            this.stockStatus = "OUT_OF_STOCK";
        }
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
