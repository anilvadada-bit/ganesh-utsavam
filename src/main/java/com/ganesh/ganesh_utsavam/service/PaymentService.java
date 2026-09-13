package com.ganesh.ganesh_utsavam.service;

import com.ganesh.ganesh_utsavam.dto.PaymentVerificationRequest;
import com.ganesh.ganesh_utsavam.entity.Donation;
import com.ganesh.ganesh_utsavam.repository.DonationRepository;
import com.razorpay.Order;
import com.razorpay.Payment;
import com.razorpay.RazorpayClient;
import com.razorpay.Utils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class PaymentService {

    private final RazorpayClient razorpayClient;
    private final String keySecret;
    private final DonationRepository donationRepository;

    public PaymentService(
            @Value("${razorpay.key.id}") String keyId,
            @Value("${razorpay.key.secret}") String keySecret,
            DonationRepository donationRepository) throws Exception {

        this.razorpayClient = new RazorpayClient(keyId, keySecret);
        this.keySecret = keySecret;
        this.donationRepository = donationRepository;
    }

    /*
     * ============================================================
     * CREATE RAZORPAY ORDER
     * ============================================================
     */
    public String createOrder(BigDecimal amount) throws Exception {
        if (amount == null) {
    throw new IllegalArgumentException(
            "Donation amount is required"
    );
}

if (amount.compareTo(BigDecimal.valueOf(10)) < 0) {
    throw new IllegalArgumentException(
            "Minimum donation amount is ₹10"
    );
}

if (amount.compareTo(BigDecimal.valueOf(100000)) > 0) {
    throw new IllegalArgumentException(
            "Maximum donation amount is ₹1,00,000"
    );
}

  

        long amountInPaise = amount
                .multiply(BigDecimal.valueOf(100))
                .longValueExact();

        JSONObject orderRequest = new JSONObject();

        orderRequest.put("amount", amountInPaise);
        orderRequest.put("currency", "INR");
        orderRequest.put(
                "receipt",
                "GANESH_" + System.currentTimeMillis()
        );

        Order order =
                razorpayClient.orders.create(orderRequest);

        return order.toString();
    }

    /*
     * ============================================================
     * VERIFY RAZORPAY PAYMENT
     * ============================================================
     */
    public Donation verifyPayment(
            PaymentVerificationRequest request) throws Exception {

        /*
         * STEP 1: Validate request
         */

        if (request == null) {
            throw new IllegalArgumentException(
                    "Payment verification request is required"
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

        if (request.getRazorpayOrderId() == null ||
                request.getRazorpayOrderId().isBlank()) {

            throw new IllegalArgumentException(
                    "Razorpay order ID is required"
            );
        }

        if (request.getRazorpayPaymentId() == null ||
                request.getRazorpayPaymentId().isBlank()) {

            throw new IllegalArgumentException(
                    "Razorpay payment ID is required"
            );
        }

        if (request.getRazorpaySignature() == null ||
                request.getRazorpaySignature().isBlank()) {

            throw new IllegalArgumentException(
                    "Razorpay signature is required"
            );
        }

        /*
         * STEP 2: Verify Razorpay signature
         */

        JSONObject paymentData = new JSONObject();

        paymentData.put(
                "razorpay_order_id",
                request.getRazorpayOrderId()
        );

        paymentData.put(
                "razorpay_payment_id",
                request.getRazorpayPaymentId()
        );

        paymentData.put(
                "razorpay_signature",
                request.getRazorpaySignature()
        );

        boolean signatureValid =
                Utils.verifyPaymentSignature(
                        paymentData,
                        keySecret
                );

        if (!signatureValid) {

            throw new IllegalArgumentException(
                    "Payment signature verification failed"
            );
        }

        /*
         * STEP 3: Fetch Razorpay order
         */

        Order razorpayOrder =
                razorpayClient.orders.fetch(
                        request.getRazorpayOrderId()
                );

        /*
         * IMPORTANT:
         * Use .toString() on the returned Object.
         *
         * Do NOT use String.valueOf() here because of the
         * ClassCastException encountered with this SDK.
         */

        Object orderCurrencyObject =
                razorpayOrder.get("currency");

        String orderCurrency =
                orderCurrencyObject == null
                        ? null
                        : orderCurrencyObject.toString();

        if (!"INR".equalsIgnoreCase(orderCurrency)) {

            throw new IllegalArgumentException(
                    "Invalid payment currency"
            );
        }

        /*
         * Get server-side order amount.
         */

        Object orderAmountObject =
                razorpayOrder.get("amount");

        if (!(orderAmountObject instanceof Number)) {

            throw new IllegalArgumentException(
                    "Invalid Razorpay order amount"
            );
        }

        long orderAmountInPaise =
                ((Number) orderAmountObject).longValue();

        if (orderAmountInPaise <= 0) {

            throw new IllegalArgumentException(
                    "Invalid Razorpay order amount"
            );
        }

        BigDecimal verifiedAmount =
                BigDecimal.valueOf(orderAmountInPaise)
                        .divide(BigDecimal.valueOf(100));

        /*
         * STEP 4: Fetch actual Razorpay payment
         */

        Payment razorpayPayment =
                razorpayClient.payments.fetch(
                        request.getRazorpayPaymentId()
                );

        /*
         * STEP 5: Verify payment belongs to this order
         */

        Object paymentOrderIdObject =
                razorpayPayment.get("order_id");

        String paymentOrderId =
                paymentOrderIdObject == null
                        ? null
                        : paymentOrderIdObject.toString();

        if (paymentOrderId == null ||
                !request.getRazorpayOrderId()
                        .equals(paymentOrderId)) {

            throw new IllegalArgumentException(
                    "Payment does not belong to this order"
            );
        }

        /*
         * STEP 6: Verify payment amount
         */

        Object paymentAmountObject =
                razorpayPayment.get("amount");

        if (!(paymentAmountObject instanceof Number)) {

            throw new IllegalArgumentException(
                    "Invalid Razorpay payment amount"
            );
        }

        long paymentAmountInPaise =
                ((Number) paymentAmountObject).longValue();

        if (paymentAmountInPaise != orderAmountInPaise) {

            throw new IllegalArgumentException(
                    "Payment amount does not match the order amount"
            );
        }

        /*
         * STEP 7: Verify payment currency
         */

        Object paymentCurrencyObject =
                razorpayPayment.get("currency");

        String paymentCurrency =
                paymentCurrencyObject == null
                        ? null
                        : paymentCurrencyObject.toString();

        if (!"INR".equalsIgnoreCase(paymentCurrency)) {

            throw new IllegalArgumentException(
                    "Invalid payment currency"
            );
        }

        /*
         * STEP 8: Verify payment status
         *
         * Only captured payments are considered successful.
         */

        Object paymentStatusObject =
                razorpayPayment.get("status");

        String paymentStatus =
                paymentStatusObject == null
                        ? null
                        : paymentStatusObject.toString();

        if (!"captured".equalsIgnoreCase(paymentStatus)) {

            throw new IllegalArgumentException(
                    "Payment has not been captured"
            );
        }

        /*
         * STEP 9: Prevent duplicate payment records
         */

        if (donationRepository
                .findByTransactionId(
                        request.getRazorpayPaymentId()
                )
                .isPresent()) {

            return donationRepository
                    .findByTransactionId(
                            request.getRazorpayPaymentId()
                    )
                    .orElseThrow();
        }

        /*
         * STEP 10: Save successful donation
         */

        Donation donation = new Donation();

        donation.setDonorName(
                request.getDonorName().trim()
        );

        donation.setMobileNumber(
                request.getMobileNumber().trim()
        );

        /*
         * Amount comes from the verified Razorpay order.
         * It is NOT taken from the browser.
         */

        donation.setAmount(verifiedAmount);

        donation.setPaymentMode("RAZORPAY");

        donation.setTransactionId(
                request.getRazorpayPaymentId()
        );

        donation.setRazorpayOrderId(
                request.getRazorpayOrderId()
        );

        donation.setDonationDate(
                LocalDateTime.now()
        );

        donation.setPaymentStatus("SUCCESS");

        return donationRepository.save(donation);
    }
}