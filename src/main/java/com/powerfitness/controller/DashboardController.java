package com.powerfitness.controller;

import com.powerfitness.entity.Member;
import com.powerfitness.entity.Payment;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.User;
import com.powerfitness.repository.MemberRepository;
import com.powerfitness.repository.PaymentRepository;
import com.powerfitness.service.AuthService;
import com.powerfitness.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@CrossOrigin(origins = "*")
public class DashboardController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private SubscriptionService subscriptionService;

    @GetMapping("/admin")
    public ResponseEntity<?> getAdminDashboard(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        // Refresh dynamic statuses first
        subscriptionService.refreshAllMemberStatuses();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalMembers", memberRepository.count());
        stats.put("totalActiveMembers", memberRepository.countActiveMembers());
        stats.put("totalExpiredMembers", memberRepository.countExpiredMembers());
        stats.put("membersExpiringSoon", memberRepository.countExpiringSoonMembers());
        stats.put("todayAdmissions", memberRepository.countTodayAdmissions(LocalDate.now()));
        stats.put("morningBatchMembers", memberRepository.countMorningBatch());
        stats.put("eveningBatchMembers", memberRepository.countEveningBatch());
        stats.put("cardioMembers", memberRepository.countCardioMembers());
        stats.put("strengthMembers", memberRepository.countStrengthMembers());

        double totalRevenue = paymentRepository.sumTotalRevenue();
        double subRevenue = paymentRepository.sumSubscriptionRevenue();
        double cardioRev = paymentRepository.sumCardioRevenue();

        stats.put("totalMonthlyRevenue", totalRevenue);
        stats.put("subscriptionRevenue", subRevenue);
        stats.put("cardioRevenue", cardioRev);

        // Recent Payments (last 6)
        List<Payment> allPayments = paymentRepository.findAllByOrderByPaymentDateDesc();
        List<Payment> recentPayments = allPayments.size() > 6 ? allPayments.subList(0, 6) : allPayments;
        stats.put("recentPayments", recentPayments);

        // Recent Admissions (last 6)
        List<Member> allMembers = memberRepository.findAll();
        allMembers.sort((a, b) -> b.getAdmissionDate().compareTo(a.getAdmissionDate()));
        List<Member> recentMembers = allMembers.size() > 6 ? allMembers.subList(0, 6) : allMembers;
        stats.put("recentAdmissions", recentMembers);

        // Plan distribution
        Map<String, Integer> planCounts = new HashMap<>();
        for (Member m : allMembers) {
            String plan = m.getSubscriptionPlan() != null ? m.getSubscriptionPlan() : "1 Month";
            planCounts.put(plan, planCounts.getOrDefault(plan, 0) + 1);
        }
        stats.put("planDistribution", planCounts);

        return ResponseEntity.ok(stats);
    }

    @GetMapping("/user")
    public ResponseEntity<?> getUserDashboard(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<Member> memberOpt = memberRepository.findByUserId(user.getId());
        if (memberOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Member record not found"));
        }

        Member member = memberOpt.get();
        subscriptionService.updateMemberSubscriptionStatus(member);
        memberRepository.save(member);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("memberId", member.getId());
        data.put("memberCode", member.getMemberCode());
        data.put("fullName", member.getFullName());
        data.put("phoneNumber", member.getPhoneNumber());
        data.put("photoUrl", member.getPhotoUrl());
        data.put("admissionDate", member.getAdmissionDate());
        data.put("startDate", member.getStartDate());
        data.put("expiryDate", member.getExpiryDate());
        data.put("subscriptionPlan", member.getSubscriptionPlan());
        data.put("trainingCategory", member.getTrainingCategory());
        data.put("batch", member.getBatch());
        data.put("hasCardio", member.isHasCardio());
        data.put("totalFee", member.getTotalFee());
        data.put("status", member.getStatus());

        long remaining = member.getDaysRemaining();
        long completed = member.getDaysCompleted();
        long total = member.getTotalDays();

        data.put("daysRemaining", remaining);
        data.put("daysCompleted", completed);
        data.put("totalDays", total);

        double progressPercent = total > 0 ? Math.min(100.0, Math.max(0.0, (completed * 100.0) / total)) : 0.0;
        data.put("progressPercent", Math.round(progressPercent * 10.0) / 10.0);
        data.put("statusBadgeText", member.getStatusBadgeText());

        // Countdown text
        if (remaining < 0) {
            data.put("countdownDisplay", "EXPIRED");
        } else {
            data.put("countdownDisplay", remaining + " DAYS LEFT");
        }

        // Recent personal payments
        List<Payment> payments = paymentRepository.findByMemberIdOrderByPaymentDateDesc(member.getId());
        data.put("payments", payments);

        return ResponseEntity.ok(data);
    }
}
