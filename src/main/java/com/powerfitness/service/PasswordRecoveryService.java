package com.powerfitness.service;

import com.powerfitness.entity.PasswordResetToken;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import com.powerfitness.repository.PasswordResetTokenRepository;
import com.powerfitness.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PasswordRecoveryService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private EmailService emailService;

    private static final SecureRandom secureRandom = new SecureRandom();
    private static final int TOKEN_EXPIRY_MINUTES = 15;
    private static final int RATE_LIMIT_SECONDS = 60;

    private final Map<String, LocalDateTime> requestCooldowns = new ConcurrentHashMap<>();

    private String generateSecureToken() {
        byte[] randomBytes = new byte[24];
        secureRandom.nextBytes(randomBytes);
        return HexFormat.of().formatHex(randomBytes);
    }

    @Transactional
    public Map<String, Object> requestPasswordReset(String usernameOrEmail, String originBaseUrl) {
        if (usernameOrEmail == null || usernameOrEmail.trim().isEmpty()) {
            throw new IllegalArgumentException("Username or email is required");
        }

        String username = usernameOrEmail.trim();

        // Rate limit check
        LocalDateTime lastReq = requestCooldowns.get(username.toLowerCase());
        if (lastReq != null && lastReq.plusSeconds(RATE_LIMIT_SECONDS).isAfter(LocalDateTime.now())) {
            long remaining = java.time.Duration.between(LocalDateTime.now(), lastReq.plusSeconds(RATE_LIMIT_SECONDS)).getSeconds();
            throw new IllegalArgumentException("Please wait " + Math.max(1, remaining) + " seconds before requesting another reset.");
        }

        requestCooldowns.put(username.toLowerCase(), LocalDateTime.now());

        Optional<User> userOpt = userRepository.findByUsername(username);

        // Only ADMIN accounts can use admin forgot-password flow
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            if (user.getRole() == Role.ADMIN && user.isEnabled()) {
                // Invalidate any existing unused tokens for this user
                List<PasswordResetToken> activeTokens = tokenRepository.findByUserAndUsedFalse(user);
                for (PasswordResetToken t : activeTokens) {
                    t.setUsed(true);
                    tokenRepository.save(t);
                }

                String tokenString = generateSecureToken();
                PasswordResetToken resetToken = new PasswordResetToken(
                    tokenString,
                    user,
                    LocalDateTime.now().plusMinutes(TOKEN_EXPIRY_MINUTES)
                );
                tokenRepository.save(resetToken);

                String resetUrl = (originBaseUrl != null ? originBaseUrl : "http://localhost:8080") + "/?resetToken=" + tokenString;
                emailService.sendPasswordResetNotification(user, tokenString, resetUrl);

                Map<String, Object> res = new HashMap<>();
                res.put("message", "If an administrator account with that username/email exists, password reset instructions have been generated.");
                res.put("expiresInMinutes", TOKEN_EXPIRY_MINUTES);
                res.put("emailConfigured", emailService.isConfigured());
                if (!emailService.isConfigured()) {
                    res.put("devResetToken", tokenString);
                }
                return res;
            }
        }

        // Generic safe response to prevent username enumeration
        Map<String, Object> genericRes = new HashMap<>();
        genericRes.put("message", "If an administrator account with that username/email exists, password reset instructions have been generated.");
        genericRes.put("expiresInMinutes", TOKEN_EXPIRY_MINUTES);
        genericRes.put("emailConfigured", emailService.isConfigured());
        return genericRes;
    }

    public boolean validateToken(String tokenString) {
        if (tokenString == null || tokenString.trim().isEmpty()) return false;
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenAndUsedFalse(tokenString.trim());
        return tokenOpt.isPresent() && tokenOpt.get().isValid();
    }

    @Transactional
    public void resetPassword(String tokenString, String newPassword, String confirmPassword) {
        if (tokenString == null || tokenString.trim().isEmpty()) {
            throw new IllegalArgumentException("Reset token is required");
        }
        if (newPassword == null || newPassword.length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters long");
        }
        if (confirmPassword == null || !newPassword.equals(confirmPassword)) {
            throw new IllegalArgumentException("Passwords do not match");
        }

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByTokenAndUsedFalse(tokenString.trim());
        if (tokenOpt.isEmpty() || !tokenOpt.get().isValid()) {
            throw new IllegalArgumentException("Invalid, expired, or previously used password reset token. Please request a new one.");
        }

        PasswordResetToken resetToken = tokenOpt.get();
        User user = resetToken.getUser();

        if (user == null || user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("Invalid password reset request");
        }

        user.setPassword(authService.hashPassword(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
    }
}
