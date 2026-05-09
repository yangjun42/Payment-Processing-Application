package com.example.paymentapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
public class Payment {

    @Id
    private UUID id;

    @Column(name = "idempotency_key", unique = true)
    private String idempotencyKey;

    @Column(name = "request_hash", nullable = false)
    private String requestHash;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PaymentState state;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "max_attempts", nullable = false)
    private int maxAttempts;

    @Column(name = "next_retry_at")
    private Instant nextRetryAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "payment_service_response_id")
    private String paymentServiceResponseId;

    @Column(name = "payment_service_status")
    private String paymentServiceStatus;

    @Column(name = "payment_service_response", columnDefinition = "text")
    private String paymentServiceResponse;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Version
    private long version;

    protected Payment() {}

    public Payment(UUID id, String idempotencyKey, String requestHash, BigDecimal amount, String currency,
                   String reference, int maxAttempts, Instant now) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
        this.amount = amount;
        this.currency = currency;
        this.reference = reference;
        this.state = PaymentState.RECEIVED;
        this.maxAttempts = maxAttempts;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public boolean transitionTo(PaymentState next, Instant now) {
        if (state == next) {
            touch(now);
            return true;
        }
        if (!state.canTransitionTo(next)) {
            return false;
        }
        this.state = next;
        touch(now);
        return true;
    }

    public void startAttempt(Instant now) {
        if (state.isTerminal()) {
            return;
        }
        transitionTo(PaymentState.IN_PROGRESS, now);
        attemptCount++;
        lastError = null;
        nextRetryAt = null;
    }

    public void complete(String responseId, String status, String rawResponse, Instant now) {
        paymentServiceResponseId = responseId;
        paymentServiceStatus = status;
        paymentServiceResponse = rawResponse;
        completedAt = now;
        transitionTo(PaymentState.COMPLETED, now);
    }

    public void failTerminal(String status, String rawResponse, String error, Instant now) {
        paymentServiceStatus = status;
        paymentServiceResponse = rawResponse;
        lastError = error;
        completedAt = now;
        transitionTo(PaymentState.FAILED, now);
    }

    public void retryLater(String error, Instant nextRetryAt, Instant now) {
        lastError = error;
        this.nextRetryAt = nextRetryAt;
        transitionTo(PaymentState.RETRY_PENDING, now);
    }

    public void markEnqueued(Instant now) {
        if (!state.isTerminal()) {
            transitionTo(PaymentState.ENQUEUED, now);
        }
    }

    public void touch(Instant now) {
        updatedAt = now;
    }

    public UUID getId() { return id; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public String getRequestHash() { return requestHash; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public String getReference() { return reference; }
    public PaymentState getState() { return state; }
    public int getAttemptCount() { return attemptCount; }
    public int getMaxAttempts() { return maxAttempts; }
    public Instant getNextRetryAt() { return nextRetryAt; }
    public String getLastError() { return lastError; }
    public String getPaymentServiceResponseId() { return paymentServiceResponseId; }
    public String getPaymentServiceStatus() { return paymentServiceStatus; }
    public String getPaymentServiceResponse() { return paymentServiceResponse; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public long getVersion() { return version; }
}

