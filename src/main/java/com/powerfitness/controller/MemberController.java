package com.powerfitness.controller;

import com.powerfitness.dto.MemberAdmissionRequest;
import com.powerfitness.dto.RenewalRequest;
import com.powerfitness.entity.*;
import com.powerfitness.repository.MemberRepository;
import com.powerfitness.repository.NotificationRepository;
import com.powerfitness.repository.PaymentRepository;
import com.powerfitness.repository.UserRepository;
import com.powerfitness.service.AuthService;
import com.powerfitness.service.PricingService;
import com.powerfitness.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class MemberController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private PricingService pricingService;

    @Autowired
    private SubscriptionService subscriptionService;

    // --- Admin: List with Search & Filters ---
    @GetMapping("/admin/members")
    public ResponseEntity<?> getMembers(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String batch,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        subscriptionService.refreshAllMemberStatuses();
        List<Member> list = memberRepository.findAll();

        // Filter by Query (Name, Phone, MemberCode)
        if (q != null && !q.trim().isEmpty()) {
            String query = q.trim().toLowerCase();
            list = list.stream().filter(m ->
                (m.getFullName() != null && m.getFullName().toLowerCase().contains(query)) ||
                (m.getPhoneNumber() != null && m.getPhoneNumber().contains(query)) ||
                (m.getMemberCode() != null && m.getMemberCode().toLowerCase().contains(query))
            ).collect(Collectors.toList());
        }

        // Filter by Batch
        if (batch != null && !batch.trim().isEmpty() && !"All".equalsIgnoreCase(batch)) {
            list = list.stream().filter(m -> batch.equalsIgnoreCase(m.getBatch())).collect(Collectors.toList());
        }

        // Filter by Category
        if (category != null && !category.trim().isEmpty() && !"All".equalsIgnoreCase(category)) {
            if ("Cardio".equalsIgnoreCase(category)) {
                list = list.stream().filter(m -> m.isHasCardio() || "Cardio".equalsIgnoreCase(m.getTrainingCategory())).collect(Collectors.toList());
            } else if ("Strength".equalsIgnoreCase(category) || "Strength Training".equalsIgnoreCase(category)) {
                list = list.stream().filter(m -> "Strength Training".equalsIgnoreCase(m.getTrainingCategory())).collect(Collectors.toList());
            }
        }

        // Filter by Status
        if (status != null && !status.trim().isEmpty() && !"All".equalsIgnoreCase(status)) {
            list = list.stream().filter(m -> status.equalsIgnoreCase(m.getStatus())).collect(Collectors.toList());
        }

        // Sort latest first
        list.sort((a, b) -> b.getId().compareTo(a.getId()));
        return ResponseEntity.ok(list);
    }

    // --- Admin: Admission (Add Member) ---
    @PostMapping("/admin/members")
    public ResponseEntity<?> addMember(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody MemberAdmissionRequest req) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        // Validation
        if (req.getFullName() == null || req.getFullName().trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Full Name is required"));
        }
        if (req.getPhoneNumber() == null || !req.getPhoneNumber().matches("^[0-9]{10}$")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Please provide a valid 10-digit Indian phone number"));
        }
        if (memberRepository.findByPhoneNumber(req.getPhoneNumber()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "A member with this phone number already exists"));
        }

        LocalDate admissionDate = req.getAdmissionDate() != null ? req.getAdmissionDate() : LocalDate.now();
        String plan = req.getSubscriptionPlan() != null ? req.getSubscriptionPlan() : "1 Month";
        String category = req.getTrainingCategory() != null ? req.getTrainingCategory() : "Strength Training";
        String batch = req.getBatch() != null ? req.getBatch() : "Morning Batch";
        boolean cardio = req.isCardioOption();

        // Calculate Pricing
        double baseFee = pricingService.getBasePlanPrice(plan);
        double cardioFee = cardio ? PricingService.CARDIO_FEE : 0.0;
        double totalFee = baseFee + cardioFee;

        // Calculate Expiry
        int durationDays = pricingService.getPlanDurationDays(plan);
        LocalDate expiryDate = admissionDate.plusDays(durationDays);

        // Generate Member Code
        long count = memberRepository.count() + 1001;
        String memberCode = "PFK-" + count;

        // Create or Link User
        String phone = req.getPhoneNumber().trim();
        User memberUser = userRepository.findByUsername(phone).orElseGet(() -> {
            User u = new User(phone, authService.hashPassword("user123"), req.getFullName(), Role.USER);
            return userRepository.save(u);
        });

        // Photo URL fallback
        String photo = (req.getPhotoUrl() != null && !req.getPhotoUrl().trim().isEmpty())
                ? req.getPhotoUrl().trim()
                : "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400&auto=format&fit=crop&q=80";

        Member member = new Member();
        member.setMemberCode(memberCode);
        member.setFullName(req.getFullName().trim());
        member.setPhoneNumber(phone);
        member.setPhotoUrl(photo);
        member.setAdmissionDate(admissionDate);
        member.setSubscriptionPlan(plan);
        member.setTrainingCategory(category);
        member.setBatch(batch);
        member.setHasCardio(cardio);
        member.setTotalFee(totalFee);
        member.setStartDate(admissionDate);
        member.setExpiryDate(expiryDate);
        member.setStatus("ACTIVE");
        member.setNotes(req.getNotes());
        member.setUser(memberUser);

        subscriptionService.updateMemberSubscriptionStatus(member);
        Member saved = memberRepository.save(member);

        // Record Initial Payment
        Payment p = new Payment();
        p.setReceiptNumber("REC-" + System.currentTimeMillis() % 10000000);
        p.setMember(saved);
        p.setBatch(saved.getBatch());
        p.setPaymentType("MEMBERSHIP");
        p.setAmount(totalFee);
        p.setBaseFee(baseFee);
        p.setCardioFee(cardioFee);
        p.setPaymentDate(admissionDate);
        p.setPaymentMethod(req.getPaymentMethod() != null ? req.getPaymentMethod() : "UPI");
        p.setSubscriptionPlan(plan);
        p.setPaymentStatus(req.getPaymentStatus() != null ? req.getPaymentStatus() : "Paid");
        p.setTransactionRef(req.getTransactionRef());
        p.setNotes("New Admission: " + plan + (cardio ? " + Cardio" : ""));
        paymentRepository.save(p);

        // Notifications
        notificationRepository.save(new Notification(null, "New Member Added", "Member " + saved.getFullName() + " (" + memberCode + ") enrolled successfully.", "NEW_MEMBER"));
        notificationRepository.save(new Notification(memberUser.getId(), "Welcome to PowerFitnessKurnool!", "Your membership is active until " + expiryDate + ". Let's crush your goals!", "WELCOME"));

        return ResponseEntity.ok(saved);
    }

    // --- Admin: Get Member Details ---
    @GetMapping("/admin/members/{id}")
    public ResponseEntity<?> getMemberDetail(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {
        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<Member> mOpt = memberRepository.findById(id);
        if (mOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Member member = mOpt.get();
        subscriptionService.updateMemberSubscriptionStatus(member);
        memberRepository.save(member);

        List<Payment> payments = paymentRepository.findByMemberIdOrderByPaymentDateDesc(member.getId());

        Map<String, Object> res = new HashMap<>();
        res.put("member", member);
        res.put("payments", payments);
        res.put("daysRemaining", member.getDaysRemaining());
        res.put("daysCompleted", member.getDaysCompleted());
        res.put("statusBadgeText", member.getStatusBadgeText());

        return ResponseEntity.ok(res);
    }

    // --- Admin: Edit Member ---
    @PutMapping("/admin/members/{id}")
    public ResponseEntity<?> updateMember(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody MemberAdmissionRequest req) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<Member> mOpt = memberRepository.findById(id);
        if (mOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Member m = mOpt.get();
        if (req.getFullName() != null) m.setFullName(req.getFullName().trim());
        if (req.getPhoneNumber() != null && req.getPhoneNumber().matches("^[0-9]{10}$")) {
            m.setPhoneNumber(req.getPhoneNumber().trim());
            if (m.getUser() != null) {
                m.getUser().setUsername(req.getPhoneNumber().trim());
                m.getUser().setFullName(m.getFullName());
                userRepository.save(m.getUser());
            }
        }
        if (req.getPhotoUrl() != null && !req.getPhotoUrl().trim().isEmpty()) {
            m.setPhotoUrl(req.getPhotoUrl().trim());
        }
        if (req.getTrainingCategory() != null) m.setTrainingCategory(req.getTrainingCategory());
        if (req.getBatch() != null) m.setBatch(req.getBatch());
        m.setHasCardio(req.isCardioOption());
        if (req.getNotes() != null) m.setNotes(req.getNotes());

        Member updated = memberRepository.save(m);
        return ResponseEntity.ok(updated);
    }

    // --- Admin: Delete Member ---
    @DeleteMapping("/admin/members/{id}")
    public ResponseEntity<?> deleteMember(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<Member> mOpt = memberRepository.findById(id);
        if (mOpt.isPresent()) {
            Member m = mOpt.get();
            // delete payments
            List<Payment> pList = paymentRepository.findByMemberIdOrderByPaymentDateDesc(m.getId());
            paymentRepository.deleteAll(pList);
            // delete member
            memberRepository.delete(m);
            return ResponseEntity.ok(Map.of("message", "Member deleted successfully"));
        }
        return ResponseEntity.notFound().build();
    }

    // --- Admin: Renew Member ---
    @PostMapping("/admin/members/{id}/renew")
    public ResponseEntity<?> renewMember(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody RenewalRequest req) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<Member> mOpt = memberRepository.findById(id);
        if (mOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String plan = req.getSubscriptionPlan() != null ? req.getSubscriptionPlan() : "1 Month";
        boolean cardio = req.isCardioOption();
        Member renewed = subscriptionService.renewSubscription(
                mOpt.get(),
                plan,
                cardio,
                req.getPaymentMethod(),
                req.getPaymentStatus(),
                req.getTransactionRef(),
                req.getNotes()
        );

        // Add notifications
        notificationRepository.save(new Notification(null, "Membership Renewed", "Member " + renewed.getFullName() + " renewed for " + plan + ".", "RENEWAL"));
        if (renewed.getUser() != null) {
            notificationRepository.save(new Notification(renewed.getUser().getId(), "Subscription Renewed! 🎉", "Your membership is renewed until " + renewed.getExpiryDate() + ".", "RENEWAL"));
        }

        return ResponseEntity.ok(renewed);
    }

    // --- User: Get Own Profile & Membership ---
    @GetMapping("/user/profile")
    public ResponseEntity<?> getUserProfile(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<Member> mOpt = memberRepository.findByUserId(user.getId());
        if (mOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Member record not found"));
        }

        Member member = mOpt.get();
        subscriptionService.updateMemberSubscriptionStatus(member);
        memberRepository.save(member);

        return ResponseEntity.ok(member);
    }
}
