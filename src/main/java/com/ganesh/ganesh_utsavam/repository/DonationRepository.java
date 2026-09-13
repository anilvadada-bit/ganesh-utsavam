package com.ganesh.ganesh_utsavam.repository;

import com.ganesh.ganesh_utsavam.entity.Donation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface DonationRepository extends JpaRepository<Donation, Long> {

    Optional<Donation> findByTransactionId(String transactionId);

    long countByPaymentStatus(String paymentStatus);

    List<Donation> findAllByOrderByDonationDateDesc();

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Donation d
            WHERE d.paymentStatus = 'SUCCESS'
            """)
    BigDecimal getTotalSuccessfulDonationAmount();

    // ================================
    // DONATION STATISTICS
    // ================================

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Donation d
            WHERE d.paymentStatus = 'SUCCESS'
            AND d.paymentMode = 'RAZORPAY'
            """)
    BigDecimal getTotalOnlineDonationAmount();

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Donation d
            WHERE d.paymentStatus = 'SUCCESS'
            AND d.paymentMode = 'CASH'
            """)
    BigDecimal getTotalCashDonationAmount();

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Donation d
            WHERE d.paymentStatus = 'SUCCESS'
            AND d.paymentMode = 'UPI'
            """)
    BigDecimal getTotalUpiDonationAmount();

    @Query("""
            SELECT COALESCE(SUM(d.amount), 0)
            FROM Donation d
            WHERE d.paymentStatus = 'SUCCESS'
            AND d.paymentMode = 'BANK TRANSFER'
            """)
    BigDecimal getTotalBankTransferDonationAmount();

    @Query("""
            SELECT COUNT(d)
            FROM Donation d
            WHERE d.paymentStatus = 'SUCCESS'
            AND d.paymentMode = 'RAZORPAY'
            """)
    long countOnlineDonations();

    @Query("""
            SELECT COUNT(d)
            FROM Donation d
            WHERE d.paymentStatus = 'SUCCESS'
            AND d.paymentMode IN ('CASH', 'UPI', 'BANK TRANSFER')
            """)
    long countOfflineDonations();
}