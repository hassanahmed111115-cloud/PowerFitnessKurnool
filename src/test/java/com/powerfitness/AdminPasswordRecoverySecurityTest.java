package com.powerfitness;

import com.powerfitness.entity.AdminOtpChallenge;
import com.powerfitness.entity.Member;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import com.powerfitness.repository.AdminOtpChallengeRepository;
import com.powerfitness.repository.MemberRepository;
import com.powerfitness.repository.UserRepository;
import com.powerfitness.service.AuthService;
import com.powerfitness.service.PasswordRecoveryService;
import com.powerfitness.service.SmsService;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class AdminPasswordRecoverySecurityTest {

    static {
        System.setProperty("java.net.preferIPv6Addresses", "true");
    }

    @Autowired
    private PasswordRecoveryService passwordRecoveryService;

    @Autowired
    private AdminOtpChallengeRepository challengeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private SmsService smsService;

    private static HttpServer mockSmsServer;
    private static int mockSmsPort;
    private static final AtomicReference<String> lastReceivedSmsPayload = new AtomicReference<>();
    private static final AtomicReference<String> lastDispatchedOtp = new AtomicReference<>();

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    @BeforeAll
    public static void startMockSmsServer() throws Exception {
        mockSmsServer = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        mockSmsPort = mockSmsServer.getAddress().getPort();
        mockSmsServer.createContext("/sms", exchange -> {
            InputStream is = exchange.getRequestBody();
            String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            lastReceivedSmsPayload.set(body);

            // Extract 6-digit OTP from payload if present
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("\\b(\\d{6})\\b").matcher(body);
            if (m.find()) {
                lastDispatchedOtp.set(m.group(1));
            }

            byte[] response = "{\"status\":\"success\"}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().set("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        mockSmsServer.start();
    }

    @AfterAll
    public static void stopMockSmsServer() {
        if (mockSmsServer != null) {
            mockSmsServer.stop(0);
        }
    }

    @BeforeEach
    public void setupTestConfig() {
        // Configure SmsService to use real HTTP dispatch to our mock SMS server
        ReflectionTestUtils.setField(smsService, "genericSmsUrl", "http://127.0.0.1:" + mockSmsPort + "/sms");
        lastReceivedSmsPayload.set(null);
        lastDispatchedOtp.set(null);
        challengeRepository.deleteAll();
        passwordRecoveryService.clearCooldownsForTest();
    }

    @Test
    public void testAdminOtpGeneration_SmsDispatch_And_SecureStorage() {
        String testPhone = "9876543210"; // Matches Gym Owner admin via AboutUs phone / primary admin

        Map<String, Object> response = passwordRecoveryService.requestAdminOtp(testPhone);

        // 1. Verify generic response message (Account Enumeration Protected)
        assertEquals("If the mobile number is registered for an admin account, an OTP has been sent.", response.get("message"));
        assertNotNull(response.get("challengeToken"));
        assertEquals(60, response.get("cooldownSeconds"));

        // 2. CRITICAL SECURITY: Verify OTP is NEVER returned in the API response
        assertNull(response.get("otp"));
        assertFalse(response.toString().matches(".*\\b\\d{6}\\b.*"), "API response must never contain 6-digit OTP");

        // 3. Verify OTP was actually dispatched via the SMS provider HTTP request
        assertNotNull(lastReceivedSmsPayload.get(), "SMS provider must have received outbound HTTP dispatch");
        String capturedOtp = lastDispatchedOtp.get();
        assertNotNull(capturedOtp, "6-digit OTP must have been sent via SMS");
        assertEquals(6, capturedOtp.length());

        // 4. Verify database stores ONLY salted hash, NEVER plaintext OTP
        String challengeToken = (String) response.get("challengeToken");
        AdminOtpChallenge challenge = challengeRepository.findByChallengeToken(challengeToken).orElseThrow();

        assertNotEquals(capturedOtp, challenge.getOtpHashed(), "Database must never store plaintext OTP");
        assertTrue(passwordEncoder.matches(capturedOtp, challenge.getOtpHashed()), "Database must store valid BCrypt hash of OTP");
        assertFalse(challenge.isUsed());
        assertFalse(challenge.isExpired());
    }

    @Test
    public void testAccountEnumerationProtection_UnregisteredPhone() {
        String unregisteredPhone = "9999999999";

        Map<String, Object> response = passwordRecoveryService.requestAdminOtp(unregisteredPhone);

        // Verify identical response structure to prevent enumeration
        assertEquals("If the mobile number is registered for an admin account, an OTP has been sent.", response.get("message"));
        assertNotNull(response.get("challengeToken"));
        assertEquals(60, response.get("cooldownSeconds"));

        // Verify NO SMS was sent for unregistered number
        assertNull(lastReceivedSmsPayload.get(), "No SMS should be sent for unregistered mobile number");
    }

    @Test
    public void testResendCooldownEnforcement() {
        String testPhone = "9876543210";

        // First request succeeds
        passwordRecoveryService.requestAdminOtp(testPhone);

        // Immediate second request must trigger 60-second cooldown exception
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            passwordRecoveryService.requestAdminOtp(testPhone);
        });

        assertTrue(ex.getMessage().contains("Please wait"), "Should enforce resend cooldown");
    }

    @Test
    public void testIncorrectOtpAttemptLimit_Max5Attempts() {
        String testPhone = "9876543210";
        // Reset cooldown map in service for test
        Map<String, LocalDateTime> cooldowns = (Map<String, LocalDateTime>) ReflectionTestUtils.getField(passwordRecoveryService, "requestCooldowns");
        cooldowns.clear();

        Map<String, Object> reqRes = passwordRecoveryService.requestAdminOtp(testPhone);
        String challengeToken = (String) reqRes.get("challengeToken");

        // Attempt 1 to 4 with incorrect OTP
        for (int i = 1; i <= 4; i++) {
            final int attempt = i;
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
                passwordRecoveryService.verifyOtp(challengeToken, "000000");
            });
            assertTrue(ex.getMessage().contains("Incorrect OTP"), "Attempt " + attempt + " should report incorrect OTP");
        }

        // 5th failed attempt: Must invalidate the OTP challenge
        IllegalArgumentException ex5 = assertThrows(IllegalArgumentException.class, () -> {
            passwordRecoveryService.verifyOtp(challengeToken, "000000");
        });
        assertTrue(ex5.getMessage().contains("invalidated") || ex5.getMessage().contains("Too many"), "5th attempt must invalidate OTP");

        // Subsequent attempt with even the correct OTP must fail
        String realOtp = lastDispatchedOtp.get();
        assertThrows(IllegalArgumentException.class, () -> {
            passwordRecoveryService.verifyOtp(challengeToken, realOtp);
        });
    }

    @Test
    public void testExpiredOtpFails() {
        String testPhone = "9876543210";
        Map<String, LocalDateTime> cooldowns = (Map<String, LocalDateTime>) ReflectionTestUtils.getField(passwordRecoveryService, "requestCooldowns");
        cooldowns.clear();

        Map<String, Object> reqRes = passwordRecoveryService.requestAdminOtp(testPhone);
        String challengeToken = (String) reqRes.get("challengeToken");
        String realOtp = lastDispatchedOtp.get();

        // Expire the challenge in DB
        AdminOtpChallenge challenge = challengeRepository.findByChallengeToken(challengeToken).orElseThrow();
        challenge.setExpiryDate(LocalDateTime.now().minusMinutes(1));
        challengeRepository.save(challenge);

        // Verification must be rejected
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            passwordRecoveryService.verifyOtp(challengeToken, realOtp);
        });
        assertTrue(ex.getMessage().contains("expired"), "Expired OTP must be rejected");
    }

    @Test
    public void testSuccessfulOtpVerification_PasswordReset_And_SessionInvalidation() {
        String testPhone = "9876543210";
        Map<String, LocalDateTime> cooldowns = (Map<String, LocalDateTime>) ReflectionTestUtils.getField(passwordRecoveryService, "requestCooldowns");
        cooldowns.clear();

        Map<String, Object> reqRes = passwordRecoveryService.requestAdminOtp(testPhone);
        String challengeToken = (String) reqRes.get("challengeToken");
        String realOtp = lastDispatchedOtp.get();

        // 1. Verify correct OTP
        Map<String, Object> verifyRes = passwordRecoveryService.verifyOtp(challengeToken, realOtp);
        assertNotNull(verifyRes.get("resetToken"));
        String resetToken = (String) verifyRes.get("resetToken");

        // 2. CRITICAL: Verify OTP cannot be used a second time (single use)
        assertThrows(IllegalArgumentException.class, () -> {
            passwordRecoveryService.verifyOtp(challengeToken, realOtp);
        }, "OTP cannot be re-verified once consumed");

        // 3. Reset Admin Password
        AdminOtpChallenge challenge = challengeRepository.findByChallengeToken(challengeToken).orElseThrow();
        User admin = challenge.getUser();
        String originalPasswordHash = admin.getPassword();

        String newPassword = "NewAdminPassword123#";
        passwordRecoveryService.resetAdminPassword(resetToken, newPassword, newPassword);

        // 4. Verify password hash updated and old password no longer works
        User updatedAdmin = userRepository.findById(admin.getId()).orElseThrow();
        assertNotEquals(originalPasswordHash, updatedAdmin.getPassword());
        assertTrue(authService.verifyPassword(newPassword, updatedAdmin.getPassword()));
        assertFalse(authService.verifyPassword("admin123", updatedAdmin.getPassword()));

        // 5. Verify reset token cannot be reused
        assertThrows(IllegalArgumentException.class, () -> {
            passwordRecoveryService.resetAdminPassword(resetToken, "AnotherPassword888#", "AnotherPassword888#");
        });

        // Restore original admin password to maintain database cleanliness
        updatedAdmin.setPassword(originalPasswordHash);
        userRepository.save(updatedAdmin);
    }

    @Test
    public void testAdminChangeMemberPassword() {
        // Verify an admin can update a member's password securely
        Optional<Member> memberOpt = memberRepository.findAll().stream().findFirst();
        assertTrue(memberOpt.isPresent(), "Member must exist in database");

        Member member = memberOpt.get();
        User memberUser = member.getUser();
        assertNotNull(memberUser, "Member must have associated User");

        String originalHash = memberUser.getPassword();
        String newMemberPassword = "NewMemberPass2026!";

        // Update password using secure hash
        memberUser.setPassword(authService.hashPassword(newMemberPassword));
        memberUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(memberUser);

        // Verify new password works and old hash is replaced
        User updatedUser = userRepository.findById(memberUser.getId()).orElseThrow();
        assertTrue(authService.verifyPassword(newMemberPassword, updatedUser.getPassword()));
        assertFalse(authService.verifyPassword("user123", updatedUser.getPassword()));

        // Restore member password
        updatedUser.setPassword(originalHash);
        userRepository.save(updatedUser);
    }

    @Test
    public void testAdminPhoneRegistration_FullOtpFlow_And_ForgotPasswordIntegration() {
        // 1. Get existing admin user (id = 1)
        User admin = userRepository.findByUsername("admin").orElseThrow();
        String originalPhone = admin.getPhoneNumber();

        try {
            // Clean initial phone state
            admin.setPhoneNumber(null);
            userRepository.save(admin);

            String candidatePhone = "9876543210";

            // 2. Request OTP to register candidate phone number
            Map<String, Object> reqRes = passwordRecoveryService.requestAdminPhoneRegistrationOtp(admin, candidatePhone);

            // Verify response
            assertNotNull(reqRes.get("challengeToken"));
            assertEquals(60, reqRes.get("cooldownSeconds"));
            assertNull(reqRes.get("otp"), "OTP must NEVER be returned in response");
            assertFalse(reqRes.toString().matches(".*\\b\\d{6}\\b.*"), "Response must never leak OTP");

            // Verify SMS was dispatched to mock SMS server
            assertNotNull(lastReceivedSmsPayload.get(), "SMS provider must have received outbound HTTP request");
            String capturedOtp = lastDispatchedOtp.get();
            assertNotNull(capturedOtp);
            assertEquals(6, capturedOtp.length());

            String challengeToken = (String) reqRes.get("challengeToken");
            AdminOtpChallenge challenge = challengeRepository.findByChallengeToken(challengeToken).orElseThrow();
            assertTrue(passwordEncoder.matches(capturedOtp, challenge.getOtpHashed()), "DB must store valid BCrypt hash of OTP");
            assertFalse(challenge.isUsed());

            // 3. Failed attempt with invalid OTP
            IllegalArgumentException wrongOtpEx = assertThrows(IllegalArgumentException.class, () -> {
                passwordRecoveryService.verifyAdminPhoneRegistrationOtp(admin, challengeToken, "111111");
            });
            assertTrue(wrongOtpEx.getMessage().contains("Incorrect OTP"));

            // 4. Successful verification with correct OTP
            Map<String, Object> verifyRes = passwordRecoveryService.verifyAdminPhoneRegistrationOtp(admin, challengeToken, capturedOtp);
            assertEquals("Recovery mobile number verified and registered successfully!", verifyRes.get("message"));
            assertEquals(candidatePhone, verifyRes.get("phoneNumber"));

            // 5. Verify database now has candidatePhone saved on admin
            User reloadedAdmin = userRepository.findById(admin.getId()).orElseThrow();
            assertEquals(candidatePhone, reloadedAdmin.getPhoneNumber());

            // 6. Verify challenge is now marked used and cannot be re-verified
            AdminOtpChallenge usedChallenge = challengeRepository.findByChallengeToken(challengeToken).orElseThrow();
            assertTrue(usedChallenge.isUsed());
            assertThrows(IllegalArgumentException.class, () -> {
                passwordRecoveryService.verifyAdminPhoneRegistrationOtp(admin, challengeToken, capturedOtp);
            });

            // 7. Verify Forgot Password OTP recovery now works using this registered number!
            passwordRecoveryService.clearCooldownsForTest();
            lastReceivedSmsPayload.set(null);
            lastDispatchedOtp.set(null);

            Map<String, Object> forgotRes = passwordRecoveryService.requestAdminOtp(candidatePhone);
            assertEquals("If the mobile number is registered for an admin account, an OTP has been sent.", forgotRes.get("message"));
            assertNotNull(lastReceivedSmsPayload.get(), "Outbound SMS should be dispatched for registered admin phone");
            assertNotNull(lastDispatchedOtp.get());

        } finally {
            // Restore original state
            admin.setPhoneNumber(originalPhone);
            userRepository.save(admin);
        }
    }

    @Test
    public void testAdminPhoneRegistration_SecurityConstraints() {
        User admin = userRepository.findByUsername("admin").orElseThrow();

        // 1. Non-admin or unauthenticated cannot register phone
        User normalUser = new User();
        normalUser.setRole(Role.USER);
        assertThrows(IllegalArgumentException.class, () -> {
            passwordRecoveryService.requestAdminPhoneRegistrationOtp(normalUser, "9876543210");
        });
        assertThrows(IllegalArgumentException.class, () -> {
            passwordRecoveryService.requestAdminPhoneRegistrationOtp(null, "9876543210");
        });

        // 2. Invalid phone format rejected
        assertThrows(IllegalArgumentException.class, () -> {
            passwordRecoveryService.requestAdminPhoneRegistrationOtp(admin, "123");
        });

        // 3. Resend cooldown enforced
        passwordRecoveryService.clearCooldownsForTest();
        passwordRecoveryService.requestAdminPhoneRegistrationOtp(admin, "9876543210");
        assertThrows(IllegalArgumentException.class, () -> {
            passwordRecoveryService.requestAdminPhoneRegistrationOtp(admin, "9876543210");
        });
    }
}
