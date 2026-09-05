package com.powerfitness.controller;

import com.powerfitness.entity.Member;
import com.powerfitness.entity.Notification;
import com.powerfitness.entity.Payment;
import com.powerfitness.entity.Role;
import com.powerfitness.entity.SupplementOrder;
import com.powerfitness.entity.User;
import com.powerfitness.repository.MemberRepository;
import com.powerfitness.repository.NotificationRepository;
import com.powerfitness.repository.PaymentRepository;
import com.powerfitness.repository.SupplementOrderRepository;
import com.powerfitness.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class PaymentController {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private SupplementOrderRepository orderRepository;

    @Autowired
    private AuthService authService;

    // Helper: ensure all supplement orders have a corresponding payment record
    private void syncExistingSupplementOrders() {
        try {
            List<SupplementOrder> orders = orderRepository.findAll();
            for (SupplementOrder o : orders) {
                Optional<Payment> existing = paymentRepository.findBySupplementOrderId(o.getId());
                if (existing.isEmpty() && o.getOrderNumber() != null) {
                    existing = paymentRepository.findByOrderNumber(o.getOrderNumber());
                }
                if (existing.isEmpty()) {
                    long payCount = paymentRepository.count() + 1001;
                    String receiptNumber = "PFK-PAY-" + String.format("%04d", payCount);
                    while (paymentRepository.findByReceiptNumber(receiptNumber).isPresent()) {
                        payCount++;
                        receiptNumber = "PFK-PAY-" + String.format("%04d", payCount);
                    }

                    Payment p = new Payment();
                    p.setReceiptNumber(receiptNumber);
                    p.setPaymentType("SUPPLEMENT");
                    Member m = o.getMember();
                    if (m == null && o.getUser() != null) {
                        m = memberRepository.findByUserId(o.getUser().getId()).orElse(null);
                        if (m == null && o.getUser().getUsername() != null) {
                            m = memberRepository.findByPhoneNumber(o.getUser().getUsername()).orElse(null);
                        }
                    }
                    p.setMember(m);
                    if (m != null) {
                        p.setMemberName(m.getFullName());
                        p.setMemberCode(m.getMemberCode());
                        p.setMemberPhone(m.getPhoneNumber());
                        p.setBatch(m.getBatch());
                    } else if (o.getUser() != null) {
                        p.setMemberName(o.getUser().getFullName());
                        p.setMemberPhone(o.getUser().getUsername());
                        p.setBatch("Morning Batch");
                    }
                    p.setAmount(o.getTotalAmount());
                    p.setBaseFee(o.getTotalAmount());
                    p.setCardioFee(0.0);
                    p.setPaymentDate(o.getOrderDate() != null ? o.getOrderDate().toLocalDate() : java.time.LocalDate.now());
                    p.setPaymentMethod(o.getPaymentMethod() != null ? o.getPaymentMethod() : "UPI");
                    p.setSubscriptionPlan("SUPPLEMENT");
                    boolean isPaid = "PAID".equalsIgnoreCase(o.getPaymentStatus()) ||
                                     "READY_FOR_COLLECTION".equalsIgnoreCase(o.getOrderStatus()) ||
                                     "COLLECTED".equalsIgnoreCase(o.getOrderStatus());
                    p.setPaymentStatus(isPaid ? "Paid" : "Pending");
                    p.setTransactionRef(o.getUpiTransactionRef());
                    p.setSupplementOrder(o);
                    p.setOrderNumber(o.getOrderNumber());
                    if (o.getSupplement() != null) {
                        p.setProductName(o.getSupplement().getName());
                    }
                    p.setQuantity(o.getQuantity());
                    p.setOriginalPrice(o.getOriginalPrice());
                    p.setDiscountValue(o.getDiscountValue());
                    p.setFinalPrice(o.getFinalPrice());
                    p.setNotes("Supplement Purchase: " + o.getQuantity() + "x " + (o.getSupplement() != null ? o.getSupplement().getName() : "Item") + " (Order: " + o.getOrderNumber() + ")");
                    Payment savedP = paymentRepository.save(p);

                    o.setPaymentId(savedP.getId());
                    o.setPaymentReceiptNumber(savedP.getReceiptNumber());
                    orderRepository.save(o);
                }
            }
        } catch (Exception e) {
            // graceful fail-safe
        }
    }

    // --- Admin: List Payments with Filters & Search ---
    @GetMapping("/admin/payments")
    public ResponseEntity<?> getPayments(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String method,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        syncExistingSupplementOrders();

        List<Payment> list = paymentRepository.findAllByOrderByPaymentDateDesc();

        // Filter by Type: ALL, MEMBERSHIP, SUPPLEMENT
        if (type != null && !type.trim().isEmpty() && !"All".equalsIgnoreCase(type)) {
            if ("SUPPLEMENT".equalsIgnoreCase(type)) {
                list = list.stream().filter(p -> "SUPPLEMENT".equalsIgnoreCase(p.getPaymentType())).collect(Collectors.toList());
            } else if ("MEMBERSHIP".equalsIgnoreCase(type)) {
                list = list.stream().filter(p -> p.getPaymentType() == null || "MEMBERSHIP".equalsIgnoreCase(p.getPaymentType())).collect(Collectors.toList());
            }
        }

        if (method != null && !method.trim().isEmpty() && !"All".equalsIgnoreCase(method)) {
            list = list.stream().filter(p -> method.equalsIgnoreCase(p.getPaymentMethod())).collect(Collectors.toList());
        }
        if (status != null && !status.trim().isEmpty() && !"All".equalsIgnoreCase(status)) {
            list = list.stream().filter(p -> status.equalsIgnoreCase(p.getPaymentStatus())).collect(Collectors.toList());
        }
        if (plan != null && !plan.trim().isEmpty() && !"All".equalsIgnoreCase(plan)) {
            list = list.stream().filter(p -> plan.equalsIgnoreCase(p.getSubscriptionPlan())).collect(Collectors.toList());
        }

        // Search query filter q
        if (q != null && !q.trim().isEmpty()) {
            String query = q.trim().toLowerCase();
            list = list.stream().filter(p -> {
                String memberName = p.getMember() != null ? p.getMember().getFullName().toLowerCase() : (p.getMemberName() != null ? p.getMemberName().toLowerCase() : "");
                String memberCode = p.getMember() != null ? p.getMember().getMemberCode().toLowerCase() : (p.getMemberCode() != null ? p.getMemberCode().toLowerCase() : "");
                String phone = p.getMember() != null ? p.getMember().getPhoneNumber() : (p.getMemberPhone() != null ? p.getMemberPhone() : "");
                String receipt = p.getReceiptNumber() != null ? p.getReceiptNumber().toLowerCase() : "";
                String orderNum = p.getOrderNumber() != null ? p.getOrderNumber().toLowerCase() : "";
                String product = p.getProductName() != null ? p.getProductName().toLowerCase() : "";
                String ref = p.getTransactionRef() != null ? p.getTransactionRef().toLowerCase() : "";

                return memberName.contains(query) || memberCode.contains(query) ||
                       (phone != null && phone.contains(query)) ||
                       receipt.contains(query) || orderNum.contains(query) ||
                       product.contains(query) || ref.contains(query);
            }).collect(Collectors.toList());
        }

        double totalRevenue = paymentRepository.sumTotalRevenue();
        double subRevenue = paymentRepository.sumSubscriptionRevenue();
        double cardioRev = paymentRepository.sumCardioRevenue();
        double suppRevenue = paymentRepository.sumSupplementRevenue();

        Map<String, Object> res = new HashMap<>();
        res.put("payments", list);
        res.put("totalRevenue", totalRevenue);
        res.put("subscriptionRevenue", subRevenue);
        res.put("cardioRevenue", cardioRev);
        res.put("supplementRevenue", suppRevenue);

        return ResponseEntity.ok(res);
    }

    // --- Admin: Update Payment Status with Bi-Directional Synchronization ---
    @PatchMapping("/admin/payments/{id}/status")
    public ResponseEntity<?> updatePaymentStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        String newStatus = body.get("status");
        if (newStatus == null || (!newStatus.equals("Paid") && !newStatus.equals("Pending") && !newStatus.equals("Failed"))) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status must be Paid, Pending, or Failed"));
        }

        Optional<Payment> pOpt = paymentRepository.findById(id);
        if (pOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Payment p = pOpt.get();
        p.setPaymentStatus(newStatus);
        if ("Paid".equalsIgnoreCase(newStatus)) {
            p.setPaymentDate(LocalDate.now());
        }
        if (p.getBatch() == null && p.getMember() != null) {
            p.setBatch(p.getMember().getBatch());
        }
        Payment saved = paymentRepository.save(p);

        // Bi-directional synchronization with SupplementOrder
        if ("SUPPLEMENT".equalsIgnoreCase(p.getPaymentType()) || p.getSupplementOrder() != null || p.getOrderNumber() != null) {
            SupplementOrder order = p.getSupplementOrder();
            if (order == null && p.getOrderNumber() != null) {
                order = orderRepository.findByOrderNumber(p.getOrderNumber()).orElse(null);
            }
            if (order != null) {
                if ("Paid".equalsIgnoreCase(newStatus)) {
                    order.setPaymentStatus("PAID");
                    if (!"COLLECTED".equalsIgnoreCase(order.getOrderStatus())) {
                        order.setOrderStatus("READY_FOR_COLLECTION");
                    }
                } else if ("Failed".equalsIgnoreCase(newStatus)) {
                    order.setPaymentStatus("FAILED");
                    if (!"COLLECTED".equalsIgnoreCase(order.getOrderStatus())) {
                        order.setOrderStatus("CANCELLED");
                    }
                } else if ("Pending".equalsIgnoreCase(newStatus)) {
                    order.setPaymentStatus("PENDING");
                    if (!"COLLECTED".equalsIgnoreCase(order.getOrderStatus())) {
                        order.setOrderStatus("PAYMENT_PENDING");
                    }
                }
                order.setUpdatedAt(java.time.LocalDateTime.now());
                orderRepository.save(order);
            }
        }

        // Notify user if status changed
        Long userId = null;
        if (p.getMember() != null && p.getMember().getUser() != null) {
            userId = p.getMember().getUser().getId();
        } else if (p.getSupplementOrder() != null && p.getSupplementOrder().getUser() != null) {
            userId = p.getSupplementOrder().getUser().getId();
        }
        if (userId != null) {
            String desc = "SUPPLEMENT".equalsIgnoreCase(p.getPaymentType()) ? ("supplement order " + (p.getOrderNumber() != null ? p.getOrderNumber() : "")) : ("subscription (" + p.getReceiptNumber() + ")");
            String msg = "Your payment of ₹" + (int)p.getAmount() + " for " + desc + " status is now: " + newStatus;
            notificationRepository.save(new Notification(userId, "Payment Status Updated", msg, "PAYMENT"));
        }

        return ResponseEntity.ok(saved);
    }

    // --- Admin: Get Daily Payment Collection Summary & History ---
    @GetMapping("/admin/payments/daily-collection")
    public ResponseEntity<?> getDailyCollection(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String date) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        syncExistingSupplementOrders();

        LocalDate targetDate;
        if (date != null && !date.trim().isEmpty()) {
            try {
                targetDate = LocalDate.parse(date.trim());
            } catch (Exception e) {
                targetDate = LocalDate.now();
            }
        } else {
            targetDate = LocalDate.now();
        }

        List<Payment> allPayments = paymentRepository.findAll();

        double morningMembership = 0.0;
        double morningSupplements = 0.0;
        double eveningMembership = 0.0;
        double eveningSupplements = 0.0;
        int morningCount = 0;
        int eveningCount = 0;

        // Map for daily collection history: date -> [morning, evening, total]
        Map<LocalDate, double[]> historyMap = new TreeMap<>(Collections.reverseOrder());

        for (Payment p : allPayments) {
            if (!"Paid".equalsIgnoreCase(p.getPaymentStatus())) {
                continue; // Only PAID/VERIFIED payments
            }

            LocalDate pDate = p.getPaymentDate();
            if (pDate == null) continue;

            String b = p.getBatch();
            if (b == null && p.getMember() != null) {
                b = p.getMember().getBatch();
            }
            if (b == null && p.getSupplementOrder() != null && p.getSupplementOrder().getMember() != null) {
                b = p.getSupplementOrder().getMember().getBatch();
            }
            boolean isEvening = b != null && b.toLowerCase().contains("evening");
            boolean isSupplement = "SUPPLEMENT".equalsIgnoreCase(p.getPaymentType());
            double amt = p.getAmount();

            // Record in history map
            double[] hist = historyMap.computeIfAbsent(pDate, k -> new double[3]);
            if (isEvening) {
                hist[1] += amt;
            } else {
                hist[0] += amt;
            }
            hist[2] += amt;

            // Record in target date collection
            if (pDate.equals(targetDate)) {
                if (isEvening) {
                    eveningCount++;
                    if (isSupplement) {
                        eveningSupplements += amt;
                    } else {
                        eveningMembership += amt;
                    }
                } else {
                    morningCount++;
                    if (isSupplement) {
                        morningSupplements += amt;
                    } else {
                        morningMembership += amt;
                    }
                }
            }
        }

        double morningTotal = morningMembership + morningSupplements;
        double eveningTotal = eveningMembership + eveningSupplements;
        double totalCollection = morningTotal + eveningTotal;

        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        DateTimeFormatter displayDtf = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH);

        List<Map<String, Object>> historyList = new ArrayList<>();
        for (Map.Entry<LocalDate, double[]> entry : historyMap.entrySet()) {
            Map<String, Object> h = new LinkedHashMap<>();
            h.put("date", entry.getKey().toString());
            h.put("formattedDate", entry.getKey().format(dtf));
            h.put("displayDate", entry.getKey().format(displayDtf));
            h.put("morning", Math.round(entry.getValue()[0] * 100.0) / 100.0);
            h.put("evening", Math.round(entry.getValue()[1] * 100.0) / 100.0);
            h.put("total", Math.round(entry.getValue()[2] * 100.0) / 100.0);
            h.put("morningTotal", Math.round(entry.getValue()[0] * 100.0) / 100.0);
            h.put("eveningTotal", Math.round(entry.getValue()[1] * 100.0) / 100.0);
            h.put("grandTotal", Math.round(entry.getValue()[2] * 100.0) / 100.0);
            historyList.add(h);
        }

        Map<String, Object> morningBreakdown = new LinkedHashMap<>();
        morningBreakdown.put("membership", Math.round(morningMembership * 100.0) / 100.0);
        morningBreakdown.put("supplements", Math.round(morningSupplements * 100.0) / 100.0);
        morningBreakdown.put("total", Math.round(morningTotal * 100.0) / 100.0);
        morningBreakdown.put("count", morningCount);

        Map<String, Object> eveningBreakdown = new LinkedHashMap<>();
        eveningBreakdown.put("membership", Math.round(eveningMembership * 100.0) / 100.0);
        eveningBreakdown.put("supplements", Math.round(eveningSupplements * 100.0) / 100.0);
        eveningBreakdown.put("total", Math.round(eveningTotal * 100.0) / 100.0);
        eveningBreakdown.put("count", eveningCount);

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("date", targetDate.toString());
        res.put("formattedDate", targetDate.format(dtf));
        res.put("displayDate", targetDate.format(displayDtf));
        res.put("morningTotal", Math.round(morningTotal * 100.0) / 100.0);
        res.put("morningMembership", Math.round(morningMembership * 100.0) / 100.0);
        res.put("morningSupplements", Math.round(morningSupplements * 100.0) / 100.0);
        res.put("eveningTotal", Math.round(eveningTotal * 100.0) / 100.0);
        res.put("eveningMembership", Math.round(eveningMembership * 100.0) / 100.0);
        res.put("eveningSupplements", Math.round(eveningSupplements * 100.0) / 100.0);
        res.put("grandTotal", Math.round(totalCollection * 100.0) / 100.0);
        res.put("totalCollection", Math.round(totalCollection * 100.0) / 100.0);
        res.put("morningBreakdown", morningBreakdown);
        res.put("eveningBreakdown", eveningBreakdown);
        res.put("history", historyList);

        return ResponseEntity.ok(res);
    }

    // --- User: Get Personal Payments ---
    @GetMapping("/user/payments")
    public ResponseEntity<?> getUserPayments(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        Optional<Member> mOpt = memberRepository.findByUserId(user.getId());
        if (mOpt.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        List<Payment> list = paymentRepository.findByMemberIdOrderByPaymentDateDesc(mOpt.get().getId());
        return ResponseEntity.ok(list);
    }

    // --- User: Submit UPI Payment Confirmation (UTR) ---
    @PostMapping("/user/payments/submit-utr")
    public ResponseEntity<?> submitUtr(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, String> body) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        String utr = body.get("utr");
        if (utr == null || utr.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "UTR / Transaction Reference is required"));
        }

        Optional<Member> mOpt = memberRepository.findByUserId(user.getId());
        if (mOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of("error", "Member not found"));
        }

        Member m = mOpt.get();
        Payment p = new Payment();
        p.setReceiptNumber("REC-" + System.currentTimeMillis() % 10000000);
        p.setMember(m);
        p.setAmount(m.getTotalFee());
        p.setBaseFee(m.getTotalFee() - (m.isHasCardio() ? 500 : 0));
        p.setCardioFee(m.isHasCardio() ? 500 : 0);
        p.setPaymentDate(java.time.LocalDate.now());
        p.setPaymentMethod("UPI");
        p.setSubscriptionPlan(m.getSubscriptionPlan());
        p.setPaymentStatus("Pending");
        p.setTransactionRef(utr.trim());
        p.setNotes("Online UPI Payment submitted by member for verification.");
        Payment saved = paymentRepository.save(p);

        // Notify Admin
        notificationRepository.save(new Notification(null, "Payment Verification Required", "Member " + m.getFullName() + " submitted UPI Ref: " + utr + " for ₹" + m.getTotalFee(), "PAYMENT"));

        return ResponseEntity.ok(saved);
    }
}
