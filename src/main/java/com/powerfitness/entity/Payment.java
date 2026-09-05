package com.powerfitness.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
public class Payment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String receiptNumber;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id", nullable = true)
    private Member member;

    @Column(nullable = true)
    private String paymentType = "MEMBERSHIP"; // MEMBERSHIP, SUPPLEMENT

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplement_order_id", nullable = true)
    private SupplementOrder supplementOrder;

    private String orderNumber; // e.g. PFK-SUP-1001
    private String productName;
    private Integer quantity = 1;
    private Double originalPrice = 0.0;
    private Double discountValue = 0.0;
    private Double finalPrice = 0.0;

    private String memberName;
    private String memberPhone;
    private String memberCode;

    private String batch; // Morning Batch, Evening Batch

    private double amount;
    private double baseFee;
    private double cardioFee;

    @Column(nullable = false)
    private LocalDate paymentDate;

    @Column(nullable = false)
    private String paymentMethod; // UPI, Cash, Other

    @Column(nullable = true)
    private String subscriptionPlan;

    @Column(nullable = false)
    private String paymentStatus; // Paid, Pending, Failed

    private String transactionRef;

    @Column(length = 500)
    private String notes;

    private LocalDateTime createdAt = LocalDateTime.now();

    public Payment() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getReceiptNumber() { return receiptNumber; }
    public void setReceiptNumber(String receiptNumber) { this.receiptNumber = receiptNumber; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public String getPaymentType() { return paymentType != null ? paymentType : "MEMBERSHIP"; }
    public void setPaymentType(String paymentType) { this.paymentType = paymentType; }

    public SupplementOrder getSupplementOrder() { return supplementOrder; }
    public void setSupplementOrder(SupplementOrder supplementOrder) { this.supplementOrder = supplementOrder; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }

    public int getQuantity() { return quantity != null ? quantity : 1; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getOriginalPrice() { return originalPrice != null ? originalPrice : 0.0; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public double getDiscountValue() { return discountValue != null ? discountValue : 0.0; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getFinalPrice() { return finalPrice != null ? finalPrice : 0.0; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMemberPhone() { return memberPhone; }
    public void setMemberPhone(String memberPhone) { this.memberPhone = memberPhone; }

    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    public String getBatch() {
        if (batch != null && !batch.trim().isEmpty()) {
            return batch;
        }
        if (member != null && member.getBatch() != null && !member.getBatch().trim().isEmpty()) {
            return member.getBatch();
        }
        if (supplementOrder != null && supplementOrder.getMember() != null && supplementOrder.getMember().getBatch() != null) {
            return supplementOrder.getMember().getBatch();
        }
        return "Morning Batch";
    }

    public void setBatch(String batch) { this.batch = batch; }

    public double getAmount() { return amount; }
    public void setAmount(double amount) { this.amount = amount; }

    public double getBaseFee() { return baseFee; }
    public void setBaseFee(double baseFee) { this.baseFee = baseFee; }

    public double getCardioFee() { return cardioFee; }
    public void setCardioFee(double cardioFee) { this.cardioFee = cardioFee; }

    public LocalDate getPaymentDate() { return paymentDate; }
    public void setPaymentDate(LocalDate paymentDate) { this.paymentDate = paymentDate; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getSubscriptionPlan() { return subscriptionPlan; }
    public void setSubscriptionPlan(String subscriptionPlan) { this.subscriptionPlan = subscriptionPlan; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getTransactionRef() { return transactionRef; }
    public void setTransactionRef(String transactionRef) { this.transactionRef = transactionRef; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
