package com.example.paymentapp.payment;

import com.example.paymentapp.api.PaymentCreateRequest;
import com.example.paymentapp.api.PaymentResponse;
import com.example.paymentapp.domain.Payment;
import com.example.paymentapp.outbox.OutboxEvent;
import com.example.paymentapp.outbox.OutboxEventRepository;
import com.example.paymentapp.worker.PaymentWorkMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("!simulator")
@Service
public class PaymentIntakeService {

    private final PaymentRepository payments;
    private final PaymentAttemptRepository attempts;
    private final OutboxEventRepository outboxEvents;
    private final PaymentProcessingProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PaymentIntakeService(
            PaymentRepository payments,
            PaymentAttemptRepository attempts,
            OutboxEventRepository outboxEvents,
            PaymentProcessingProperties properties,
            ObjectMapper objectMapper,
            Clock clock) {
        this.payments = payments;
        this.attempts = attempts;
        this.outboxEvents = outboxEvents;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public PaymentResponse createPayment(PaymentCreateRequest request) {
        String requestHash = RequestHasher.hash(request);
        if (request.idempotencyKey() != null && !request.idempotencyKey().isBlank()) {
            Payment existing = payments.findByIdempotencyKey(request.idempotencyKey()).orElse(null);
            if (existing != null) {
                if (!existing.getRequestHash().equals(requestHash)) {
                    throw new IdempotencyConflictException("Idempotency key already exists for a different request");
                }
                return PaymentMapper.toResponse(existing, attempts.findByPaymentIdOrderByAttemptNumberAsc(existing.getId()));
            }
        }

        Instant now = clock.instant();
        Payment payment = new Payment(
                UUID.randomUUID(),
                normalizeBlank(request.idempotencyKey()),
                requestHash,
                request.amount(),
                request.currency().toUpperCase(Locale.ROOT),
                request.reference(),
                properties.maxAttempts(),
                now);
        payments.save(payment);
        outboxEvents.save(new OutboxEvent(
                payment.getId(),
                "PaymentReceived",
                serialize(new PaymentWorkMessage(payment.getId())),
                now));
        return PaymentMapper.toResponse(payment, List.of());
    }

    private String normalizeBlank(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private String serialize(PaymentWorkMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize outbox event", exception);
        }
    }
}

