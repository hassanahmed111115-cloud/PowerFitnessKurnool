package com.powerfitness.dto;

import java.time.LocalDate;

public class MemberAdmissionRequest {
    private String fullName;
    private String phoneNumber;
    private String photoUrl;
    private LocalDate admissionDate;
    private Integer durationMonths; // Custom Membership Duration in Months
    private String subscriptionPlan; // 1 Month, 3 Months, 6 Months, 1 Year
    private String trainingCategory; // Cardio, Strength Training
    private String batch; // Morning Batch, Evening Batch
    private boolean cardioOption; // ₹500 extra
    private String paymentMethod; // UPI, Cash, Other
    private String paymentStatus; // Paid, Pending, Failed
    private String transactionRef;
    private String notes;
    private Double customPrice; // Custom Membership Price (₹) entered by Admin
    private Double customMembershipPrice;
    private Double amount;

    public MemberAdmissionRequest() {}

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

    public boolean isCardioOption() { return cardioOption; }
    public void setCardioOption(boolean cardioOption) { this.cardioOption = cardioOption; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Double getCustomPrice() {
        if (customPrice != null) return customPrice;
        if (customMembershipPrice != null) return customMembershipPrice;
        return amount;
    }
    public void setCustomPrice(Double customPrice) { this.customPrice = customPrice; }

    public Double getCustomMembershipPrice() { return customMembershipPrice; }
    public void setCustomMembershipPrice(Double customMembershipPrice) { this.customMembershipPrice = customMembershipPrice; }

    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    public Integer getDurationMonths() {
        if (durationMonths != null) return durationMonths;
        if (subscriptionPlan != null) {
            String p = subscriptionPlan.trim().toLowerCase();
            if (p.contains("1 year") || p.contains("12 month")) return 12;
            if (p.contains("6 month")) return 6;
            if (p.contains("3 month")) return 3;
            if (p.contains("2 month")) return 2;
            if (p.contains("1 month")) return 1;
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+)").matcher(p);
            if (m.find()) {
                try {
                    return Integer.parseInt(m.group(1));
                } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }
}
