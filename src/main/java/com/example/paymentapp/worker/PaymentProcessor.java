package com.example.paymentapp.worker;

import com.example.paymentapp.domain.AttemptResult;
import com.example.paymentapp.domain.Payment;
import com.example.paymentapp.domain.PaymentAttempt;
import com.example.paymentapp.payment.PaymentAttemptRepository;
import com.example.paymentapp.payment.PaymentNotFoundException;
import com.example.paymentapp.payment.PaymentProcessingProperties;
import com.example.paymentapp.payment.PaymentRepository;
import com.example.paymentapp.payment.RetryPolicy;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("!simulator")
@Service
public class PaymentProcessor {

    private final PaymentRepository payments;
    private final PaymentAttemptRepository attempts;
    private final ExternalPaymentClient externalPaymentClient;
    private final RetryPolicy retryPolicy;
    private final Clock clock;

    public PaymentProcessor(
            PaymentRepository payments,
            PaymentAttemptRepository attempts,
            ExternalPaymentClient externalPaymentClient,
            PaymentProcessingProperties properties,
            Clock clock) {
        this.payments = payments;
        this.attempts = attempts;
        this.externalPaymentClient = externalPaymentClient;
        this.retryPolicy = properties.retryPolicy();
        this.clock = clock;
    }

    @Transactional
    public void process(UUID paymentId) {
        Payment payment = payments.findById(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));
        if (payment.getState().isTerminal()) {
            return;
        }

        Instant startedAt = clock.instant();
        payment.startAttempt(startedAt);
        PaymentAttempt attempt = new PaymentAttempt(payment, payment.getAttemptCount(), startedAt);

        try {
            ExternalPaymentResult result = externalPaymentClient.charge(payment);
            Instant finishedAt = clock.instant();
            if (result.approved()) {
                payment.complete(result.externalPaymentId(), result.status(), result.rawBody(), finishedAt);
                attempt.finish(AttemptResult.SUCCESS, result.httpStatus(), null, result.rawBody(), finishedAt);
            } else {
                String error = result.message() == null ? "Payment declined" : result.message();
                payment.failTerminal(result.status(), result.rawBody(), error, finishedAt);
                attempt.finish(AttemptResult.BUSINESS_FAILURE, result.httpStatus(), error, result.rawBody(), finishedAt);
            }
        } catch (TechnicalPaymentException exception) {
            handleTechnicalFailure(payment, attempt, exception.httpStatus(), exception.getMessage(), exception.rawBody());
        } catch (RuntimeException exception) {
            handleTechnicalFailure(payment, attempt, null, exception.getMessage(), null);
        }

        attempts.save(attempt);
        payments.save(payment);
    }

    private void handleTechnicalFailure(
            Payment payment, PaymentAttempt attempt, Integer httpStatus, String errorMessage, String rawBody) {
        Instant finishedAt = clock.instant();
        attempt.finish(AttemptResult.TECHNICAL_FAILURE, httpStatus, errorMessage, rawBody, finishedAt);
        if (retryPolicy.shouldRetry(payment.getAttemptCount())) {
            payment.retryLater(errorMessage, finishedAt.plus(retryPolicy.nextDelay(payment.getAttemptCount())), finishedAt);
        } else {
            payment.failTerminal("TECHNICAL_FAILURE", rawBody, errorMessage, finishedAt);
        }
    }
}

