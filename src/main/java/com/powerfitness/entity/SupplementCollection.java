package com.powerfitness.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplement_collections")
public class SupplementCollection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String collectionNumber; // e.g. COL-1001

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "order_id", nullable = false)
    private SupplementOrder order;

    @Column(nullable = false)
    private String memberName;

    private String memberCode; // e.g. PFK-1001

    @Column(length = 1000)
    private String memberPhotoUrl;

    private String phoneNumber;

    @Column(nullable = false)
    private String supplementName;

    private String supplementCategory;

    @Column(length = 1000)
    private String supplementImageUrl;

    @Column(nullable = false)
    private int quantity;

    private double originalPrice;

    private double finalPrice;

    private double totalAmount;

    @Column(nullable = false)
    private LocalDateTime orderDate;

    @Column(nullable = false)
    private LocalDateTime collectionDate = LocalDateTime.now();

    private String collectionDateFormatted; // e.g. "04 September 2026"

    private String collectionTimeFormatted; // e.g. "03:25 PM"

    @Column(length = 1000, nullable = false)
    private String collectionPhotoUrl; // Photo taken during physical collection

    private String collectedByStaff = "Admin / Front Desk";

    private String status = "COLLECTED";

    private String paymentStatus = "PAID";

    @Column(length = 2000)
    private String notes;

    private LocalDateTime createdAt = LocalDateTime.now();

    public SupplementCollection() {}

    public SupplementCollection(String collectionNumber, SupplementOrder order, String collectionPhotoUrl,
                                String collectedByStaff, String collectionDateFormatted, String collectionTimeFormatted) {
        this.collectionNumber = collectionNumber;
        this.order = order;
        this.memberName = (order.getMember() != null) ? order.getMember().getFullName() : order.getUser().getFullName();
        this.memberCode = (order.getMember() != null) ? order.getMember().getMemberCode() : ("USR-" + order.getUser().getId());
        this.memberPhotoUrl = (order.getMember() != null) ? order.getMember().getPhotoUrl() : null;
        this.phoneNumber = (order.getMember() != null) ? order.getMember().getPhoneNumber() : order.getUser().getUsername();
        this.supplementName = order.getSupplement().getName();
        this.supplementCategory = order.getSupplement().getCategory();
        this.supplementImageUrl = order.getSupplement().getImageUrl();
        this.quantity = order.getQuantity();
        this.originalPrice = order.getOriginalPrice();
        this.finalPrice = order.getFinalPrice();
        this.totalAmount = order.getTotalAmount();
        this.orderDate = order.getOrderDate();
        this.collectionDate = LocalDateTime.now();
        this.collectionDateFormatted = collectionDateFormatted;
        this.collectionTimeFormatted = collectionTimeFormatted;
        this.collectionPhotoUrl = collectionPhotoUrl;
        this.collectedByStaff = collectedByStaff;
        this.status = "COLLECTED";
        this.paymentStatus = "PAID";
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCollectionNumber() { return collectionNumber; }
    public void setCollectionNumber(String collectionNumber) { this.collectionNumber = collectionNumber; }

    public SupplementOrder getOrder() { return order; }
    public void setOrder(SupplementOrder order) { this.order = order; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }

    public String getMemberCode() { return memberCode; }
    public void setMemberCode(String memberCode) { this.memberCode = memberCode; }

    public String getMemberPhotoUrl() { return memberPhotoUrl; }
    public void setMemberPhotoUrl(String memberPhotoUrl) { this.memberPhotoUrl = memberPhotoUrl; }

    public String getPhoneNumber() { return phoneNumber; }
    public void setPhoneNumber(String phoneNumber) { this.phoneNumber = phoneNumber; }

    public String getSupplementName() { return supplementName; }
    public void setSupplementName(String supplementName) { this.supplementName = supplementName; }

    public String getSupplementCategory() { return supplementCategory; }
    public void setSupplementCategory(String supplementCategory) { this.supplementCategory = supplementCategory; }

    public String getSupplementImageUrl() { return supplementImageUrl; }
    public void setSupplementImageUrl(String supplementImageUrl) { this.supplementImageUrl = supplementImageUrl; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public double getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(double originalPrice) { this.originalPrice = originalPrice; }

    public double getFinalPrice() { return finalPrice; }
    public void setFinalPrice(double finalPrice) { this.finalPrice = finalPrice; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }

    public LocalDateTime getCollectionDate() { return collectionDate; }
    public void setCollectionDate(LocalDateTime collectionDate) { this.collectionDate = collectionDate; }

    public String getCollectionDateFormatted() { return collectionDateFormatted; }
    public void setCollectionDateFormatted(String collectionDateFormatted) { this.collectionDateFormatted = collectionDateFormatted; }

    public String getCollectionTimeFormatted() { return collectionTimeFormatted; }
    public void setCollectionTimeFormatted(String collectionTimeFormatted) { this.collectionTimeFormatted = collectionTimeFormatted; }

    public String getCollectionPhotoUrl() { return collectionPhotoUrl; }
    public void setCollectionPhotoUrl(String collectionPhotoUrl) { this.collectionPhotoUrl = collectionPhotoUrl; }

    public String getCollectedByStaff() { return collectedByStaff; }
    public void setCollectedByStaff(String collectedByStaff) { this.collectedByStaff = collectedByStaff; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
