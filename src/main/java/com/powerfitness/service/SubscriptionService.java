package com.powerfitness.service;

import com.powerfitness.entity.Member;
import com.powerfitness.entity.Payment;
import com.powerfitness.repository.MemberRepository;
import com.powerfitness.repository.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Service
public class SubscriptionService {

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private PricingService pricingService;

    public void updateMemberSubscriptionStatus(Member member) {
        if (member == null || member.getExpiryDate() == null) return;
        LocalDate today = LocalDate.now();
        long daysRemaining = ChronoUnit.DAYS.between(today, member.getExpiryDate());

        if (daysRemaining < 0) {
            member.setStatus("EXPIRED");
        } else if (daysRemaining <= 7) {
            member.setStatus("EXPIRING_SOON");
        } else {
            member.setStatus("ACTIVE");
        }
    }

    public void refreshAllMemberStatuses() {
        List<Member> members = memberRepository.findAll();
        for (Member m : members) {
            updateMemberSubscriptionStatus(m);
        }
        memberRepository.saveAll(members);
    }

    @Transactional
    public Member renewSubscription(Member member, String newPlan, boolean cardioOption,
                                     String paymentMethod, String paymentStatus, String transactionRef, String notes) {
        LocalDate today = LocalDate.now();
        LocalDate newStartDate;

        // If currently active, start from existing expiry date; if expired or today or before, start from today
        if (member.getExpiryDate() != null && member.getExpiryDate().isAfter(today)) {
            newStartDate = member.getExpiryDate();
        } else {
            newStartDate = today;
        }

        int durationDays = pricingService.getPlanDurationDays(newPlan);
        LocalDate newExpiryDate = newStartDate.plusDays(durationDays);

        double baseFee = pricingService.getBasePlanPrice(newPlan);
        double cardioFee = cardioOption ? PricingService.CARDIO_FEE : 0.0;
        double totalFee = baseFee + cardioFee;

        member.setSubscriptionPlan(newPlan);
        member.setHasCardio(cardioOption);
        member.setTotalFee(totalFee);
        member.setStartDate(newStartDate);
        member.setExpiryDate(newExpiryDate);
        if (cardioOption && !"Cardio".equals(member.getTrainingCategory())) {
            // Keep training category or ensure cardio is tracked
        }
        updateMemberSubscriptionStatus(member);
        Member updated = memberRepository.save(member);

        // Record payment
        Payment payment = new Payment();
        String receiptNumber = "REC-" + System.currentTimeMillis() % 10000000;
        payment.setReceiptNumber(receiptNumber);
        payment.setMember(updated);
        payment.setBatch(updated.getBatch());
        payment.setPaymentType("MEMBERSHIP");
        payment.setAmount(totalFee);
        payment.setBaseFee(baseFee);
        payment.setCardioFee(cardioFee);
        payment.setPaymentDate(today);
        payment.setPaymentMethod(paymentMethod != null ? paymentMethod : "UPI");
        payment.setSubscriptionPlan(newPlan);
        payment.setPaymentStatus(paymentStatus != null ? paymentStatus : "Paid");
        payment.setTransactionRef(transactionRef);
        payment.setNotes("Renewal: " + (notes != null ? notes : "Membership Renewal"));
        paymentRepository.save(payment);

        return updated;
    }
}
