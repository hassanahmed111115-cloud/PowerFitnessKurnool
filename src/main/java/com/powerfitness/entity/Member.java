package com.powerfitness.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String memberCode; // e.g. PFK-1001

    @Column(nullable = false)
    private String fullName;

    @Column(nullable = false, length = 15)
    private String phoneNumber;

    @Column(length = 1000)
    private String photoUrl;

    @Column(nullable = false)
    private LocalDate admissionDate;

    @Column(nullable = false)
    private String subscriptionPlan; // 1 Month, 3 Months, 6 Months, 1 Year

    @Column(nullable = false)
    private String trainingCategory; // Cardio, Strength Training

    @Column(nullable = false)
    private String batch; // Morning Batch, Evening Batch

    private boolean hasCardio;

    private double totalFee;
    @Column(nullable = true)
    private Double customPrice; // Saved member-specific custom membership price

    @Column(nullable = true)
    private Integer durationMonths; // Custom membership duration in months

    private LocalDate startDate;

    private LocalDate expiryDate;

    private String status; // ACTIVE, EXPIRING_SOON, EXPIRED

    @Column(length = 1000)
    private String notes;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id")
    private User user;

    public Member() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    public String getFullName() { return fullName; }
    public void setFullName(String fullName) { this.fullName = fullName; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getPhotoUrl() { return photoUrl; }
    public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }

    public LocalDate getAdmissionDate() { return admissionDate; }
    public void setAdmissionDate(LocalDate admissionDate) { this.admissionDate = admissionDate; }

    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

    public String getTrainingCategory() { return trainingCategory; }
    public void setTrainingCategory(String trainingCategory) { this.trainingCategory = trainingCategory; }

    public String getBatch() { return batch; }
    public void setBatch(String batch) { this.batch = batch; }

    public boolean isHasCardio() { return hasCardio; }
    public void setHasCardio(boolean hasCardio) { this.hasCardio = hasCardio; }

    public double getTotalFee() { return totalFee; }
    public void setTotalFee(double totalFee) { this.totalFee = totalFee; }

    public Double getCustomPrice() { return customPrice != null ? customPrice : totalFee; }
    public void setCustomPrice(Double customPrice) { this.customPrice = customPrice; }

    public Integer getDurationMonths() {
        if (durationMonths != null && durationMonths > 0) return durationMonths;
        if (startDate != null && expiryDate != null) {
            long months = java.time.temporal.ChronoUnit.MONTHS.between(startDate, expiryDate);
            if (months > 0) return (int) months;
        }
        if (subscriptionPlan != null) {
            String p = subscriptionPlan.toLowerCase();
            if (p.contains("1 year") || p.contains("12 month")) return 12;
            if (p.contains("6 month")) return 6;
            if (p.contains("3 month")) return 3;
            if (p.contains("2 month")) return 2;
            if (p.contains("1 month")) return 1;
        }
        return 1;
    }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    @Transient
    public long getDaysRemaining() {
        if (expiryDate == null) return 0;
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    @Transient
    public long getDaysCompleted() {
        if (startDate == null) return 0;
        long completed = ChronoUnit.DAYS.between(startDate, LocalDate.now());
        return Math.max(0, completed);
    }

    @Transient
    public long getTotalDays() {
        if (startDate == null || expiryDate == null) return 0;
        return Math.max(1, ChronoUnit.DAYS.between(startDate, expiryDate));
    }

    @Transient
    public String getStatusBadgeText() {
        long remaining = getDaysRemaining();
        if (remaining < 0) {
            return "❌ Subscription Expired";
        } else if (remaining <= 7) {
            return "⚠️ Renewal Required — " + remaining + " Days Left";
        } else {
            return "Active (" + remaining + " Days Left)";
        }
    }
}
