package com.example.paymentapp.payment;

import java.time.Duration;

public record RetryPolicy(int maxAttempts, Duration initialBackoff, double multiplier, Duration maxBackoff) {

    public boolean shouldRetry(int completedAttemptCount) {
        return completedAttemptCount < maxAttempts;
    }

    public Duration nextDelay(int nextAttemptNumber) {
        int exponent = Math.max(0, nextAttemptNumber - 1);
        double rawMillis = initialBackoff.toMillis() * Math.pow(multiplier, exponent);
        long boundedMillis = Math.min((long) rawMillis, maxBackoff.toMillis());
        return Duration.ofMillis(boundedMillis);
    }
}

