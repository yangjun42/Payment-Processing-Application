package com.example.paymentapp.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class RetryPolicyTest {

    @Test
    void computesExponentialBackoffWithoutExceedingMaximumDelay() {
        RetryPolicy policy = new RetryPolicy(5, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(5));

        assertThat(policy.nextDelay(1)).isEqualTo(Duration.ofSeconds(1));
        assertThat(policy.nextDelay(2)).isEqualTo(Duration.ofSeconds(2));
        assertThat(policy.nextDelay(3)).isEqualTo(Duration.ofSeconds(4));
        assertThat(policy.nextDelay(4)).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void stopsRetryingAfterMaximumAttemptCount() {
        RetryPolicy policy = new RetryPolicy(3, Duration.ofMillis(500), 2.0, Duration.ofSeconds(5));

        assertThat(policy.shouldRetry(1)).isTrue();
        assertThat(policy.shouldRetry(2)).isTrue();
        assertThat(policy.shouldRetry(3)).isFalse();
    }
}

