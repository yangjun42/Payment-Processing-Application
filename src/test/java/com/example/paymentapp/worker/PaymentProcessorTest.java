package com.example.paymentapp.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.paymentapp.domain.AttemptResult;
import com.example.paymentapp.domain.Payment;
import com.example.paymentapp.domain.PaymentAttempt;
import com.example.paymentapp.domain.PaymentState;
import com.example.paymentapp.payment.PaymentAttemptRepository;
import com.example.paymentapp.payment.PaymentProcessingProperties;
import com.example.paymentapp.payment.PaymentRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentProcessorTest {

    private static final Instant NOW = Instant.parse("2026-05-08T12:00:00Z");

    @Mock
    private PaymentRepository payments;

    @Mock
    private PaymentAttemptRepository attempts;

    @Mock
    private ExternalPaymentClient externalPaymentClient;

    private PaymentProcessor processor;

    @BeforeEach
    void setUp() {
        PaymentProcessingProperties properties = new PaymentProcessingProperties(
                3, Duration.ofSeconds(1), 2.0, Duration.ofSeconds(30), Duration.ofSeconds(30), "http://simulator");
        processor = new PaymentProcessor(
                payments,
                attempts,
                externalPaymentClient,
                properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void approvedExternalResultCompletesPayment() {
        Payment payment = newPayment();
        payment.markEnqueued(NOW);
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(externalPaymentClient.charge(payment))
                .thenReturn(new ExternalPaymentResult("ext-1", "APPROVED", null, 200, "{\"status\":\"APPROVED\"}"));

        processor.process(payment.getId());

        assertThat(payment.getState()).isEqualTo(PaymentState.COMPLETED);
        assertThat(payment.getPaymentServiceResponseId()).isEqualTo("ext-1");
        assertThat(payment.getPaymentServiceStatus()).isEqualTo("APPROVED");
        PaymentAttempt attempt = savedAttempt();
        assertThat(attempt.getResult()).isEqualTo(AttemptResult.SUCCESS);
        assertThat(attempt.getHttpStatus()).isEqualTo(200);
    }

    @Test
    void businessDeclineFailsTerminallyWithoutRetry() {
        Payment payment = newPayment();
        payment.markEnqueued(NOW);
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(externalPaymentClient.charge(payment))
                .thenReturn(new ExternalPaymentResult(null, "DECLINED", "Insufficient funds", 402, "{}"));

        processor.process(payment.getId());

        assertThat(payment.getState()).isEqualTo(PaymentState.FAILED);
        assertThat(payment.getLastError()).isEqualTo("Insufficient funds");
        assertThat(payment.getNextRetryAt()).isNull();
        assertThat(savedAttempt().getResult()).isEqualTo(AttemptResult.BUSINESS_FAILURE);
    }

    @Test
    void technicalFailureMovesPaymentToRetryPendingWhenAttemptsRemain() {
        Payment payment = newPayment();
        payment.markEnqueued(NOW);
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));
        when(externalPaymentClient.charge(payment))
                .thenThrow(new TechnicalPaymentException("Upstream timeout", 503, "unavailable"));

        processor.process(payment.getId());

        assertThat(payment.getState()).isEqualTo(PaymentState.RETRY_PENDING);
        assertThat(payment.getAttemptCount()).isEqualTo(1);
        assertThat(payment.getNextRetryAt()).isEqualTo(NOW.plusSeconds(1));
        assertThat(savedAttempt().getResult()).isEqualTo(AttemptResult.TECHNICAL_FAILURE);
    }

    @Test
    void terminalPaymentIsNoOpOnDuplicateDelivery() {
        Payment payment = newPayment();
        payment.failTerminal("DECLINED", "{}", "already failed", NOW);
        when(payments.findById(payment.getId())).thenReturn(Optional.of(payment));

        processor.process(payment.getId());

        verify(externalPaymentClient, never()).charge(any(Payment.class));
        verify(attempts, never()).save(any(PaymentAttempt.class));
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

    private PaymentAttempt savedAttempt() {
        ArgumentCaptor<PaymentAttempt> captor = ArgumentCaptor.forClass(PaymentAttempt.class);
        verify(attempts).save(captor.capture());
        return captor.getValue();
    }
}
