package com.example.paymentapp.payment;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.processing")
public record PaymentProcessingProperties(
        int maxAttempts,
        Duration initialBackoff,
        double backoffMultiplier,
        Duration maxBackoff,
        Duration staleInProgressAfter,
        String paymentServiceBaseUrl) {

    public PaymentProcessingProperties {
        if (maxAttempts <= 0) {
            maxAttempts = 3;
        }
        if (initialBackoff == null) {
            initialBackoff = Duration.ofSeconds(1);
        }
        if (backoffMultiplier < 1.0) {
            backoffMultiplier = 2.0;
        }
        if (maxBackoff == null) {
            maxBackoff = Duration.ofSeconds(30);
        }
        if (staleInProgressAfter == null) {
            staleInProgressAfter = Duration.ofSeconds(30);
        }
        if (paymentServiceBaseUrl == null || paymentServiceBaseUrl.isBlank()) {
            paymentServiceBaseUrl = "http://localhost:8081";
        }
    }

    public RetryPolicy retryPolicy() {
        return new RetryPolicy(maxAttempts, initialBackoff, backoffMultiplier, maxBackoff);
    }
}

