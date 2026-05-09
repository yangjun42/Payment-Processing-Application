package com.example.paymentapp.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PaymentStateTest {

    @Test
    void terminalStatesCannotTransitionBackToProcessing() {
        assertThat(PaymentState.COMPLETED.canTransitionTo(PaymentState.IN_PROGRESS)).isFalse();
        assertThat(PaymentState.FAILED.canTransitionTo(PaymentState.RETRY_PENDING)).isFalse();
    }

    @Test
    void retryPendingCanBeRequeuedForProcessing() {
        assertThat(PaymentState.RETRY_PENDING.canTransitionTo(PaymentState.ENQUEUED)).isTrue();
        assertThat(PaymentState.ENQUEUED.canTransitionTo(PaymentState.IN_PROGRESS)).isTrue();
    }
}

