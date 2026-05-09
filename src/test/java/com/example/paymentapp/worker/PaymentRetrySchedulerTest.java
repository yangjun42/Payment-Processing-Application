package com.example.paymentapp.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentapp.domain.Payment;
import com.example.paymentapp.domain.PaymentState;
import com.example.paymentapp.outbox.OutboxEvent;
import com.example.paymentapp.outbox.OutboxEventRepository;
import com.example.paymentapp.payment.PaymentProcessingProperties;
import com.example.paymentapp.payment.PaymentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentRetrySchedulerTest {

    private static final Instant NOW = Instant.parse("2026-05-08T12:00:00Z");

    @Mock
    private PaymentRepository payments;

    @Mock
    private OutboxEventRepository outboxEvents;

    private PaymentRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        PaymentProcessingProperties properties = new PaymentProcessingProperties(
                3, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(30), Duration.ofSeconds(30), "http://simulator");
        scheduler = new PaymentRetryScheduler(
                payments,
                outboxEvents,
                properties,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void dueRetryCreatesOutboxEventAndMarksPaymentEnqueued() {
        Payment payment = newPayment();
        payment.markEnqueued(NOW);
        payment.startAttempt(NOW);
        payment.retryLater("timeout", NOW.minusSeconds(1), NOW);
        when(payments.findTop50ByStateAndNextRetryAtBeforeOrderByUpdatedAtAsc(eq(PaymentState.RETRY_PENDING), eq(NOW)))
                .thenReturn(List.of(payment));

        scheduler.requeueDueRetries();

        assertThat(payment.getState()).isEqualTo(PaymentState.ENQUEUED);
        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEvents).save(eventCaptor.capture());
        assertThat(eventCaptor.getValue().getEventType()).isEqualTo("PaymentRetryDue");
        assertThat(eventCaptor.getValue().getPayload()).contains(payment.getId().toString());
        verify(payments).save(payment);
    }

    private Payment newPayment() {
        return new Payment(
                UUID.randomUUID(),
                "key-1",
                "hash",
                new BigDecimal("10.00"),
                "EUR",
                "INV-1",
                3,
                NOW);
    }
}
