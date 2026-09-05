package com.powerfitness.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "upi_settings")
public class UpiSetting {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String upiId;

    @Column(nullable = false)
    private String merchantName;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String qrCodeUrl;

    @Column(length = 500)
    private String notes;

    public UpiSetting() {}

    public UpiSetting(String upiId, String merchantName, String qrCodeUrl, String notes) {
        this.upiId = upiId;
        this.merchantName = merchantName;
        this.qrCodeUrl = qrCodeUrl;
        this.notes = notes;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUpiId() { return upiId; }
    public void setUpiId(String upiId) { this.upiId = upiId; }

    public String getMerchantName() { return merchantName; }
    public void setMerchantName(String merchantName) { this.merchantName = merchantName; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
