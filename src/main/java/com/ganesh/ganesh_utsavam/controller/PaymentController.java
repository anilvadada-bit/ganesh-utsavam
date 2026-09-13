package com.ganesh.ganesh_utsavam.controller;

import com.ganesh.ganesh_utsavam.dto.PaymentVerificationRequest;
import com.ganesh.ganesh_utsavam.entity.Donation;
import com.ganesh.ganesh_utsavam.service.PaymentService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/create-order")
    @ResponseStatus(HttpStatus.CREATED)
    public String createOrder(
            @RequestParam BigDecimal amount) throws Exception {

        return paymentService.createOrder(amount);
    }

    @PostMapping("/verify")
    public Map<String, Object> verifyPayment(
            @RequestBody PaymentVerificationRequest request)
            throws Exception {

        Donation donation =
                paymentService.verifyPayment(request);

        return Map.of(
                "success", true,
                "message",
                "Payment verified and donation saved successfully",

                "donationId",
                donation.getId(),

                "paymentId",
                donation.getTransactionId(),

                "orderId",
                donation.getRazorpayOrderId(),

                "amount",
                donation.getAmount(),

                "status",
                donation.getPaymentStatus()
        );
    }
}