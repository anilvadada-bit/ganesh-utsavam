const amountButtons = document.querySelectorAll(".amount-btn");
const amountInput = document.getElementById("amount");
const donationForm = document.getElementById("donationForm");
const message = document.getElementById("message");


// =========================
// QUICK AMOUNT BUTTONS
// =========================

amountButtons.forEach(button => {

    button.addEventListener("click", () => {

        const amount = button.getAttribute("data-amount");

        amountInput.value = amount;

    });

});


// =========================
// DONATION FORM
// =========================

donationForm.addEventListener("submit", async (event) => {

    event.preventDefault();

    const donorName =
        document.getElementById("donorName").value.trim();

    const mobileNumber =
        document.getElementById("mobileNumber").value.trim();

    const amount =
        Number(document.getElementById("amount").value);


    // =========================
    // VALIDATION
    // =========================

    if (!donorName) {

        message.textContent =
            "Please enter your name.";

        return;
    }


    if (!/^[0-9]{10}$/.test(mobileNumber)) {

        message.textContent =
            "Please enter a valid 10-digit mobile number.";

        return;
    }


    if (!Number.isFinite(amount) || amount <= 0) {

        message.textContent =
            "Please enter a valid donation amount.";

        return;
    }


    // =========================
    // CREATE RAZORPAY ORDER
    // =========================

    message.textContent =
        "Creating secure payment...";


    try {

        const response = await fetch(
            `/api/payments/create-order?amount=${encodeURIComponent(amount)}`,
            {
                method: "POST"
            }
        );


        if (!response.ok) {

            throw new Error(
                `Payment order failed: ${response.status}`
            );

        }


        const order = await response.json();

        console.log("Razorpay Order:", order);


        // =========================
        // RAZORPAY CHECKOUT
        // =========================

        const options = {

            // IMPORTANT:
            // Use your Razorpay TEST KEY ID.
            // NEVER put your Razorpay SECRET here.
            key: "rzp_test_TadEv9xAQzm4Bg",

            amount: order.amount,

            currency: order.currency,

            name: "శ్రీ శ్రీ మల్లీశ్వర సేవా సమితి",

            description:
                "Ganesh Utsavam Donation",

            order_id: order.id,


            prefill: {

                name: donorName,

                contact: mobileNumber

            },


            notes: {

                organization:
                    "శ్రీ శ్రీ మల్లీశ్వర సేవా సమితి",

                village:
                    "ఆలికాం – పొండర వీధి"

            },


            theme: {

                color: "#ff6b00"

            },


            // =========================
            // RAZORPAY SUCCESS
            // =========================

            handler: async function (paymentResponse) {

                console.log(
                    "Razorpay Payment Response:",
                    paymentResponse
                );


                message.innerHTML = `
                    <div class="success-message">

                        <h3>⏳ Verifying Payment...</h3>

                        <p>
                            Please wait while we securely
                            verify your payment.
                        </p>

                    </div>
                `;


                // =========================
                // SEND PAYMENT TO BACKEND
                // =========================

                const verificationData = {

                    donorName: donorName,

                    mobileNumber: mobileNumber,

                    amount: amount.toString(),

                    razorpayOrderId:
                        paymentResponse.razorpay_order_id,

                    razorpayPaymentId:
                        paymentResponse.razorpay_payment_id,

                    razorpaySignature:
                        paymentResponse.razorpay_signature

                };


                console.log(
                    "Sending payment for verification:",
                    verificationData
                );


                try {

                    const verificationResponse =
                        await fetch(
                            "/api/payments/verify",
                            {

                                method: "POST",

                                headers: {

                                    "Content-Type":
                                        "application/json"

                                },

                                body:
                                    JSON.stringify(
                                        verificationData
                                    )

                            }
                        );


                    if (!verificationResponse.ok) {

                        throw new Error(
                            `Verification request failed: ${verificationResponse.status}`
                        );

                    }


                    const verificationResult =
                        await verificationResponse.json();


                    console.log(
                        "Verification Result:",
                        verificationResult
                    );


                    // =========================
                    // PAYMENT VERIFIED
                    // =========================

                    

                    // =========================
                    // VERIFICATION FAILED
                    // =========================

                    if (verificationResult.success) {

    message.innerHTML = `
        <div class="success-message">
            <h3>🙏 Payment Verified!</h3>

            <p>
                Your payment has been securely verified.
            </p>

            <p>
                <strong>Payment ID:</strong>
                ${verificationResult.paymentId}
            </p>

            <p>
                <strong>Order ID:</strong>
                ${verificationResult.orderId}
            </p>

            <p>
                <strong>Amount:</strong>
                ₹${Number(
                    verificationResult.amount
                ).toFixed(2)}
            </p>

            <p>
                <strong>Donation ID:</strong>
                #${verificationResult.donationId}
            </p>

            <p>
                <strong>Status:</strong>
                ${verificationResult.status}
            </p>

            <p>
                🙏 Thank you for your generous contribution!
            </p>
        </div>
    `;

    donationForm.reset();

} else {

    message.innerHTML = `
        <div class="error-message">
            <h3>❌ Payment Verification Failed</h3>

            <p>
                We could not verify this payment.
            </p>

            <p>
                Please contact the organizer if
                money was deducted.
            </p>
        </div>
    `;
}

               

                } catch (error) {

                    console.error(
                        "Payment Verification Error:",
                        error
                    );


                    message.innerHTML = `

                        <div class="error-message">

                            <h3>⚠️ Verification Error</h3>

                            <p>
                                Payment was received, but we could
                                not complete verification.
                            </p>

                            <p>
                                Please contact the organizer if
                                money was deducted.
                            </p>

                        </div>

                    `;

                }

            },


            // =========================
            // PAYMENT CANCELLED
            // =========================

            modal: {

                ondismiss: function () {

                    message.textContent =
                        "Payment cancelled.";

                }

            }

        };


        // =========================
        // CREATE RAZORPAY INSTANCE
        // =========================

        const razorpay =
            new Razorpay(options);


        // =========================
        // PAYMENT FAILED
        // =========================

        razorpay.on(
            "payment.failed",
            function (response) {

                console.error(
                    "Payment Failed:",
                    response.error
                );


                message.innerHTML = `

                    <div class="error-message">

                        <h3>❌ Payment Failed</h3>

                        <p>
                            Please try again.
                        </p>

                        <p>
                            ${
                                response.error.description || ""
                            }
                        </p>

                    </div>

                `;

            }
        );


        // =========================
        // OPEN RAZORPAY
        // =========================

        razorpay.open();


    } catch (error) {

        console.error(
            "Payment Error:",
            error
        );


        message.innerHTML = `

            <div class="error-message">

                Something went wrong while
                creating the payment.

                <br>

                Please try again.

            </div>

        `;

    }

});