package com.powerfitness.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_otp_challenges")
public class AdminOtpChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 128)
    private String challengeToken;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String mobileNumber;

    @Column(nullable = false, length = 128)
    private String otpHashed;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    @Column(nullable = false)
    private int failedAttempts = 0;

    @Column(nullable = false)
    private boolean verified = false;

    @Column(nullable = false)
    private boolean used = false;

    @Column(unique = true, length = 128)
    private String resetToken;

    private LocalDateTime resetTokenExpiry;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public AdminOtpChallenge() {}

    public AdminOtpChallenge(String challengeToken, User user, String mobileNumber, String otpHashed, LocalDateTime expiryDate) {
        this.challengeToken = challengeToken;
        this.user = user;
        this.mobileNumber = mobileNumber;
        this.otpHashed = otpHashed;
        this.expiryDate = expiryDate;
        this.failedAttempts = 0;
        this.verified = false;
        this.used = false;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    public boolean isResetTokenExpired() {
        return resetTokenExpiry == null || LocalDateTime.now().isAfter(resetTokenExpiry);
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getChallengeToken() { return challengeToken; }
    public void setChallengeToken(String challengeToken) { this.challengeToken = challengeToken; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getMobileNumber() { return mobileNumber; }
    public void setMobileNumber(String mobileNumber) { this.mobileNumber = mobileNumber; }

    public String getOtpHashed() { return otpHashed; }
    public void setOtpHashed(String otpHashed) { this.otpHashed = otpHashed; }

    public LocalDateTime getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDateTime expiryDate) { this.expiryDate = expiryDate; }

    public int getFailedAttempts() { return failedAttempts; }
    public void setFailedAttempts(int failedAttempts) { this.failedAttempts = failedAttempts; }

    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }

    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }

    public String getResetToken() { return resetToken; }
    public void setResetToken(String resetToken) { this.resetToken = resetToken; }

    public LocalDateTime getResetTokenExpiry() { return resetTokenExpiry; }
    public void setResetTokenExpiry(LocalDateTime resetTokenExpiry) { this.resetTokenExpiry = resetTokenExpiry; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
