package com.example.paymentapp.worker;

import com.example.paymentapp.domain.Payment;
import com.example.paymentapp.domain.PaymentState;
import com.example.paymentapp.outbox.OutboxEvent;
import com.example.paymentapp.outbox.OutboxEventRepository;
import com.example.paymentapp.payment.PaymentProcessingProperties;
import com.example.paymentapp.payment.PaymentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Profile("!simulator")
@Component
public class PaymentRetryScheduler {

    private final PaymentRepository payments;
    private final OutboxEventRepository outboxEvents;
    private final PaymentProcessingProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public PaymentRetryScheduler(
            PaymentRepository payments,
            OutboxEventRepository outboxEvents,
            PaymentProcessingProperties properties,
            ObjectMapper objectMapper,
            Clock clock) {
        this.payments = payments;
        this.outboxEvents = outboxEvents;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${payment.retry.scan-delay:PT1S}")
    @Transactional
    public void requeueDueRetries() {
        Instant now = clock.instant();
        for (Payment payment : payments.findTop50ByStateAndNextRetryAtBeforeOrderByUpdatedAtAsc(
                PaymentState.RETRY_PENDING, now)) {
            outboxEvents.save(new OutboxEvent(
                    payment.getId(), "PaymentRetryDue", serialize(new PaymentWorkMessage(payment.getId())), now));
            payment.markEnqueued(now);
            payments.save(payment);
        }
    }

    @Scheduled(fixedDelayString = "${payment.retry.recovery-delay:PT5S}")
    @Transactional
    public void recoverStaleInProgress() {
        Instant now = clock.instant();
        Instant threshold = now.minus(properties.staleInProgressAfter());
        for (Payment payment : payments.findTop50ByStateAndUpdatedAtBeforeOrderByUpdatedAtAsc(
                PaymentState.IN_PROGRESS, threshold)) {
            payment.retryLater("Recovered stale IN_PROGRESS payment after app restart or worker failure",
                    now, now);
            payments.save(payment);
        }
    }

    private String serialize(PaymentWorkMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize retry outbox event", exception);
        }
    }
}
