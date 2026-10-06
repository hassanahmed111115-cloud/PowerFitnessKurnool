package com.powerfitness;

import com.powerfitness.controller.FileServeController;
import com.powerfitness.controller.MemberController;
import com.powerfitness.dto.MemberAdmissionRequest;
import com.powerfitness.entity.Member;
import com.powerfitness.entity.Payment;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.StoredFile;
import com.powerfitness.entity.User;
import com.powerfitness.repository.MemberRepository;
import com.powerfitness.repository.PaymentRepository;
import com.powerfitness.repository.StoredFileRepository;
import com.powerfitness.repository.UserRepository;
import com.powerfitness.service.AuthService;
import com.powerfitness.service.FileStorageService;
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
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CustomAdmissionAndPermanentStorageTest {

    static {
        System.setProperty("java.net.preferIPv6Addresses", "true");
    }

    @Autowired
    private MemberController memberController;

    @Autowired
    private FileServeController fileServeController;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private StoredFileRepository storedFileRepository;

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
    private final List<String> createdFileKeys = new ArrayList<>();

    @BeforeEach
    public void setUp() {
        String adminPhone = "9999000002";
        adminUser = userRepository.findByUsername(adminPhone).orElseGet(() -> {
            User u = new User(adminPhone, authService.hashPassword("adminPass123"), "Test Admin Storage", Role.ADMIN);
            return userRepository.save(u);
        });

        String token = "TEST-ADMIN-STORAGE-" + UUID.randomUUID();
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
        for (String key : createdFileKeys) {
            try {
                fileStorageService.deleteFile(key);
            } catch (Exception ignored) {}
        }
    }

    // --- CHANGE 1 TESTS: Custom Admission Date + Custom Duration Months ---

    @Test
    public void testCustomAdmissionDateAndDurationMonthsEndCalculation() {
        String phone = "97771" + (int)(10000 + Math.random() * 89999);
        LocalDate customAdmissionDate = LocalDate.of(2026, 10, 10);
        int durationMonths = 3;

        MemberAdmissionRequest req = new MemberAdmissionRequest();
        req.setFullName("Rahul Custom Date");
        req.setPhoneNumber(phone);
        req.setAdmissionDate(customAdmissionDate);
        req.setDurationMonths(durationMonths);
        req.setSubscriptionPlan("3 Months");
        req.setTrainingCategory("Strength Training");
        req.setBatch("Morning Batch");
        req.setCustomPrice(1800.0);
        req.setPaymentMethod("UPI");
        req.setPaymentStatus("Paid");

        ResponseEntity<?> res = memberController.addMember(adminAuthHeader, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());

        Member member = (Member) res.getBody();
        assertNotNull(member);
        createdMemberIds.add(member.getId());
        if (member.getUser() != null) createdUserIds.add(member.getUser().getId());

        assertEquals(customAdmissionDate, member.getAdmissionDate());
        assertEquals(customAdmissionDate, member.getStartDate());
        assertEquals(durationMonths, member.getDurationMonths());
        // Expiry date must strictly be Admission Date + 3 Months = 2027-01-10
        LocalDate expectedExpiry = customAdmissionDate.plusMonths(durationMonths);
        assertEquals(expectedExpiry, member.getExpiryDate());
        assertEquals(LocalDate.of(2027, 1, 10), member.getExpiryDate());
    }

    @Test
    public void testAdmissionDateDifferentFromTodayCalculatesExpiryCorrectly() {
        String phone = "97772" + (int)(10000 + Math.random() * 89999);
        // Custom date in the past: 1 October 2026
        LocalDate pastAdmissionDate = LocalDate.of(2026, 10, 1);
        int durationMonths = 6;

        MemberAdmissionRequest req = new MemberAdmissionRequest();
        req.setFullName("Past Date Athlete");
        req.setPhoneNumber(phone);
        req.setAdmissionDate(pastAdmissionDate);
        req.setDurationMonths(durationMonths);
        req.setSubscriptionPlan("6 Months");
        req.setTrainingCategory("Strength Training");
        req.setBatch("Evening Batch");
        req.setCustomPrice(3500.0);
        req.setPaymentMethod("Cash");
        req.setPaymentStatus("Paid");

        ResponseEntity<?> res = memberController.addMember(adminAuthHeader, req);
        assertEquals(HttpStatus.OK, res.getStatusCode());

        Member member = (Member) res.getBody();
        assertNotNull(member);
        createdMemberIds.add(member.getId());
        if (member.getUser() != null) createdUserIds.add(member.getUser().getId());

        assertEquals(pastAdmissionDate, member.getAdmissionDate());
        assertEquals(LocalDate.of(2027, 4, 1), member.getExpiryDate());
    }

    @Test
    public void testRejectionWhenAdmissionDateIsMissing() {
        String phone = "97773" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest req = new MemberAdmissionRequest();
        req.setFullName("Missing Date Athlete");
        req.setPhoneNumber(phone);
        req.setAdmissionDate(null); // Missing admission date
        req.setDurationMonths(1);
        req.setCustomPrice(800.0);

        ResponseEntity<?> res = memberController.addMember(adminAuthHeader, req);
        assertEquals(HttpStatus.BAD_REQUEST, res.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) res.getBody();
        assertNotNull(body);
        assertTrue(body.get("error").contains("Admission Date / Start Date is required"));
    }

    @Test
    public void testRejectionWhenDurationMonthsIsZeroOrNegative() {
        String phoneZero = "97774" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest reqZero = new MemberAdmissionRequest();
        reqZero.setFullName("Zero Duration Athlete");
        reqZero.setPhoneNumber(phoneZero);
        reqZero.setAdmissionDate(LocalDate.now());
        reqZero.setDurationMonths(0); // Zero months
        reqZero.setCustomPrice(800.0);

        ResponseEntity<?> resZero = memberController.addMember(adminAuthHeader, reqZero);
        assertEquals(HttpStatus.BAD_REQUEST, resZero.getStatusCode());
        @SuppressWarnings("unchecked")
        Map<String, String> bodyZero = (Map<String, String>) resZero.getBody();
        assertNotNull(bodyZero);
        assertTrue(bodyZero.get("error").contains("Membership Duration (Months) must be a valid positive number"));

        String phoneNeg = "97775" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest reqNeg = new MemberAdmissionRequest();
        reqNeg.setFullName("Negative Duration Athlete");
        reqNeg.setPhoneNumber(phoneNeg);
        reqNeg.setAdmissionDate(LocalDate.now());
        reqNeg.setDurationMonths(-2); // Negative months
        reqNeg.setCustomPrice(800.0);

        ResponseEntity<?> resNeg = memberController.addMember(adminAuthHeader, reqNeg);
        assertEquals(HttpStatus.BAD_REQUEST, resNeg.getStatusCode());
    }

    @Test
    public void testAdminEditMemberAdmissionDatePersistsCorrectly() {
        String phone = "97776" + (int)(10000 + Math.random() * 89999);
        LocalDate initialAdmissionDate = LocalDate.of(2026, 10, 5);
        int durationMonths = 1;

        MemberAdmissionRequest createReq = new MemberAdmissionRequest();
        createReq.setFullName("Suresh Redit Date");
        createReq.setPhoneNumber(phone);
        createReq.setAdmissionDate(initialAdmissionDate);
        createReq.setDurationMonths(durationMonths);
        createReq.setSubscriptionPlan("1 Month");
        createReq.setTrainingCategory("Strength Training");
        createReq.setBatch("Morning Batch");
        createReq.setCustomPrice(800.0);
        createReq.setPaymentMethod("UPI");
        createReq.setPaymentStatus("Paid");

        ResponseEntity<?> createRes = memberController.addMember(adminAuthHeader, createReq);
        assertEquals(HttpStatus.OK, createRes.getStatusCode());
        Member member = (Member) createRes.getBody();
        assertNotNull(member);
        createdMemberIds.add(member.getId());
        if (member.getUser() != null) createdUserIds.add(member.getUser().getId());

        assertEquals(initialAdmissionDate, member.getAdmissionDate());
        LocalDate originalExpiry = member.getExpiryDate();
        assertEquals(LocalDate.of(2026, 11, 5), originalExpiry);

        // Edit Member: Change admission date from 05/10/2026 to 15/09/2026 (day and month changed)
        LocalDate newAdmissionDate = LocalDate.of(2026, 9, 15);
        MemberAdmissionRequest editReq = new MemberAdmissionRequest();
        editReq.setAdmissionDate(newAdmissionDate);
        editReq.setFullName("Suresh Redit Date Updated");

        ResponseEntity<?> editRes = memberController.updateMember(adminAuthHeader, member.getId(), editReq);
        assertEquals(HttpStatus.OK, editRes.getStatusCode());

        Member updatedFromApi = (Member) editRes.getBody();
        assertNotNull(updatedFromApi);
        assertEquals(newAdmissionDate, updatedFromApi.getAdmissionDate());
        assertEquals(newAdmissionDate, updatedFromApi.getStartDate());

        // Verify direct reload from database
        Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
        assertEquals(newAdmissionDate, reloaded.getAdmissionDate());
        assertEquals(newAdmissionDate, reloaded.getStartDate());

        // Verify other fields, prices, and payments remain untouched
        assertEquals(800.0, reloaded.getCustomPrice(), 0.001);
        assertEquals(800.0, reloaded.getTotalFee(), 0.001);
        // CHANGE 2: Expiry date is automatically recalculated based on New Start Date (2026-09-15) + Existing Duration (1 month) = 2026-10-15
        LocalDate expectedRecalculatedExpiry = LocalDate.of(2026, 10, 15);
        assertEquals(expectedRecalculatedExpiry, reloaded.getExpiryDate(), "Expiry date must be automatically recalculated on start date change");

        List<Payment> payments = paymentRepository.findByMemberIdOrderByPaymentDateDesc(member.getId());
        assertEquals(1, payments.size());
        assertEquals(800.0, payments.get(0).getAmount(), 0.001);
    }

    @Test
    public void testAdminEditMemberAdmissionDateWithYearChange() {
        String phone = "97777" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest createReq = new MemberAdmissionRequest();
        createReq.setFullName("Year Change Athlete");
        createReq.setPhoneNumber(phone);
        createReq.setAdmissionDate(LocalDate.of(2026, 10, 10));
        createReq.setDurationMonths(3);
        createReq.setSubscriptionPlan("3 Months");
        createReq.setCustomPrice(1500.0);

        ResponseEntity<?> createRes = memberController.addMember(adminAuthHeader, createReq);
        assertEquals(HttpStatus.OK, createRes.getStatusCode());
        Member member = (Member) createRes.getBody();
        assertNotNull(member);
        createdMemberIds.add(member.getId());
        if (member.getUser() != null) createdUserIds.add(member.getUser().getId());

        // Change complete date: Day, Month, and Year (e.g. 2025-08-20)
        LocalDate newDateWithPastYear = LocalDate.of(2025, 8, 20);
        MemberAdmissionRequest editReq = new MemberAdmissionRequest();
        editReq.setAdmissionDate(newDateWithPastYear);

        ResponseEntity<?> editRes = memberController.updateMember(adminAuthHeader, member.getId(), editReq);
        assertEquals(HttpStatus.OK, editRes.getStatusCode());

        Member reloaded = memberRepository.findById(member.getId()).orElseThrow();
        assertEquals(newDateWithPastYear, reloaded.getAdmissionDate());
        assertEquals(newDateWithPastYear, reloaded.getStartDate());
        // CHANGE 2: Expiry date recalculated from 2025-08-20 + 3 months = 2025-11-20
        assertEquals(LocalDate.of(2025, 11, 20), reloaded.getExpiryDate());
    }

    @Test
    public void testPromptExampleAutoExpiryRecalculation3Months() {
        // Prompt Example: Start Date: 10/10/2026, Duration: 3 months -> changes Start Date to: 15/09/2026 -> new Expiry Date: 15/12/2026
        String phone = "97781" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest createReq = new MemberAdmissionRequest();
        createReq.setFullName("Three Month Example Athlete");
        createReq.setPhoneNumber(phone);
        createReq.setAdmissionDate(LocalDate.of(2026, 10, 10));
        createReq.setDurationMonths(3);
        createReq.setSubscriptionPlan("3 Months");
        createReq.setCustomPrice(2400.0);

        ResponseEntity<?> createRes = memberController.addMember(adminAuthHeader, createReq);
        assertEquals(HttpStatus.OK, createRes.getStatusCode());
        Member member = (Member) createRes.getBody();
        createdMemberIds.add(member.getId());
        if (member.getUser() != null) createdUserIds.add(member.getUser().getId());

        assertEquals(LocalDate.of(2026, 10, 10), member.getStartDate());
        assertEquals(LocalDate.of(2027, 1, 10), member.getExpiryDate());

        // Admin changes Start Date to 15/09/2026
        LocalDate newStartDate = LocalDate.of(2026, 9, 15);
        MemberAdmissionRequest editReq = new MemberAdmissionRequest();
        editReq.setAdmissionDate(newStartDate);

        ResponseEntity<?> editRes = memberController.updateMember(adminAuthHeader, member.getId(), editReq);
        assertEquals(HttpStatus.OK, editRes.getStatusCode());
        Member updated = (Member) editRes.getBody();
        assertNotNull(updated);

        // Verification of prompt example: New Start Date (15/09/2026) + Existing Duration (3 months) = 15/12/2026
        LocalDate expectedExpiry = LocalDate.of(2026, 12, 15);
        assertEquals(expectedExpiry, updated.getExpiryDate());

        Member dbMember = memberRepository.findById(member.getId()).orElseThrow();
        assertEquals(expectedExpiry, dbMember.getExpiryDate());
        assertEquals(newStartDate, dbMember.getAdmissionDate());
        assertEquals(3, dbMember.getDurationMonths());
    }

    @Test
    public void testAutoExpiryRecalculation6MonthsAnd12Months() {
        // Test 6-Month duration
        String phone6 = "97782" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest req6 = new MemberAdmissionRequest();
        req6.setFullName("Six Month Athlete");
        req6.setPhoneNumber(phone6);
        req6.setAdmissionDate(LocalDate.of(2026, 4, 1));
        req6.setDurationMonths(6);
        req6.setSubscriptionPlan("6 Months");
        req6.setCustomPrice(4500.0);

        ResponseEntity<?> res6 = memberController.addMember(adminAuthHeader, req6);
        Member member6 = (Member) res6.getBody();
        createdMemberIds.add(member6.getId());
        if (member6.getUser() != null) createdUserIds.add(member6.getUser().getId());

        // Change start date to 2026-05-10 -> Expiry must be 2026-05-10 + 6 months = 2026-11-10
        MemberAdmissionRequest edit6 = new MemberAdmissionRequest();
        edit6.setAdmissionDate(LocalDate.of(2026, 5, 10));
        ResponseEntity<?> editRes6 = memberController.updateMember(adminAuthHeader, member6.getId(), edit6);
        Member updated6 = (Member) editRes6.getBody();
        assertEquals(LocalDate.of(2026, 11, 10), updated6.getExpiryDate());

        // Test 12-Month duration
        String phone12 = "97783" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest req12 = new MemberAdmissionRequest();
        req12.setFullName("Annual Athlete");
        req12.setPhoneNumber(phone12);
        req12.setAdmissionDate(LocalDate.of(2026, 1, 15));
        req12.setDurationMonths(12);
        req12.setSubscriptionPlan("1 Year");
        req12.setCustomPrice(8000.0);

        ResponseEntity<?> res12 = memberController.addMember(adminAuthHeader, req12);
        Member member12 = (Member) res12.getBody();
        createdMemberIds.add(member12.getId());
        if (member12.getUser() != null) createdUserIds.add(member12.getUser().getId());

        // Change start date to 2026-03-01 -> Expiry must be 2026-03-01 + 12 months = 2027-03-01
        MemberAdmissionRequest edit12 = new MemberAdmissionRequest();
        edit12.setAdmissionDate(LocalDate.of(2026, 3, 1));
        ResponseEntity<?> editRes12 = memberController.updateMember(adminAuthHeader, member12.getId(), edit12);
        Member updated12 = (Member) editRes12.getBody();
        assertEquals(LocalDate.of(2027, 3, 1), updated12.getExpiryDate());
    }

    @Test
    public void testMonthEndDateClampingOnExpiryCalculation() {
        // Start date on Jan 31 + 1 month duration -> Must produce Feb 28 (safe month-end)
        String phone = "97784" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest req = new MemberAdmissionRequest();
        req.setFullName("Month End Athlete");
        req.setPhoneNumber(phone);
        req.setAdmissionDate(LocalDate.of(2026, 1, 1));
        req.setDurationMonths(1);
        req.setSubscriptionPlan("1 Month");
        req.setCustomPrice(800.0);

        ResponseEntity<?> res = memberController.addMember(adminAuthHeader, req);
        Member member = (Member) res.getBody();
        createdMemberIds.add(member.getId());
        if (member.getUser() != null) createdUserIds.add(member.getUser().getId());

        // Admin changes start date to January 31, 2026
        MemberAdmissionRequest editReq = new MemberAdmissionRequest();
        editReq.setAdmissionDate(LocalDate.of(2026, 1, 31));

        ResponseEntity<?> editRes = memberController.updateMember(adminAuthHeader, member.getId(), editReq);
        assertEquals(HttpStatus.OK, editRes.getStatusCode());
        Member updated = (Member) editRes.getBody();

        // 2026-01-31 + 1 month = 2026-02-28 (2026 is non-leap year)
        assertEquals(LocalDate.of(2026, 2, 28), updated.getExpiryDate());

        // Test March 31 + 1 month -> 2026-04-30
        editReq.setAdmissionDate(LocalDate.of(2026, 3, 31));
        ResponseEntity<?> editResMar = memberController.updateMember(adminAuthHeader, member.getId(), editReq);
        Member updatedMar = (Member) editResMar.getBody();
        assertEquals(LocalDate.of(2026, 4, 30), updatedMar.getExpiryDate());
    }

    @Test
    public void testMemberEditPhotoReplacementAndRemoval() throws Exception {
        byte[] png1 = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
        };
        String base64Image1 = "data:image/png;base64," + Base64.getEncoder().encodeToString(png1);
        String initialPhotoUrl = fileStorageService.saveMemberPhotoBase64(base64Image1);
        createdFileKeys.add(fileStorageService.extractFilename(initialPhotoUrl));

        String phone = "97785" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest createReq = new MemberAdmissionRequest();
        createReq.setFullName("Photo Edit Athlete");
        createReq.setPhoneNumber(phone);
        createReq.setPhotoUrl(initialPhotoUrl);
        createReq.setAdmissionDate(LocalDate.of(2026, 10, 10));
        createReq.setDurationMonths(1);
        createReq.setCustomPrice(800.0);

        ResponseEntity<?> createRes = memberController.addMember(adminAuthHeader, createReq);
        Member member = (Member) createRes.getBody();
        createdMemberIds.add(member.getId());
        if (member.getUser() != null) createdUserIds.add(member.getUser().getId());

        assertEquals(initialPhotoUrl, member.getPhotoUrl());

        // 1. Admin selects a new photo: save new photo first
        byte[] png2 = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x02, 0x00, 0x00, 0x00, 0x02,
            0x08, 0x06, 0x00, 0x00, 0x00, (byte) 0xF4, 0x78, (byte) 0xD4, (byte) 0xFA
        };
        String base64Image2 = "data:image/png;base64," + Base64.getEncoder().encodeToString(png2);
        String newPhotoUrl = fileStorageService.saveMemberPhotoBase64(base64Image2);
        createdFileKeys.add(fileStorageService.extractFilename(newPhotoUrl));

        // 2. Admin saves member with new photo
        MemberAdmissionRequest editReq = new MemberAdmissionRequest();
        editReq.setPhotoUrl(newPhotoUrl);
        ResponseEntity<?> editRes = memberController.updateMember(adminAuthHeader, member.getId(), editReq);
        assertEquals(HttpStatus.OK, editRes.getStatusCode());
        Member updated = (Member) editRes.getBody();
        assertEquals(newPhotoUrl, updated.getPhotoUrl());

        // Verify old photo was cleanly deleted from storage
        String oldFilename = fileStorageService.extractFilename(initialPhotoUrl);
        assertTrue(storedFileRepository.findByFileKey(oldFilename).isEmpty(), "Old photo must be deleted when replaced");

        // Verify new photo is present in storage
        String newFilename = fileStorageService.extractFilename(newPhotoUrl);
        assertTrue(storedFileRepository.findByFileKey(newFilename).isPresent(), "New photo must be preserved in storage");

        // 3. Admin removes member photo
        MemberAdmissionRequest removePhotoReq = new MemberAdmissionRequest();
        removePhotoReq.setPhotoUrl("REMOVE");
        ResponseEntity<?> removeRes = memberController.updateMember(adminAuthHeader, member.getId(), removePhotoReq);
        assertEquals(HttpStatus.OK, removeRes.getStatusCode());
        Member removedPhotoMember = (Member) removeRes.getBody();
        assertTrue(removedPhotoMember.getPhotoUrl().contains("unsplash.com"), "Member photo must reset to default placeholder");
        assertTrue(storedFileRepository.findByFileKey(newFilename).isEmpty(), "Custom photo must be deleted on removal");
    }

    @Test
    public void testRejectInvalidAdmissionDateRangeOnEdit() {
        String phone = "97778" + (int)(10000 + Math.random() * 89999);
        MemberAdmissionRequest createReq = new MemberAdmissionRequest();
        createReq.setFullName("Range Check Athlete");
        createReq.setPhoneNumber(phone);
        createReq.setAdmissionDate(LocalDate.of(2026, 10, 10));
        createReq.setDurationMonths(1);
        createReq.setCustomPrice(800.0);

        ResponseEntity<?> createRes = memberController.addMember(adminAuthHeader, createReq);
        assertEquals(HttpStatus.OK, createRes.getStatusCode());
        Member member = (Member) createRes.getBody();
        createdMemberIds.add(member.getId());
        if (member.getUser() != null) createdUserIds.add(member.getUser().getId());

        // Send absurd year e.g. year 1800
        MemberAdmissionRequest editReq = new MemberAdmissionRequest();
        editReq.setAdmissionDate(LocalDate.of(1800, 1, 1));

        ResponseEntity<?> editRes = memberController.updateMember(adminAuthHeader, member.getId(), editReq);
        assertEquals(HttpStatus.BAD_REQUEST, editRes.getStatusCode());
    }

    // --- CHANGE 2 TESTS: Permanent Storage Architecture ---

    @Test
    public void testPermanentPhotoUploadServingAndDeletion() throws Exception {
        // Valid 1x1 PNG image with standard PNG magic header (89 50 4E 47 0D 0A 1A 0A)
        byte[] validPngBytes = new byte[] {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52,
            0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00, 0x01,
            0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4, (byte) 0x89
        };
        String base64Image = "data:image/png;base64," + Base64.getEncoder().encodeToString(validPngBytes);

        // 1. Upload & persist photo
        String publicUrl = fileStorageService.saveMemberPhotoBase64(base64Image);
        assertNotNull(publicUrl);
        assertTrue(publicUrl.startsWith("/api/files/members/"));

        String filename = fileStorageService.extractFilename(publicUrl);
        assertNotNull(filename);
        createdFileKeys.add(filename);

        // 2. Verify stored in Supabase PostgreSQL stored_files table
        Optional<StoredFile> storedOpt = storedFileRepository.findByFileKey(filename);
        assertTrue(storedOpt.isPresent(), "Photo must be permanently stored in PostgreSQL stored_files table");
        StoredFile stored = storedOpt.get();
        assertEquals("members", stored.getCategory());
        assertEquals("image/png", stored.getContentType());
        assertNotNull(stored.getData());
        assertTrue(stored.getData().length > 0);

        // 3. Serve through permanent endpoint
        ResponseEntity<?> serveRes = fileServeController.serveFile("members", filename);
        assertEquals(HttpStatus.OK, serveRes.getStatusCode());
        assertEquals("image/png", serveRes.getHeaders().getContentType().toString());
        assertNotNull(serveRes.getBody());

        // 4. Serve through legacy /uploads path (backward compatibility)
        ResponseEntity<?> legacyRes = fileServeController.serveLegacyUpload("members", filename);
        assertEquals(HttpStatus.OK, legacyRes.getStatusCode());
        assertEquals("image/png", legacyRes.getHeaders().getContentType().toString());

        // 5. Delete file
        fileStorageService.deleteFile(publicUrl);
        Optional<StoredFile> deletedOpt = storedFileRepository.findByFileKey(filename);
        assertTrue(deletedOpt.isEmpty(), "Photo must be removed from PostgreSQL stored_files upon deletion");
    }

    @Test
    public void testRejectInvalidCorruptedImageFile() {
        // Non-image plain text data pretending to be an image
        byte[] textBytes = "This is not an image file!".getBytes();
        String fakeBase64 = "data:image/png;base64," + Base64.getEncoder().encodeToString(textBytes);

        assertThrows(IllegalArgumentException.class, () -> {
            fileStorageService.saveMemberPhotoBase64(fakeBase64);
        });
    }
}
