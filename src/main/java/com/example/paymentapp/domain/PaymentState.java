package com.example.paymentapp.domain;

import java.util.Set;

public enum PaymentState {
    RECEIVED,
    ENQUEUED,
    IN_PROGRESS,
    RETRY_PENDING,
    COMPLETED,
    FAILED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED;
    }

    public boolean canTransitionTo(PaymentState next) {
        if (isTerminal()) {
            return false;
        }
        return allowedTargets().contains(next);
    }

    private Set<PaymentState> allowedTargets() {
        return switch (this) {
            case RECEIVED -> Set.of(ENQUEUED, IN_PROGRESS, RETRY_PENDING, FAILED);
            case ENQUEUED -> Set.of(IN_PROGRESS, RETRY_PENDING, FAILED);
            case IN_PROGRESS -> Set.of(COMPLETED, FAILED, RETRY_PENDING);
            case RETRY_PENDING -> Set.of(ENQUEUED, IN_PROGRESS, FAILED);
            case COMPLETED, FAILED -> Set.of();
        };
    }
}

