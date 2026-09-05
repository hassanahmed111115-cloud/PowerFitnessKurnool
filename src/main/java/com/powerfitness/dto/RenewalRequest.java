package com.powerfitness.dto;

public class RenewalRequest {
    private String subscriptionPlan; // 1 Month, 3 Months, 6 Months, 1 Year
    private boolean cardioOption; // ₹500 extra
    private String paymentMethod; // UPI, Cash, Other
    private String paymentStatus; // Paid, Pending, Failed
    private String transactionRef;
    private String notes;

    public RenewalRequest() {}

    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

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
