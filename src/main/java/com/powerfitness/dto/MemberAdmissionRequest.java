package com.powerfitness.dto;

import java.time.LocalDate;

public class MemberAdmissionRequest {
    private String fullName;
    private String phoneNumber;
    private String photoUrl;
    private LocalDate admissionDate;
    private String subscriptionPlan; // 1 Month, 3 Months, 6 Months, 1 Year
    private String trainingCategory; // Cardio, Strength Training
    private String batch; // Morning Batch, Evening Batch
    private boolean cardioOption; // ₹500 extra
    private String paymentMethod; // UPI, Cash, Other
    private String paymentStatus; // Paid, Pending, Failed
    private String transactionRef;
    private String notes;

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
}
