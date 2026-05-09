package com.example.paymentapp.api;

import com.example.paymentapp.domain.AttemptResult;
import java.time.Instant;
import java.util.UUID;

public record PaymentAttemptResponse(
        UUID id,
        int attemptNumber,
        Instant startedAt,
        Instant finishedAt,
        AttemptResult result,
        Integer httpStatus,
        Long durationMs,
        String errorMessage) {}

