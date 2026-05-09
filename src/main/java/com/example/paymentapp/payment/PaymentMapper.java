package com.example.paymentapp.payment;

import com.example.paymentapp.api.PaymentAttemptResponse;
import com.example.paymentapp.api.PaymentResponse;
import com.example.paymentapp.domain.Payment;
import com.example.paymentapp.domain.PaymentAttempt;
import java.util.List;

public final class PaymentMapper {

    private PaymentMapper() {}

    public static PaymentResponse toResponse(Payment payment, List<PaymentAttempt> attempts) {
        return new PaymentResponse(
                payment.getId(),
                payment.getState(),
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference(),
                payment.getIdempotencyKey(),
                payment.getAttemptCount(),
                payment.getMaxAttempts(),
                payment.getNextRetryAt(),
                payment.getLastError(),
                payment.getPaymentServiceResponseId(),
                payment.getPaymentServiceStatus(),
                payment.getCreatedAt(),
                payment.getUpdatedAt(),
                payment.getCompletedAt(),
                "/payments/" + payment.getId(),
                attempts.stream().map(PaymentMapper::toResponse).toList());
    }

    public static PaymentAttemptResponse toResponse(PaymentAttempt attempt) {
        return new PaymentAttemptResponse(
                attempt.getId(),
                attempt.getAttemptNumber(),
                attempt.getStartedAt(),
                attempt.getFinishedAt(),
                attempt.getResult(),
                attempt.getHttpStatus(),
                attempt.getDurationMs(),
                attempt.getErrorMessage());
    }
}

