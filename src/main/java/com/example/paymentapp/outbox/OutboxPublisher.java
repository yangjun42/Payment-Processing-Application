package com.example.paymentapp.outbox;

import com.example.paymentapp.config.PaymentRabbitProperties;
import com.example.paymentapp.domain.PaymentState;
import com.example.paymentapp.payment.PaymentRepository;
import com.example.paymentapp.worker.PaymentWorkMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Profile("!simulator")
@Component
public class OutboxPublisher {

    private final OutboxEventRepository outboxEvents;
    private final PaymentRepository payments;
    private final RabbitTemplate rabbitTemplate;
    private final PaymentRabbitProperties rabbitProperties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OutboxPublisher(
            OutboxEventRepository outboxEvents,
            PaymentRepository payments,
            RabbitTemplate rabbitTemplate,
            PaymentRabbitProperties rabbitProperties,
            ObjectMapper objectMapper,
            Clock clock) {
        this.outboxEvents = outboxEvents;
        this.payments = payments;
        this.rabbitTemplate = rabbitTemplate;
        this.rabbitProperties = rabbitProperties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${payment.outbox.publish-delay:PT1S}")
    @Transactional
    public void publishDueEvents() {
        Instant now = clock.instant();
        List<OutboxEvent> dueEvents = outboxEvents.findTop20ByStatusInAndNextPublishAtBeforeOrderByCreatedAtAsc(
                List.of(OutboxStatus.NEW, OutboxStatus.FAILED), now);
        for (OutboxEvent event : dueEvents) {
            publish(event, now);
        }
    }

    private void publish(OutboxEvent event, Instant now) {
        try {
            PaymentWorkMessage message = objectMapper.readValue(event.getPayload(), PaymentWorkMessage.class);
            rabbitTemplate.convertAndSend(rabbitProperties.exchange(), rabbitProperties.routingKey(), message);
            event.markPublished(now);
            markPaymentEnqueued(event, now);
        } catch (Exception exception) {
            event.markFailed(exception.getMessage(), now.plus(Duration.ofSeconds(2)));
        }
    }

    private void markPaymentEnqueued(OutboxEvent event, Instant now) {
        payments.updateStateIfCurrentStateIn(
                event.getAggregateId(),
                PaymentState.ENQUEUED,
                List.of(PaymentState.RECEIVED, PaymentState.RETRY_PENDING),
                now);
    }
}
