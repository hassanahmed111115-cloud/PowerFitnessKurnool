package com.powerfitness.service;

import com.powerfitness.entity.AboutUs;
import com.powerfitness.entity.AdminOtpChallenge;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import com.powerfitness.repository.AboutUsRepository;
import com.powerfitness.repository.AdminOtpChallengeRepository;
import com.powerfitness.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordRecoveryService {

    private static final Logger log = LoggerFactory.getLogger(PasswordRecoveryService.class);

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AboutUsRepository aboutUsRepository;

    @Autowired
    private AdminOtpChallengeRepository challengeRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private SmsService smsService;

    private static final SecureRandom secureRandom = new SecureRandom();
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int RESET_TOKEN_EXPIRY_MINUTES = 5;
    private static final int RESEND_COOLDOWN_SECONDS = 60;
    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int HOURLY_OTP_LIMIT = 5;

    // In-memory cooldown tracking per normalized mobile number
    private final Map<String, LocalDateTime> requestCooldowns = new ConcurrentHashMap<>();

    public String normalizePhone(String input) {
        if (input == null) return "";
        String cleaned = input.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("91") && cleaned.length() == 12) {
            cleaned = cleaned.substring(2);
        } else if (cleaned.startsWith("0") && cleaned.length() == 11) {
            cleaned = cleaned.substring(1);
        }
        return cleaned;
    }

    public Optional<User> findAdminByMobileNumber(String rawMobile) {
        String normalized = normalizePhone(rawMobile);
        if (normalized.length() < 10) {
            return Optional.empty();
        }

        List<User> admins = userRepository.findByRoleOrderByIdAsc(Role.ADMIN);
        for (User admin : admins) {
            if (!admin.isEnabled()) continue;

            // 1. Direct phoneNumber field on User entity
            if (admin.getPhoneNumber() != null && normalizePhone(admin.getPhoneNumber()).equals(normalized)) {
                return Optional.of(admin);
            }

            // 2. Admin's username is the phone number
            if (normalizePhone(admin.getUsername()).equals(normalized)) {
                return Optional.of(admin);
            }

            // 3. Primary Gym Owner Admin (id = 1 or username "admin") matching AboutUs phone
            if ("admin".equalsIgnoreCase(admin.getUsername()) || Long.valueOf(1L).equals(admin.getId())) {
                Optional<AboutUs> aboutOpt = aboutUsRepository.findAll().stream().findFirst();
                if (aboutOpt.isPresent() && aboutOpt.get().getPhone() != null) {
                    if (normalizePhone(aboutOpt.get().getPhone()).equals(normalized)) {
                        return Optional.of(admin);
                    }
                }
            }

            // 4. Check ADMIN_RECOVERY_MOBILE environment variable if configured
            String envPhone = System.getenv("ADMIN_RECOVERY_MOBILE");
            if (envPhone != null && normalizePhone(envPhone).equals(normalized)) {
                return Optional.of(admin);
            }

            // 5. Check if admin has matching name with a registered user phone in the database
            if (admin.getFullName() != null && !admin.getFullName().trim().isEmpty()) {
                List<User> matchingUsers = userRepository.findAll().stream()
                        .filter(u -> u.getFullName() != null
                                && u.getFullName().trim().equalsIgnoreCase(admin.getFullName().trim())
                                && normalizePhone(u.getUsername()).equals(normalized))
                        .toList();
                if (!matchingUsers.isEmpty()) {
                    return Optional.of(admin);
                }
            }
        }
        return Optional.empty();
    }

    @Transactional
    public Map<String, Object> requestAdminOtp(String mobileNumber) {
        if (mobileNumber == null || mobileNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is required");
        }

        String normalized = normalizePhone(mobileNumber.trim());
        if (normalized.length() < 10) {
            throw new IllegalArgumentException("Please enter a valid 10-digit mobile number");
        }

        // 1. Check Resend Cooldown (60 seconds)
        LocalDateTime lastReq = requestCooldowns.get(normalized);
        if (lastReq != null && lastReq.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            long remaining = java.time.Duration.between(LocalDateTime.now(), lastReq.plusSeconds(RESEND_COOLDOWN_SECONDS)).getSeconds();
            throw new IllegalArgumentException("Please wait " + Math.max(1, remaining) + " seconds before requesting another OTP.");
        }

        // 2. Check Hourly Rate Limit (max 5 requests per hour)
        long recentCount = challengeRepository.countByMobileNumberAndCreatedAtAfter(normalized, LocalDateTime.now().minusHours(1));
        if (recentCount >= HOURLY_OTP_LIMIT) {
            throw new IllegalArgumentException("Too many OTP requests for this mobile number. Please try again after 1 hour.");
        }

        requestCooldowns.put(normalized, LocalDateTime.now());

        Optional<User> adminOpt = findAdminByMobileNumber(normalized);

        // Account Enumeration Protection:
        // Generic message regardless of whether the admin account was found
        String genericMessage = "If the mobile number is registered for an admin account, an OTP has been sent.";

        if (adminOpt.isPresent()) {
            User admin = adminOpt.get();

            // Invalidate any existing active challenges for this admin
            List<AdminOtpChallenge> activeChallenges = challengeRepository.findByUserAndUsedFalse(admin);
            for (AdminOtpChallenge c : activeChallenges) {
                c.setUsed(true);
                c.setOtpHashed("SUPERSEDED");
                challengeRepository.save(c);
            }

            // Generate cryptographically secure 6-digit OTP
            int otpValue = 100000 + secureRandom.nextInt(900000);
            String otpString = String.valueOf(otpValue);

            // Store ONLY salted BCrypt hash of OTP in database, NEVER plaintext
            String otpHashed = passwordEncoder.encode(otpString);
            String challengeToken = UUID.randomUUID().toString();
            LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

            AdminOtpChallenge challenge = new AdminOtpChallenge(challengeToken, admin, normalized, otpHashed, expiry);
            challengeRepository.save(challenge);

            // Send OTP through real configured SMS provider (never logging the OTP)
            try {
                smsService.sendOtp(normalized, otpString);
            } catch (Exception e) {
                log.error("Failed to send OTP SMS to registered admin: {}", e.getMessage());
            }

            Map<String, Object> res = new HashMap<>();
            res.put("message", genericMessage);
            res.put("challengeToken", challengeToken);
            res.put("cooldownSeconds", RESEND_COOLDOWN_SECONDS);
            return res;
        }

        // If no active admin matches, return indistinguishable generic response with a dummy challenge token
        Map<String, Object> genericRes = new HashMap<>();
        genericRes.put("message", genericMessage);
        genericRes.put("challengeToken", UUID.randomUUID().toString());
        genericRes.put("cooldownSeconds", RESEND_COOLDOWN_SECONDS);
        return genericRes;
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public Map<String, Object> verifyOtp(String challengeToken, String otp) {
        if (challengeToken == null || challengeToken.trim().isEmpty() || otp == null || otp.trim().isEmpty()) {
            throw new IllegalArgumentException("Challenge session token and OTP are required");
        }

        String cleanOtp = otp.trim();
        if (cleanOtp.length() != 6 || !cleanOtp.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("OTP must be exactly 6 digits");
        }

        Optional<AdminOtpChallenge> challengeOpt = challengeRepository.findByChallengeToken(challengeToken.trim());
        if (challengeOpt.isEmpty() || challengeOpt.get().isUsed()) {
            throw new IllegalArgumentException("Invalid, expired, or already used OTP session. Please request a new OTP.");
        }

        AdminOtpChallenge challenge = challengeOpt.get();

        if (challenge.isExpired()) {
            challenge.setUsed(true);
            challengeRepository.save(challenge);
            throw new IllegalArgumentException("OTP has expired. Please request a new OTP.");
        }

        if (challenge.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            challenge.setUsed(true);
            challengeRepository.save(challenge);
            throw new IllegalArgumentException("Too many failed attempts. This OTP has been invalidated. Please request a new OTP.");
        }

        // Verify entered OTP against secure BCrypt hash
        if (!passwordEncoder.matches(cleanOtp, challenge.getOtpHashed())) {
            int newFailedCount = challenge.getFailedAttempts() + 1;
            challenge.setFailedAttempts(newFailedCount);
            if (newFailedCount >= MAX_FAILED_ATTEMPTS) {
                challenge.setUsed(true);
            }
            challengeRepository.save(challenge);

            int remaining = MAX_FAILED_ATTEMPTS - newFailedCount;
            if (remaining <= 0) {
                throw new IllegalArgumentException("Too many incorrect attempts. This OTP has been invalidated. Please request a new OTP.");
            }
            throw new IllegalArgumentException("Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }

        // Successful verification:
        // Immediately invalidate the OTP so it can NEVER be verified or reused again
        challenge.setOtpHashed("INVALIDATED");
        challenge.setVerified(true);

        // Generate a single-use cryptographically secure reset token
        String resetToken = "PRT-" + UUID.randomUUID().toString();
        challenge.setResetToken(resetToken);
        challenge.setResetTokenExpiry(LocalDateTime.now().plusMinutes(RESET_TOKEN_EXPIRY_MINUTES));
        challengeRepository.save(challenge);

        Map<String, Object> res = new HashMap<>();
        res.put("message", "OTP verified successfully. You may now set a new admin password.");
        res.put("resetToken", resetToken);
        return res;
    }

    @Transactional
    public void resetAdminPassword(String resetToken, String newPassword, String confirmPassword) {
        if (resetToken == null || resetToken.trim().isEmpty()) {
            throw new IllegalArgumentException("Password reset session token is required");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        Optional<AdminOtpChallenge> challengeOpt = challengeRepository.findByResetToken(resetToken.trim());
        if (challengeOpt.isEmpty() || !challengeOpt.get().isVerified() || challengeOpt.get().isUsed() || challengeOpt.get().isResetTokenExpired()) {
            throw new IllegalArgumentException("Invalid or expired password reset session. Please request a new OTP.");
        }

        AdminOtpChallenge challenge = challengeOpt.get();
        User admin = challenge.getUser();

        if (admin == null || admin.getRole() != Role.ADMIN || !admin.isEnabled()) {
            throw new IllegalArgumentException("Invalid administrator account");
        }

        // Store new password with existing BCrypt hashing
        admin.setPassword(authService.hashPassword(newPassword));
        admin.setUpdatedAt(LocalDateTime.now());
        userRepository.save(admin);

        // Completely invalidate the challenge and reset token
        challenge.setUsed(true);
        challenge.setResetToken(null);
        challengeRepository.save(challenge);

        // Invalidate existing admin sessions/tokens
        authService.invalidateSessionsForUser(admin.getId());

        log.info("Administrator password successfully updated via Mobile OTP recovery for admin ID {}", admin.getId());
    }

    @Transactional
    public Map<String, Object> requestAdminPhoneRegistrationOtp(User admin, String rawMobileNumber) {
        if (admin == null || admin.getRole() != Role.ADMIN || !admin.isEnabled()) {
            throw new IllegalArgumentException("Only authenticated administrators can register a recovery mobile number.");
        }

        if (rawMobileNumber == null || rawMobileNumber.trim().isEmpty()) {
            throw new IllegalArgumentException("Mobile number is required");
        }

        String normalized = normalizePhone(rawMobileNumber.trim());
        if (normalized.length() != 10) {
            throw new IllegalArgumentException("Please enter a valid 10-digit mobile number");
        }

        // Check if another admin already registered this mobile number
        List<User> admins = userRepository.findByRoleOrderByIdAsc(Role.ADMIN);
        for (User otherAdmin : admins) {
            if (!otherAdmin.getId().equals(admin.getId()) && otherAdmin.isEnabled() && otherAdmin.getPhoneNumber() != null) {
                if (normalizePhone(otherAdmin.getPhoneNumber()).equals(normalized)) {
                    throw new IllegalArgumentException("This mobile number is already registered to another administrator account.");
                }
            }
        }

        // Check Resend Cooldown (60 seconds)
        String cooldownKey = "REG-" + admin.getId();
        LocalDateTime lastReq = requestCooldowns.get(cooldownKey);
        if (lastReq != null && lastReq.plusSeconds(RESEND_COOLDOWN_SECONDS).isAfter(LocalDateTime.now())) {
            long remaining = java.time.Duration.between(LocalDateTime.now(), lastReq.plusSeconds(RESEND_COOLDOWN_SECONDS)).getSeconds();
            throw new IllegalArgumentException("Please wait " + Math.max(1, remaining) + " seconds before requesting another OTP.");
        }

        // Check Hourly Rate Limit (max 5 requests per hour)
        long recentCount = challengeRepository.countByMobileNumberAndCreatedAtAfter(normalized, LocalDateTime.now().minusHours(1));
        if (recentCount >= HOURLY_OTP_LIMIT) {
            throw new IllegalArgumentException("Too many OTP requests for this mobile number. Please try again after 1 hour.");
        }

        requestCooldowns.put(cooldownKey, LocalDateTime.now());

        // Invalidate any existing active challenges for this admin
        List<AdminOtpChallenge> activeChallenges = challengeRepository.findByUserAndUsedFalse(admin);
        for (AdminOtpChallenge c : activeChallenges) {
            c.setUsed(true);
            c.setOtpHashed("SUPERSEDED");
            challengeRepository.save(c);
        }

        // Generate cryptographically secure 6-digit OTP
        int otpValue = 100000 + secureRandom.nextInt(900000);
        String otpString = String.valueOf(otpValue);

        // Store ONLY salted BCrypt hash of OTP in database, NEVER plaintext
        String otpHashed = passwordEncoder.encode(otpString);
        String challengeToken = "REG-" + UUID.randomUUID().toString();
        LocalDateTime expiry = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);

        AdminOtpChallenge challenge = new AdminOtpChallenge(challengeToken, admin, normalized, otpHashed, expiry);
        challengeRepository.save(challenge);

        // Send OTP through real configured SMS provider (never logging the OTP)
        try {
            smsService.sendOtp(normalized, otpString);
        } catch (Exception e) {
            log.error("Failed to send phone registration OTP SMS: {}", e.getMessage());
            throw new IllegalArgumentException("Unable to send SMS: " + e.getMessage());
        }

        Map<String, Object> res = new HashMap<>();
        res.put("message", "A 6-digit verification OTP has been sent via SMS to +91 " + normalized + ".");
        res.put("challengeToken", challengeToken);
        res.put("cooldownSeconds", RESEND_COOLDOWN_SECONDS);
        return res;
    }

    @Transactional(noRollbackFor = IllegalArgumentException.class)
    public Map<String, Object> verifyAdminPhoneRegistrationOtp(User admin, String challengeToken, String otp) {
        if (admin == null || admin.getRole() != Role.ADMIN || !admin.isEnabled()) {
            throw new IllegalArgumentException("Only authenticated administrators can verify a recovery mobile number.");
        }

        if (challengeToken == null || challengeToken.trim().isEmpty() || otp == null || otp.trim().isEmpty()) {
            throw new IllegalArgumentException("Challenge session token and OTP are required");
        }

        String cleanOtp = otp.trim();
        if (cleanOtp.length() != 6 || !cleanOtp.matches("^\\d{6}$")) {
            throw new IllegalArgumentException("OTP must be exactly 6 digits");
        }

        Optional<AdminOtpChallenge> challengeOpt = challengeRepository.findByChallengeToken(challengeToken.trim());
        if (challengeOpt.isEmpty() || challengeOpt.get().isUsed()) {
            throw new IllegalArgumentException("Invalid, expired, or already used OTP session. Please request a new OTP.");
        }

        AdminOtpChallenge challenge = challengeOpt.get();

        // Verify challenge belongs to this admin
        if (!challenge.getUser().getId().equals(admin.getId())) {
            throw new IllegalArgumentException("Unauthorized OTP challenge session.");
        }

        if (challenge.isExpired()) {
            challenge.setUsed(true);
            challengeRepository.save(challenge);
            throw new IllegalArgumentException("OTP has expired. Please request a new OTP.");
        }

        if (challenge.getFailedAttempts() >= MAX_FAILED_ATTEMPTS) {
            challenge.setUsed(true);
            challengeRepository.save(challenge);
            throw new IllegalArgumentException("Too many failed attempts. This OTP has been invalidated. Please request a new OTP.");
        }

        // Verify entered OTP against secure BCrypt hash
        if (!passwordEncoder.matches(cleanOtp, challenge.getOtpHashed())) {
            int newFailedCount = challenge.getFailedAttempts() + 1;
            challenge.setFailedAttempts(newFailedCount);
            if (newFailedCount >= MAX_FAILED_ATTEMPTS) {
                challenge.setUsed(true);
            }
            challengeRepository.save(challenge);

            int remaining = MAX_FAILED_ATTEMPTS - newFailedCount;
            if (remaining <= 0) {
                throw new IllegalArgumentException("Too many incorrect attempts. This OTP has been invalidated. Please request a new OTP.");
            }
            throw new IllegalArgumentException("Incorrect OTP. " + remaining + " attempt(s) remaining.");
        }

        // Successful verification:
        // Immediately invalidate the OTP so it can NEVER be reused
        challenge.setOtpHashed("INVALIDATED");
        challenge.setVerified(true);
        challenge.setUsed(true);
        challengeRepository.save(challenge);

        // Update the admin's recovery mobile number in users table
        String registeredMobile = challenge.getMobileNumber();
        admin.setPhoneNumber(registeredMobile);
        admin.setUpdatedAt(LocalDateTime.now());
        userRepository.save(admin);

        log.info("Administrator ID {} successfully registered/updated recovery mobile number via OTP verification", admin.getId());

        Map<String, Object> res = new HashMap<>();
        res.put("message", "Recovery mobile number verified and registered successfully!");
        res.put("phoneNumber", registeredMobile);
        return res;
    }

    public void clearCooldownsForTest() {
        requestCooldowns.clear();
    }
}
