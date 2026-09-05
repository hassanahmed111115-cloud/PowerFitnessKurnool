package com.powerfitness.repository;

import com.powerfitness.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    List<Payment> findByMemberIdOrderByPaymentDateDesc(Long memberId);
    List<Payment> findAllByOrderByPaymentDateDesc();
    List<Payment> findTop10ByOrderByPaymentDateDesc();
    List<Payment> findByPaymentTypeOrderByPaymentDateDesc(String paymentType);

    Optional<Payment> findBySupplementOrderId(Long supplementOrderId);
    Optional<Payment> findByOrderNumber(String orderNumber);
    Optional<Payment> findByReceiptNumber(String receiptNumber);

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = 'Paid'")
    double sumTotalRevenue();

    @Query("SELECT COALESCE(SUM(p.baseFee), 0) FROM Payment p WHERE p.paymentStatus = 'Paid' AND (p.paymentType IS NULL OR p.paymentType = 'MEMBERSHIP')")
    double sumSubscriptionRevenue();

    @Query("SELECT COALESCE(SUM(p.cardioFee), 0) FROM Payment p WHERE p.paymentStatus = 'Paid' AND (p.paymentType IS NULL OR p.paymentType = 'MEMBERSHIP')")
    double sumCardioRevenue();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = 'Paid' AND p.paymentType = 'SUPPLEMENT'")
    double sumSupplementRevenue();

    @Query("SELECT COALESCE(SUM(p.amount), 0) FROM Payment p WHERE p.paymentStatus = 'Paid' AND (p.paymentType IS NULL OR p.paymentType = 'MEMBERSHIP')")
    double sumMembershipRevenue();
}
