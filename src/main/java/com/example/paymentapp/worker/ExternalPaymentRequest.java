package com.example.paymentapp.worker;

import java.math.BigDecimal;
import java.util.UUID;

public record ExternalPaymentRequest(
        UUID paymentId,
        String idempotencyKey,
        BigDecimal amount,
        String currency,
        String reference) {}

