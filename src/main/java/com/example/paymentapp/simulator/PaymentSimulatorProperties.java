package com.example.paymentapp.simulator;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.simulator")
public record PaymentSimulatorProperties(
        Duration minDelay,
        Duration maxDelay,
        double technicalFailureRate,
        double businessDeclineRate) {

    public PaymentSimulatorProperties {
        if (minDelay == null) {
            minDelay = Duration.ofMillis(10);
        }
        if (maxDelay == null) {
            maxDelay = Duration.ofSeconds(2);
        }
        technicalFailureRate = clamp(technicalFailureRate);
        businessDeclineRate = clamp(businessDeclineRate);
    }

    private static double clamp(double value) {
        return Math.max(0.0, Math.min(1.0, value));
    }
}

