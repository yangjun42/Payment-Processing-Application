package com.example.paymentapp.api;

import com.example.paymentapp.domain.PaymentState;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentResponse(
        UUID paymentId,
        PaymentState state,
        BigDecimal amount,
        String currency,
        String reference,
        String idempotencyKey,
        int attemptCount,
        int maxAttempts,
        Instant nextRetryAt,
        String lastError,
        String paymentServiceResponseId,
        String paymentServiceStatus,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt,
        String statusUrl,
        List<PaymentAttemptResponse> attempts) {}

