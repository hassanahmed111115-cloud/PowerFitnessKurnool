package com.powerfitness.controller;

import com.lowagie.text.DocumentException;
import com.powerfitness.entity.Member;
import com.powerfitness.entity.Payment;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.Supplement;
import com.powerfitness.entity.User;
import com.powerfitness.repository.MemberRepository;
import com.powerfitness.repository.PaymentRepository;
import com.powerfitness.repository.SupplementRepository;
import com.powerfitness.service.AuthService;
import com.powerfitness.service.PdfReportService;
import com.powerfitness.service.SubscriptionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/admin/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private SupplementRepository supplementRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private PdfReportService pdfReportService;

    private User getAuthorizedAdmin(String authHeader, String tokenParam) {
        String effectiveHeader = authHeader;
        if ((effectiveHeader == null || effectiveHeader.trim().isEmpty()) && tokenParam != null && !tokenParam.trim().isEmpty()) {
            effectiveHeader = "Bearer " + tokenParam.trim();
        }
        User user = authService.getAuthenticatedUser(effectiveHeader);
        if (user != null && user.getRole() == Role.ADMIN) {
            return user;
        }
        return null;
    }

    @GetMapping
    public ResponseEntity<?> getReports(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        User user = getAuthorizedAdmin(authHeader, token);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        subscriptionService.refreshAllMemberStatuses();
        List<Member> members = memberRepository.findAll();
        List<Payment> payments = paymentRepository.findAll();

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("totalMembers", members.size());
        data.put("activeMembers", memberRepository.countActiveMembers());
        data.put("expiredMembers", memberRepository.countExpiredMembers());
        data.put("expiringSoonMembers", memberRepository.countExpiringSoonMembers());

        data.put("morningMembers", memberRepository.countMorningBatch());
        data.put("eveningMembers", memberRepository.countEveningBatch());
        data.put("cardioMembers", memberRepository.countCardioMembers());
        data.put("strengthMembers", memberRepository.countStrengthMembers());

        data.put("totalRevenue", paymentRepository.sumTotalRevenue());
        data.put("subscriptionRevenue", paymentRepository.sumSubscriptionRevenue());
        data.put("cardioRevenue", paymentRepository.sumCardioRevenue());

        // Subscriptions distribution
        Map<String, Integer> planMap = new HashMap<>();
        for (Member m : members) {
            String p = m.getSubscriptionPlan() != null ? m.getSubscriptionPlan() : "1 Month";
            planMap.put(p, planMap.getOrDefault(p, 0) + 1);
        }
        data.put("subscriptionDistribution", planMap);

        // Payment Method distribution
        Map<String, Integer> methodMap = new HashMap<>();
        for (Payment p : payments) {
            String m = p.getPaymentMethod() != null ? p.getPaymentMethod() : "UPI";
            methodMap.put(m, methodMap.getOrDefault(m, 0) + 1);
        }
        data.put("paymentMethodDistribution", methodMap);

        return ResponseEntity.ok(data);
    }

    // ================= CSV REPORTS =================

    // 1. Members CSV
    @GetMapping("/export/members.csv")
    public ResponseEntity<?> exportMembersCsv(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        User user = getAuthorizedAdmin(authHeader, token);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        subscriptionService.refreshAllMemberStatuses();
        List<Member> members = memberRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("Member ID,Full Name,Phone Number,Admission Date,Subscription Plan,Training Category,Cardio,Batch,Payment Amount,Payment Date,Expiry Date,Days Remaining,Membership Status\n");

        for (Member m : members) {
            String cleanName = m.getFullName() != null ? m.getFullName().replace("\"", "'") : "";
            csv.append(String.format("%s,\"%s\",%s,%s,%s,%s,%s,%s,%.2f,%s,%s,%d,%s\n",
                m.getMemberCode(),
                cleanName,
                m.getPhoneNumber(),
                m.getAdmissionDate(),
                m.getSubscriptionPlan(),
                m.getTrainingCategory(),
                m.isHasCardio() ? "Yes" : "No",
                m.getBatch(),
                m.getTotalFee(),
                m.getStartDate(),
                m.getExpiryDate(),
                m.getDaysRemaining(),
                m.getStatus()
            ));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PowerFitnessKurnool_Members.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    // 2. Payments CSV
    @GetMapping("/export/payments.csv")
    public ResponseEntity<?> exportPaymentsCsv(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        User user = getAuthorizedAdmin(authHeader, token);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        List<Payment> payments = paymentRepository.findAllByOrderByPaymentDateDesc();
        double totalRev = paymentRepository.sumTotalRevenue();
        StringBuilder csv = new StringBuilder();
        csv.append("Payment ID,Type,Order ID,Member ID,Member Name,Plan / Item,Qty,Total Amount,Payment Date,Payment Method,Payment Status\n");

        for (Payment p : payments) {
            String memberName = p.getMember() != null && p.getMember().getFullName() != null 
                    ? p.getMember().getFullName().replace("\"", "'") 
                    : (p.getMemberName() != null ? p.getMemberName().replace("\"", "'") : "N/A");
            String memberCode = p.getMember() != null ? p.getMember().getMemberCode() : (p.getMemberCode() != null ? p.getMemberCode() : "N/A");
            String type = p.getPaymentType() != null ? p.getPaymentType() : "MEMBERSHIP";
            String orderNum = "SUPPLEMENT".equalsIgnoreCase(type) ? (p.getOrderNumber() != null ? p.getOrderNumber() : "-") : "-";
            String itemOrPlan = "SUPPLEMENT".equalsIgnoreCase(type) ? (p.getProductName() != null ? p.getProductName().replace("\"", "'") : "Supplement") : (p.getSubscriptionPlan() != null ? p.getSubscriptionPlan() : "Membership");
            int qty = "SUPPLEMENT".equalsIgnoreCase(type) ? p.getQuantity() : 1;

            csv.append(String.format("%s,%s,%s,%s,\"%s\",\"%s\",%d,%.2f,%s,%s,%s\n",
                p.getReceiptNumber(),
                type,
                orderNum,
                memberCode,
                memberName,
                itemOrPlan,
                qty,
                p.getAmount(),
                p.getPaymentDate(),
                p.getPaymentMethod(),
                p.getPaymentStatus()
            ));
        }
        csv.append(String.format("\n,,,,,,,Total Revenue,%.2f,,,\n", totalRev));

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PowerFitnessKurnool_Payments.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    // 3. Subscriptions CSV
    @GetMapping("/export/subscriptions.csv")
    public ResponseEntity<?> exportSubscriptionsCsv(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        User user = getAuthorizedAdmin(authHeader, token);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        subscriptionService.refreshAllMemberStatuses();
        List<Member> members = memberRepository.findAll();
        StringBuilder csv = new StringBuilder();
        csv.append("Member ID,Member Name,Plan,Start Date,Expiry Date,Total Duration,Days Remaining,Status\n");

        for (Member m : members) {
            String cleanName = m.getFullName() != null ? m.getFullName().replace("\"", "'") : "";
            csv.append(String.format("%s,\"%s\",%s,%s,%s,%d days,%d days,%s\n",
                m.getMemberCode(),
                cleanName,
                m.getSubscriptionPlan(),
                m.getStartDate(),
                m.getExpiryDate(),
                m.getTotalDays(),
                m.getDaysRemaining(),
                m.getStatus()
            ));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PowerFitnessKurnool_Subscriptions.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    // 4. Supplements CSV
    @GetMapping("/export/supplements.csv")
    public ResponseEntity<?> exportSupplementsCsv(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        User user = getAuthorizedAdmin(authHeader, token);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        List<Supplement> supplements = supplementRepository.findAllByOrderByCreatedAtDesc();
        StringBuilder csv = new StringBuilder();
        csv.append("Supplement ID,Product Name,Category,Price,Availability,Description\n");

        for (Supplement s : supplements) {
            String name = s.getName() != null ? s.getName().replace("\"", "'") : "";
            String desc = s.getDescription() != null ? s.getDescription().replace("\"", "'").replace("\n", " ") : "";
            csv.append(String.format("SUP-%d,\"%s\",%s,%.2f,%s,\"%s\"\n",
                s.getId(),
                name,
                s.getCategory(),
                s.getPrice(),
                "AVAILABLE".equals(s.getStockStatus()) ? "Available" : "Out of Stock",
                desc
            ));
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PowerFitnessKurnool_Supplements.csv")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(csv.toString());
    }

    // ================= PDF REPORTS =================

    // 1. Members PDF
    @GetMapping("/export/members.pdf")
    public ResponseEntity<?> exportMembersPdf(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        User user = getAuthorizedAdmin(authHeader, token);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        try {
            subscriptionService.refreshAllMemberStatuses();
            List<Member> members = memberRepository.findAll();
            byte[] pdfBytes = pdfReportService.generateMembersPdf(members);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PowerFitnessKurnool_Members.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (DocumentException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Report generation failed: " + e.getMessage()));
        }
    }

    // 2. Payments PDF
    @GetMapping("/export/payments.pdf")
    public ResponseEntity<?> exportPaymentsPdf(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        User user = getAuthorizedAdmin(authHeader, token);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        try {
            List<Payment> payments = paymentRepository.findAllByOrderByPaymentDateDesc();
            double totalRev = paymentRepository.sumTotalRevenue();
            byte[] pdfBytes = pdfReportService.generatePaymentsPdf(payments, totalRev);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PowerFitnessKurnool_Payments.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (DocumentException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Report generation failed: " + e.getMessage()));
        }
    }

    // 3. Subscriptions PDF
    @GetMapping("/export/subscriptions.pdf")
    public ResponseEntity<?> exportSubscriptionsPdf(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        User user = getAuthorizedAdmin(authHeader, token);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        try {
            subscriptionService.refreshAllMemberStatuses();
            List<Member> members = memberRepository.findAll();
            byte[] pdfBytes = pdfReportService.generateSubscriptionsPdf(members);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PowerFitnessKurnool_Subscriptions.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (DocumentException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Report generation failed: " + e.getMessage()));
        }
    }

    // 4. Supplements PDF
    @GetMapping("/export/supplements.pdf")
    public ResponseEntity<?> exportSupplementsPdf(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        User user = getAuthorizedAdmin(authHeader, token);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        try {
            List<Supplement> supplements = supplementRepository.findAllByOrderByCreatedAtDesc();
            byte[] pdfBytes = pdfReportService.generateSupplementsPdf(supplements);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PowerFitnessKurnool_Supplements.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (DocumentException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Report generation failed: " + e.getMessage()));
        }
    }

    // 5. Revenue PDF
    @GetMapping("/export/revenue.pdf")
    public ResponseEntity<?> exportRevenuePdf(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(value = "token", required = false) String token) {

        User user = getAuthorizedAdmin(authHeader, token);
        if (user == null) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        try {
            double totalRev = paymentRepository.sumTotalRevenue();
            double subRev = paymentRepository.sumSubscriptionRevenue();
            double cardioRev = paymentRepository.sumCardioRevenue();
            List<Payment> payments = paymentRepository.findAllByOrderByPaymentDateDesc();
            byte[] pdfBytes = pdfReportService.generateRevenuePdf(totalRev, subRev, cardioRev, payments);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=PowerFitnessKurnool_Revenue.pdf")
                    .contentType(MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        } catch (DocumentException e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Report generation failed: " + e.getMessage()));
        }
    }
}
