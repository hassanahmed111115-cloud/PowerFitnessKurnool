package com.powerfitness.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplement_orders")
public class SupplementOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String orderNumber; // e.g. ORD-1001

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "supplement_id", nullable = false)
    private Supplement supplement;

    @Column(nullable = false)
    private int quantity = 1;

    private double originalPrice;

    private String offerType;

    private double discountValue;

    private double finalPrice; // Unit price after discount

    private double totalAmount; // finalPrice * quantity

    @Column(nullable = false)
    private String orderStatus = "PAYMENT_PENDING"; // PENDING, PAYMENT_PENDING, PAID, READY_FOR_COLLECTION, COLLECTED, CANCELLED

    @Column(nullable = false)
    private String paymentMethod = "UPI"; // UPI, CASH, CARD

    @Column(nullable = false)
    private String paymentStatus = "PENDING"; // PENDING, PAID

    private String upiTransactionRef;

    private LocalDateTime orderDate = LocalDateTime.now();

    private LocalDateTime collectionDate;

    @Column(length = 1000)
    private String collectionPhotoUrl; // Photo captured by device camera at front desk

    private String collectedByStaff; // Front desk staff name

    @Column(length = 2000)
    private String notes;

    private Long paymentId; // Linked Payment entity ID

    private String paymentReceiptNumber; // Linked Payment receipt (e.g. PFK-PAY-1001)

    private LocalDateTime updatedAt = LocalDateTime.now();

    public SupplementOrder() {}

    public SupplementOrder(String orderNumber, User user, Member member, Supplement supplement, int quantity,
                           double originalPrice, String offerType, double discountValue, double finalPrice,
                           double totalAmount, String paymentMethod, String paymentStatus, String upiTransactionRef) {
        this.orderNumber = orderNumber;
        this.user = user;
        this.member = member;
        this.supplement = supplement;
        this.quantity = quantity;
        this.originalPrice = originalPrice;
        this.offerType = offerType;
        this.discountValue = discountValue;
        this.finalPrice = finalPrice;
        this.totalAmount = totalAmount;
        this.paymentMethod = paymentMethod;
        this.paymentStatus = paymentStatus;
        this.upiTransactionRef = upiTransactionRef;
        this.orderStatus = "PAID".equalsIgnoreCase(paymentStatus) ? "READY_FOR_COLLECTION" : "PAYMENT_PENDING";
        this.orderDate = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getOrderNumber() { return orderNumber; }
    public void setOrderNumber(String orderNumber) { this.orderNumber = orderNumber; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public Member getMember() { return member; }
    public void setMember(Member member) { this.member = member; }

    public Supplement getSupplement() { return supplement; }
    public void setSupplement(Supplement supplement) { this.supplement = supplement; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public String getOfferType() { return offerType; }
    public void setOfferType(String offerType) { this.offerType = offerType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public String getOrderStatus() { return orderStatus; }
    public void setOrderStatus(String orderStatus) { this.orderStatus = orderStatus; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getUpiTransactionRef() { return upiTransactionRef; }
    public void setUpiTransactionRef(String upiTransactionRef) { this.upiTransactionRef = upiTransactionRef; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public LocalDateTime getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDateTime collectionDate) { this.collectionDate = collectionDate; }

    public String getCollectionPhotoUrl() { return collectionPhotoUrl; }
    public void setCollectionPhotoUrl(String collectionPhotoUrl) { this.collectionPhotoUrl = collectionPhotoUrl; }

    public String getCollectedByStaff() { return collectedByStaff; }
    public void setCollectedByStaff(String collectedByStaff) { this.collectedByStaff = collectedByStaff; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Long getPaymentId() { return paymentId; }
    public void setPaymentId(Long paymentId) { this.paymentId = paymentId; }

    public String getPaymentReceiptNumber() { return paymentReceiptNumber; }
    public void setPaymentReceiptNumber(String paymentReceiptNumber) { this.paymentReceiptNumber = paymentReceiptNumber; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
