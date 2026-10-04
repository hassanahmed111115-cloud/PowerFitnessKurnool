package com.powerfitness;

import com.powerfitness.controller.MemberController;
import com.powerfitness.dto.MemberAdmissionRequest;
import com.powerfitness.entity.Member;
import com.powerfitness.entity.Payment;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import com.powerfitness.repository.MemberRepository;
import com.powerfitness.repository.PaymentRepository;
import com.powerfitness.repository.UserRepository;
import com.powerfitness.service.AuthService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CustomMembershipPriceTest {

    static {
        System.setProperty("java.net.preferIPv6Addresses", "true");
    }

    @Autowired
    private MemberController memberController;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AuthService authService;

    private User adminUser;
    private String adminAuthHeader;
    private final List<Long> createdMemberIds = new ArrayList<>();
    private final List<Long> createdUserIds = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        // Ensure test admin exists
        String adminPhone = "9999000001";
        adminUser = userRepository.findByUsername(adminPhone).orElseGet(() -> {
            User u = new User(adminPhone, authService.hashPassword("adminPass123"), "Test Admin", Role.ADMIN);
            return userRepository.save(u);
        });

        // Generate session token for test admin
        String token = "TEST-ADMIN-" + UUID.randomUUID();
        @SuppressWarnings("unchecked")
        Map<String, Long> tokenToUserId = (Map<String, Long>) ReflectionTestUtils.getField(authService, "tokenToUserId");
        assertNotNull(tokenToUserId);
        tokenToUserId.put(token, adminUser.getId());
        adminAuthHeader = "Bearer " + token;
    }

    @AfterEach
    public void tearDown() {
        for (Long memberId : createdMemberIds) {
            try {
                List<Payment> payments = paymentRepository.findByMemberIdOrderByPaymentDateDesc(memberId);
                paymentRepository.deleteAll(payments);
                memberRepository.deleteById(memberId);
            } catch (Exception ignored) {}
        }
        for (Long uid : createdUserIds) {
            try {
                userRepository.deleteById(uid);
            } catch (Exception ignored) {}
        }
    }

    @Test
    public void testSuccessfulAdmissionWithCustomMembershipPrice() {
        String phone = "98881" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest req = new MemberAdmissionRequest();
        req.setFullName("John Custom 800");
        req.setPhoneNumber(phone);
        req.setSubscriptionPlan("1 Month");
        req.setTrainingCategory("Strength Training");
        req.setBatch("Morning Batch");
        req.setCardioOption(false);
        req.setCustomPrice(800.0);
        req.setPaymentMethod("UPI");
        req.setPaymentStatus("Paid");

        ResponseEntity<?> res = memberController.addMember(adminAuthHeader, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());

        Member member = (Member) res.getBody();
        assertNotNull(member);
        createdMemberIds.add(member.getId());
        if (member.getUser() != null) {
            createdUserIds.add(member.getUser().getId());
        }

        assertEquals(800.0, member.getTotalFee(), 0.001);
        assertEquals(800.0, member.getCustomPrice(), 0.001);

        // Verify payment record
        List<Payment> payments = paymentRepository.findByMemberIdOrderByPaymentDateDesc(member.getId());
        assertFalse(payments.isEmpty());
        Payment p = payments.get(0);
        assertEquals(800.0, p.getAmount(), 0.001);
        assertEquals(800.0, p.getBaseFee(), 0.001);
        assertEquals(0.0, p.getCardioFee(), 0.001);
        assertTrue(p.getNotes().contains("Custom Price: ₹800.0"));
    }

    @Test
    public void testAdmissionWithDecimalCustomPriceAndCardio() {
        String phone = "98882" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest req = new MemberAdmissionRequest();
        req.setFullName("Decimal Cardio Athlete");
        req.setPhoneNumber(phone);
        req.setSubscriptionPlan("3 Months");
        req.setTrainingCategory("Cardio");
        req.setBatch("Evening Batch");
        req.setCardioOption(true);
        req.setCustomPrice(1499.50);
        req.setPaymentMethod("Cash");
        req.setPaymentStatus("Paid");

        ResponseEntity<?> res = memberController.addMember(adminAuthHeader, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());

        Member member = (Member) res.getBody();
        assertNotNull(member);
        createdMemberIds.add(member.getId());
        if (member.getUser() != null) {
            createdUserIds.add(member.getUser().getId());
        }

        assertEquals(1499.50, member.getTotalFee(), 0.001);
        assertEquals(1499.50, member.getCustomPrice(), 0.001);

        // Verify payment record
        List<Payment> payments = paymentRepository.findByMemberIdOrderByPaymentDateDesc(member.getId());
        assertFalse(payments.isEmpty());
        Payment p = payments.get(0);
        assertEquals(1499.50, p.getAmount(), 0.001);
        assertEquals(500.0, p.getCardioFee(), 0.001);
        assertEquals(999.50, p.getBaseFee(), 0.001);
        assertEquals(p.getAmount(), p.getBaseFee() + p.getCardioFee(), 0.001);
    }

    @Test
    public void testRejectionWhenCustomPriceIsMissingOrEmpty() {
        String phone = "98883" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest req = new MemberAdmissionRequest();
        req.setFullName("No Price Member");
        req.setPhoneNumber(phone);
        req.setSubscriptionPlan("1 Month");
        req.setTrainingCategory("Strength Training");
        req.setBatch("Morning Batch");
        req.setCustomPrice(null); // Missing custom price

        ResponseEntity<?> res = memberController.addMember(adminAuthHeader, req);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) res.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").contains("Custom Membership Price (₹) is required"));
    }

    @Test
    public void testRejectionWhenCustomPriceIsZeroOrNegative() {
        String phoneZero = "98884" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest reqZero = new MemberAdmissionRequest();
        reqZero.setFullName("Zero Price Member");
        reqZero.setPhoneNumber(phoneZero);
        reqZero.setSubscriptionPlan("1 Month");
        reqZero.setCustomPrice(0.0); // Zero amount

        ResponseEntity<?> resZero = memberController.addMember(adminAuthHeader, reqZero);
        assertEquals(HttpStatus.BAD_REQUEST, resZero.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> bodyZero = (Map<String, String>) resZero.getBody();
        assertNotNull(bodyZero);
        assertTrue(bodyZero.get("error").contains("Custom Membership Price must be a valid positive amount"));

        String phoneNeg = "98885" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest reqNeg = new MemberAdmissionRequest();
        reqNeg.setFullName("Negative Price Member");
        reqNeg.setPhoneNumber(phoneNeg);
        reqNeg.setSubscriptionPlan("1 Month");
        reqNeg.setCustomPrice(-500.0); // Negative amount

        ResponseEntity<?> resNeg = memberController.addMember(adminAuthHeader, reqNeg);
        assertEquals(HttpStatus.BAD_REQUEST, resNeg.getStatusCode());
    }

    @Test
    public void testDifferentMembersSamePlanHaveDifferentCustomAmounts() {
        String phoneA = "98886" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest reqA = new MemberAdmissionRequest();
        reqA.setFullName("Member A 800");
        reqA.setPhoneNumber(phoneA);
        reqA.setSubscriptionPlan("1 Month");
        reqA.setTrainingCategory("Strength Training");
        reqA.setBatch("Morning Batch");
        reqA.setCustomPrice(800.0);

        ResponseEntity<?> resA = memberController.addMember(adminAuthHeader, reqA);
        assertEquals(HttpStatus.OK, resA.getStatusCode());
        Member memberA = (Member) resA.getBody();
        createdMemberIds.add(memberA.getId());
        if (memberA.getUser() != null) createdUserIds.add(memberA.getUser().getId());

        String phoneB = "98887" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest reqB = new MemberAdmissionRequest();
        reqB.setFullName("Member B 1200");
        reqB.setPhoneNumber(phoneB);
        reqB.setSubscriptionPlan("1 Month"); // Same plan!
        reqB.setTrainingCategory("Strength Training");
        reqB.setBatch("Morning Batch");
        reqB.setCustomPrice(1200.0); // Different custom amount!

        ResponseEntity<?> resB = memberController.addMember(adminAuthHeader, reqB);
        assertEquals(HttpStatus.OK, resB.getStatusCode());
        Member memberB = (Member) resB.getBody();
        createdMemberIds.add(memberB.getId());
        if (memberB.getUser() != null) createdUserIds.add(memberB.getUser().getId());

        // Verify independent pricing
        assertEquals(800.0, memberA.getTotalFee(), 0.001);
        assertEquals(1200.0, memberB.getTotalFee(), 0.001);
    }

    @Test
    public void testBackwardCompatibilityWhenCustomPriceIsNull() {
        Member legacyMember = new Member();
        legacyMember.setTotalFee(1800.0);
        legacyMember.setCustomPrice(null);

        // When customPrice is null, getCustomPrice() falls back to totalFee
        assertEquals(1800.0, legacyMember.getCustomPrice(), 0.001);
    }
}
