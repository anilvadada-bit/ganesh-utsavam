package com.ganesh.ganesh_utsavam.controller;

import com.ganesh.ganesh_utsavam.dto.OfflineDonationRequest;
import com.ganesh.ganesh_utsavam.entity.Donation;
import com.ganesh.ganesh_utsavam.entity.Expense;
import com.ganesh.ganesh_utsavam.repository.DonationRepository;
import com.ganesh.ganesh_utsavam.repository.ExpenseRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final DonationRepository donationRepository;
    private final ExpenseRepository expenseRepository;

    public AdminController(
            DonationRepository donationRepository,
            ExpenseRepository expenseRepository) {

        this.donationRepository = donationRepository;
        this.expenseRepository = expenseRepository;
    }

    // =========================================================
    // ADMIN DASHBOARD
    // =========================================================

    @GetMapping("/dashboard")
    public Map<String, Object> getDashboard() {

        // -----------------------------------------------------
        // DONATION STATISTICS
        // -----------------------------------------------------

        long totalDonations =
                donationRepository.count();

        long successfulDonations =
                donationRepository.countByPaymentStatus("SUCCESS");

        BigDecimal totalAmount =
                donationRepository.getTotalSuccessfulDonationAmount();

        // -----------------------------------------------------
        // EXPENSE STATISTICS
        // -----------------------------------------------------

        BigDecimal totalExpenses =
                expenseRepository.getTotalExpenseAmount();

        // -----------------------------------------------------
        // BALANCE
        // -----------------------------------------------------

        BigDecimal balance =
                totalAmount.subtract(totalExpenses);

        // -----------------------------------------------------
        // DONATION BREAKDOWN
        // -----------------------------------------------------

        BigDecimal onlineAmount =
                donationRepository.getTotalOnlineDonationAmount();

        BigDecimal cashAmount =
                donationRepository.getTotalCashDonationAmount();

        BigDecimal upiAmount =
                donationRepository.getTotalUpiDonationAmount();

        BigDecimal bankTransferAmount =
                donationRepository.getTotalBankTransferDonationAmount();

        long onlineDonations =
                donationRepository.countOnlineDonations();

        long offlineDonations =
                donationRepository.countOfflineDonations();

        Map<String, Object> donationBreakdown = Map.of(
                "onlineAmount", onlineAmount,
                "cashAmount", cashAmount,
                "upiAmount", upiAmount,
                "bankTransferAmount", bankTransferAmount,
                "onlineDonations", onlineDonations,
                "offlineDonations", offlineDonations
        );

        // -----------------------------------------------------
        // RECENT DONATIONS
        // -----------------------------------------------------

        List<Donation> recentDonations =
                donationRepository.findAllByOrderByDonationDateDesc();

        // -----------------------------------------------------
        // RECENT EXPENSES
        // -----------------------------------------------------

        List<Expense> recentExpenses =
                expenseRepository.findAllByOrderByExpenseDateDesc();

        // -----------------------------------------------------
        // RETURN DASHBOARD DATA
        // -----------------------------------------------------

        return Map.of(
                "totalDonations", totalDonations,
                "successfulDonations", successfulDonations,
                "totalAmount", totalAmount,
                "totalExpenses", totalExpenses,
                "balance", balance,
                "donationBreakdown", donationBreakdown,
                "recentDonations", recentDonations,
                "recentExpenses", recentExpenses
        );
    }

    // =========================================================
    // ADD OFFLINE DONATION
    // =========================================================

    @PostMapping("/donations/offline")
    public Map<String, Object> addOfflineDonation(
            @RequestBody OfflineDonationRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Donation details are required"
            );
        }

        if (request.getDonorName() == null ||
                request.getDonorName().isBlank()) {

            throw new IllegalArgumentException(
                    "Donor name is required"
            );
        }

        if (request.getMobileNumber() == null ||
                !request.getMobileNumber().matches("^[0-9]{10}$")) {

            throw new IllegalArgumentException(
                    "Valid 10-digit mobile number is required"
            );
        }

        if (request.getAmount() == null ||
                request.getAmount()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Donation amount must be greater than zero"
            );
        }

        if (request.getPaymentMode() == null ||
                request.getPaymentMode().isBlank()) {

            throw new IllegalArgumentException(
                    "Payment mode is required"
            );
        }

        String paymentMode =
                request.getPaymentMode()
                        .trim()
                        .toUpperCase();

        if (!paymentMode.equals("CASH") &&
                !paymentMode.equals("UPI") &&
                !paymentMode.equals("BANK TRANSFER")) {

            throw new IllegalArgumentException(
                    "Invalid offline payment mode"
            );
        }

        Donation donation = new Donation();

        donation.setDonorName(
                request.getDonorName().trim()
        );

        donation.setMobileNumber(
                request.getMobileNumber().trim()
        );

        donation.setAmount(
                request.getAmount()
        );

        donation.setPaymentMode(
                paymentMode
        );

        // Internal transaction ID for offline donations
        donation.setTransactionId(
                "OFFLINE-" +
                UUID.randomUUID()
                        .toString()
                        .substring(0, 8)
                        .toUpperCase()
        );

        // Offline donations don't have Razorpay order IDs
        donation.setRazorpayOrderId(null);

        if (request.getReferenceNumber() != null &&
                !request.getReferenceNumber().isBlank()) {

            donation.setReferenceNumber(
                    request.getReferenceNumber().trim()
            );
        }

        if (request.getNotes() != null &&
                !request.getNotes().isBlank()) {

            donation.setNotes(
                    request.getNotes().trim()
            );
        }

        donation.setDonationDate(
                LocalDateTime.now()
        );

        donation.setPaymentStatus(
                "SUCCESS"
        );

        Donation savedDonation =
                donationRepository.save(donation);

        return Map.of(
                "success", true,
                "message",
                "Offline donation added successfully",

                "donationId",
                savedDonation.getId(),

                "transactionId",
                savedDonation.getTransactionId(),

                "amount",
                savedDonation.getAmount(),

                "paymentMode",
                savedDonation.getPaymentMode()
        );
    }
}