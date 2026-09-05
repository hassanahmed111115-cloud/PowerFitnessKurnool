package com.powerfitness.controller;

import com.powerfitness.entity.*;
import com.powerfitness.repository.*;
import com.powerfitness.service.AuthService;
import com.powerfitness.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SupplementOrderController {

    @Autowired
    private SupplementOrderRepository orderRepository;

    @Autowired
    private SupplementCollectionRepository collectionRepository;

    @Autowired
    private SupplementRepository supplementRepository;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private AuthService authService;

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private PaymentRepository paymentRepository;

    // ================= 1. MEMBER: BUY SUPPLEMENT =================
    @PostMapping("/orders/buy")
    public ResponseEntity<?> buySupplement(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestBody Map<String, Object> body) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Please login to purchase supplements"));
        }

        if (!body.containsKey("supplementId")) {
            return ResponseEntity.badRequest().body(Map.of("error", "Supplement ID is required"));
        }

        Long supplementId = Long.valueOf(body.get("supplementId").toString());
        int quantity = body.containsKey("quantity") ? Integer.parseInt(body.get("quantity").toString()) : 1;
        if (quantity <= 0) {
            return ResponseEntity.badRequest().body(Map.of("error", "Quantity must be at least 1"));
        }

        String paymentMethod = body.getOrDefault("paymentMethod", "UPI").toString();
        String upiRef = body.containsKey("upiTransactionRef") ? body.get("upiTransactionRef").toString().trim() : "";
        String notes = body.containsKey("notes") ? body.get("notes").toString().trim() : "";

        Optional<Supplement> suppOpt = supplementRepository.findById(supplementId);
        if (suppOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Supplement supplement = suppOpt.get();
        supplement.recalculatePrices();

        if (!supplement.isInStock()) {
            return ResponseEntity.badRequest().body(Map.of("error", "This supplement is currently out of stock."));
        }

        int availableStock = supplement.getStockQuantity();
        if (quantity > availableStock) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "Requested quantity (" + quantity + ") exceeds available stock (" + availableStock + " units available)."
            ));
        }

        // Secure server price calculation
        double originalPrice = supplement.getOriginalPrice() > 0 ? supplement.getOriginalPrice() : supplement.getPrice();
        double finalPrice = supplement.getEffectivePrice();
        double totalAmount = Math.round(finalPrice * quantity * 100.0) / 100.0;

        // Find linked Member for athlete details
        Member member = memberRepository.findByUserId(user.getId()).orElse(null);
        if (member == null && user.getUsername() != null) {
            member = memberRepository.findByPhoneNumber(user.getUsername()).orElse(null);
        }

        // Generate unique Order Number
        long count = orderRepository.count() + 1;
        String orderNumber = "PFK-SUP-" + String.format("%04d", count);

        String paymentStatus = "PENDING";
        String orderStatus = "PAYMENT_PENDING";

        SupplementOrder order = new SupplementOrder(
            orderNumber,
            user,
            member,
            supplement,
            quantity,
            originalPrice,
            supplement.getOfferType(),
            supplement.getDiscountValue(),
            finalPrice,
            totalAmount,
            paymentMethod,
            paymentStatus,
            upiRef
        );
        order.setOrderStatus(orderStatus);
        order.setNotes(notes);

        SupplementOrder saved = orderRepository.save(order);

        // Create unified Payment record in Admin Payments
        long payCount = paymentRepository.count() + 1001;
        String receiptNumber = "PFK-PAY-" + String.format("%04d", payCount);
        while (paymentRepository.findByReceiptNumber(receiptNumber).isPresent()) {
            payCount++;
            receiptNumber = "PFK-PAY-" + String.format("%04d", payCount);
        }

        Payment payment = new Payment();
        payment.setReceiptNumber(receiptNumber);
        payment.setPaymentType("SUPPLEMENT");
        payment.setMember(member);
        if (member != null) {
            payment.setMemberName(member.getFullName());
            payment.setMemberCode(member.getMemberCode());
            payment.setMemberPhone(member.getPhoneNumber());
            payment.setBatch(member.getBatch());
        } else if (user != null) {
            payment.setMemberName(user.getFullName());
            payment.setMemberPhone(user.getUsername());
        }
        payment.setAmount(totalAmount);
        payment.setBaseFee(totalAmount);
        payment.setCardioFee(0.0);
        payment.setPaymentDate(java.time.LocalDate.now());
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod.toUpperCase() : "UPI");
        payment.setSubscriptionPlan("SUPPLEMENT");
        payment.setPaymentStatus("Pending");
        payment.setTransactionRef(upiRef);
        payment.setSupplementOrder(saved);
        payment.setOrderNumber(orderNumber);
        payment.setProductName(supplement.getName());
        payment.setQuantity(quantity);
        payment.setOriginalPrice(originalPrice);
        payment.setDiscountValue(supplement.getDiscountValue());
        payment.setFinalPrice(finalPrice);
        payment.setNotes("Supplement Purchase: " + quantity + "x " + supplement.getName() + " (Order: " + orderNumber + ")");
        Payment savedPayment = paymentRepository.save(payment);

        saved.setPaymentId(savedPayment.getId());
        saved.setPaymentReceiptNumber(savedPayment.getReceiptNumber());
        saved = orderRepository.save(saved);

        // Notify member
        notificationRepository.save(new Notification(
            user.getId(),
            "Supplement Order Created (" + orderNumber + ")",
            "Order for " + quantity + "x " + supplement.getName() + " (Total: Rs." + (int)totalAmount + ") placed. Please collect from the Front Desk.",
            "SUPPLEMENT"
        ));

        return ResponseEntity.ok(saved);
    }

    // ================= 2. MEMBER: VIEW MY ORDERS =================
    @GetMapping("/orders/my-orders")
    public ResponseEntity<?> getMyOrders(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<SupplementOrder> orders = orderRepository.findByUserIdOrderByOrderDateDesc(user.getId());
        return ResponseEntity.ok(orders);
    }

    // ================= 3. MEMBER: VIEW MY COLLECTION HISTORY =================
    @GetMapping("/orders/my-collections")
    public ResponseEntity<?> getMyCollections(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Unauthorized"));
        }

        List<SupplementCollection> collections = collectionRepository.findByOrderUserIdOrderByCollectionDateDesc(user.getId());
        return ResponseEntity.ok(collections);
    }

    // ================= 4. ADMIN: GET ALL SUPPLEMENT ORDERS =================
    @GetMapping("/admin/orders")
    public ResponseEntity<?> getAdminOrders(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String q) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        List<SupplementOrder> list = orderRepository.findAllByOrderByOrderDateDesc();

        if (status != null && !status.trim().isEmpty() && !"ALL".equalsIgnoreCase(status)) {
            list = list.stream().filter(o -> status.equalsIgnoreCase(o.getOrderStatus())).collect(Collectors.toList());
        }

        if (q != null && !q.trim().isEmpty()) {
            String query = q.trim().toLowerCase();
            list = list.stream().filter(o -> {
                String memberName = o.getMember() != null ? o.getMember().getFullName().toLowerCase() : o.getUser().getFullName().toLowerCase();
                String memberCode = o.getMember() != null ? o.getMember().getMemberCode().toLowerCase() : "";
                String phone = o.getMember() != null ? o.getMember().getPhoneNumber() : o.getUser().getUsername();
                String suppName = o.getSupplement().getName().toLowerCase();
                String orderNum = o.getOrderNumber().toLowerCase();

                return memberName.contains(query) || memberCode.contains(query) ||
                       (phone != null && phone.contains(query)) ||
                       suppName.contains(query) || orderNum.contains(query);
            }).collect(Collectors.toList());
        }

        return ResponseEntity.ok(list);
    }

    // ================= 5. ADMIN: VERIFY PAYMENT =================
    @PatchMapping("/admin/orders/{id}/verify-payment")
    public ResponseEntity<?> verifyPayment(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<SupplementOrder> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();

        SupplementOrder order = orderOpt.get();
        order.setPaymentStatus("PAID");
        if (!"COLLECTED".equalsIgnoreCase(order.getOrderStatus())) {
            order.setOrderStatus("READY_FOR_COLLECTION");
        }
        order.setUpdatedAt(LocalDateTime.now());
        SupplementOrder saved = orderRepository.save(order);

        // Synchronize linked Payment record
        Optional<Payment> payOpt = paymentRepository.findBySupplementOrderId(order.getId());
        if (payOpt.isEmpty() && order.getOrderNumber() != null) {
            payOpt = paymentRepository.findByOrderNumber(order.getOrderNumber());
        }
        if (payOpt.isPresent()) {
            Payment p = payOpt.get();
            p.setPaymentStatus("Paid");
            p.setPaymentDate(java.time.LocalDate.now());
            if (p.getBatch() == null && order.getMember() != null) {
                p.setBatch(order.getMember().getBatch());
            }
            paymentRepository.save(p);
        }

        // Notify athlete
        notificationRepository.save(new Notification(
            order.getUser().getId(),
            "Payment Verified for " + order.getOrderNumber(),
            "Payment of Rs." + (int)order.getTotalAmount() + " confirmed. Your product is READY FOR COLLECTION at the gym front desk!",
            "SUPPLEMENT"
        ));

        return ResponseEntity.ok(saved);
    }

    // ================= 6. ADMIN: UPDATE ORDER STATUS =================
    @PatchMapping("/admin/orders/{id}/status")
    public ResponseEntity<?> updateOrderStatus(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<SupplementOrder> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) return ResponseEntity.notFound().build();

        SupplementOrder order = orderOpt.get();
        String newStatus = body.get("orderStatus");
        if (newStatus != null) {
            order.setOrderStatus(newStatus);
            if ("PAID".equalsIgnoreCase(newStatus)) {
                order.setPaymentStatus("PAID");
            }
        }
        order.setUpdatedAt(LocalDateTime.now());
        SupplementOrder saved = orderRepository.save(order);

        // Synchronize linked Payment record
        Optional<Payment> payOpt2 = paymentRepository.findBySupplementOrderId(order.getId());
        if (payOpt2.isEmpty() && order.getOrderNumber() != null) {
            payOpt2 = paymentRepository.findByOrderNumber(order.getOrderNumber());
        }
        if (payOpt2.isPresent()) {
            Payment p = payOpt2.get();
            if ("PAID".equalsIgnoreCase(newStatus) || "READY_FOR_COLLECTION".equalsIgnoreCase(newStatus) || "COLLECTED".equalsIgnoreCase(newStatus)) {
                p.setPaymentStatus("Paid");
            } else if ("CANCELLED".equalsIgnoreCase(newStatus)) {
                p.setPaymentStatus("Failed");
            } else if ("PAYMENT_PENDING".equalsIgnoreCase(newStatus)) {
                p.setPaymentStatus("Pending");
            }
            paymentRepository.save(p);
        }

        return ResponseEntity.ok(saved);
    }

    // ================= 7. ADMIN / FRONT DESK: COLLECT PRODUCT WITH CAMERA PHOTO =================
    @PostMapping("/admin/orders/{id}/collect")
    public ResponseEntity<?> collectProduct(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        Optional<SupplementOrder> orderOpt = orderRepository.findById(id);
        if (orderOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        SupplementOrder order = orderOpt.get();
        if ("COLLECTED".equalsIgnoreCase(order.getOrderStatus())) {
            return ResponseEntity.badRequest().body(Map.of("error", "This order has already been collected."));
        }

        String photoBase64 = body.get("collectionPhotoBase64");
        String photoUrl = body.get("collectionPhotoUrl");
        String notes = body.get("notes");

        // Save camera photo if base64 provided
        if (photoBase64 != null && !photoBase64.trim().isEmpty()) {
            try {
                photoUrl = fileStorageService.saveCollectionPhotoBase64(photoBase64);
            } catch (IOException e) {
                return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save collection photo: " + e.getMessage()));
            }
        }

        if (photoUrl == null || photoUrl.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Collection photo is required. Please capture the member's photo."));
        }

        // Format dates
        LocalDateTime now = LocalDateTime.now();
        String dateFormatted = now.format(DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale.ENGLISH));
        String timeFormatted = now.format(DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH));

        // 1. Update Order
        order.setOrderStatus("COLLECTED");
        order.setPaymentStatus("PAID");
        order.setCollectionDate(now);
        order.setCollectionPhotoUrl(photoUrl);
        order.setCollectedByStaff(user.getFullName() != null ? user.getFullName() : "Admin / Front Desk");
        if (notes != null) order.setNotes(notes);
        order.setUpdatedAt(now);
        SupplementOrder savedOrder = orderRepository.save(order);

        // Ensure linked payment remains Paid
        Optional<Payment> payOpt3 = paymentRepository.findBySupplementOrderId(order.getId());
        if (payOpt3.isEmpty() && order.getOrderNumber() != null) {
            payOpt3 = paymentRepository.findByOrderNumber(order.getOrderNumber());
        }
        if (payOpt3.isPresent()) {
            Payment p = payOpt3.get();
            p.setPaymentStatus("Paid");
            paymentRepository.save(p);
        }

        // 2. Decrement Supplement Stock in database
        Supplement supplement = order.getSupplement();
        supplement.decrementStock(order.getQuantity());
        supplementRepository.save(supplement);

        // 3. Create Permanent Collection Record
        long colCount = collectionRepository.count() + 1;
        String collectionNumber = "PFK-COL-" + String.format("%04d", colCount);

        SupplementCollection collection = new SupplementCollection(
            collectionNumber,
            savedOrder,
            photoUrl,
            user.getFullName() != null ? user.getFullName() : "Admin / Front Desk",
            dateFormatted,
            timeFormatted
        );
        if (notes != null) collection.setNotes(notes);
        SupplementCollection savedCollection = collectionRepository.save(collection);

        // 4. Send Confirmation Notification to Member
        notificationRepository.save(new Notification(
            order.getUser().getId(),
            "Product Collected: " + supplement.getName() + " ✓",
            "Your supplement order (" + order.getOrderNumber() + ") was collected on " + dateFormatted + " at " + timeFormatted + ". Collection photo recorded.",
            "SUPPLEMENT"
        ));

        return ResponseEntity.ok(Map.of(
            "message", "Supplement collection confirmed successfully!",
            "order", savedOrder,
            "collection", savedCollection,
            "updatedStock", supplement.getStockQuantity(),
            "stockStatus", supplement.getStockStatus()
        ));
    }

    // ================= 8. ADMIN: GET ALL COLLECTION HISTORY =================
    @GetMapping("/admin/collections")
    public ResponseEntity<?> getAdminCollections(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        User user = authService.getAuthenticatedUser(authHeader);
        if (user == null || user.getRole() != Role.ADMIN) {
            return ResponseEntity.status(403).body(Map.of("error", "Admin access required"));
        }

        List<SupplementCollection> collections = collectionRepository.findAllByOrderByCollectionDateDesc();
        return ResponseEntity.ok(collections);
    }
}
