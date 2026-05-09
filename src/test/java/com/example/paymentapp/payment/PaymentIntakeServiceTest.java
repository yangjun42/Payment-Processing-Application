package com.example.paymentapp.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentapp.api.PaymentCreateRequest;
import com.example.paymentapp.api.PaymentResponse;
import com.example.paymentapp.domain.Payment;
import com.example.paymentapp.domain.PaymentState;
import com.example.paymentapp.outbox.OutboxEvent;
import com.example.paymentapp.outbox.OutboxEventRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(MockitoExtension.class)
class PaymentIntakeServiceTest {

    private static final Instant NOW = Instant.parse("2026-05-08T12:00:00Z");

    @Mock
    private PaymentRepository payments;

    @Mock
    private PaymentAttemptRepository attempts;

    @Mock
    private OutboxEventRepository outboxEvents;

    private PaymentIntakeService service;

    @BeforeEach
    void setUp() {
        PaymentProcessingProperties properties = new PaymentProcessingProperties(
                3, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(30), Duration.ofSeconds(30), "http://simulator");
        service = new PaymentIntakeService(
                payments,
                attempts,
                outboxEvents,
                properties,
                new ObjectMapper(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createPaymentPersistsReceivedPaymentAndOutboxEvent() {
        PaymentCreateRequest request = new PaymentCreateRequest(
                new BigDecimal("12.50"), "eur", "INV-100", "key-100");

        PaymentResponse response = service.createPayment(request);

        assertThat(response.state()).isEqualTo(PaymentState.RECEIVED);
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.idempotencyKey()).isEqualTo("key-100");
        assertThat(response.maxAttempts()).isEqualTo(3);
        assertThat(response.statusUrl()).isEqualTo("/payments/" + response.paymentId());

        ArgumentCaptor<OutboxEvent> eventCaptor = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outboxEvents).save(eventCaptor.capture());
        OutboxEvent event = eventCaptor.getValue();
        assertThat(event.getAggregateId()).isEqualTo(response.paymentId());
        assertThat(event.getEventType()).isEqualTo("PaymentReceived");
        assertThat(event.getPayload()).contains(response.paymentId().toString());
    }

    @Test
    void duplicateIdempotencyKeyWithSamePayloadReturnsExistingPayment() {
        PaymentCreateRequest request = new PaymentCreateRequest(
                new BigDecimal("12.50"), "EUR", "INV-100", "key-100");
        Payment existing = new Payment(
                UUID.randomUUID(),
                "key-100",
                RequestHasher.hash(request),
                new BigDecimal("12.50"),
                "EUR",
                "INV-100",
                3,
                NOW);

        when(payments.findByIdempotencyKey("key-100")).thenReturn(Optional.of(existing));
        when(attempts.findByPaymentIdOrderByAttemptNumberAsc(existing.getId())).thenReturn(List.of());

        PaymentResponse response = service.createPayment(request);

        assertThat(response.paymentId()).isEqualTo(existing.getId());
        verify(payments, never()).save(any(Payment.class));
        verify(outboxEvents, never()).save(any(OutboxEvent.class));
    }

    @Test
    void duplicateIdempotencyKeyWithChangedPayloadIsRejected() {
        PaymentCreateRequest original = new PaymentCreateRequest(
                new BigDecimal("12.50"), "EUR", "INV-100", "key-100");
        Payment existing = new Payment(
                UUID.randomUUID(),
                "key-100",
                RequestHasher.hash(original),
                new BigDecimal("12.50"),
                "EUR",
                "INV-100",
                3,
                NOW);
        PaymentCreateRequest changed = new PaymentCreateRequest(
                new BigDecimal("13.00"), "EUR", "INV-100", "key-100");

        when(payments.findByIdempotencyKey("key-100")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.createPayment(changed))
                .isInstanceOf(IdempotencyConflictException.class);
        verify(outboxEvents, never()).save(any(OutboxEvent.class));
    }
}
