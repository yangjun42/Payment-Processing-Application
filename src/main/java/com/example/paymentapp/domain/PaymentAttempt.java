package com.example.paymentapp.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_attempts")
public class PaymentAttempt {

    @Id
    private UUID id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "payment_id", nullable = false)
    private Payment payment;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Enumerated(EnumType.STRING)
    private AttemptResult result;

    @Column(name = "http_status")
    private Integer httpStatus;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "raw_response", columnDefinition = "text")
    private String rawResponse;

    protected PaymentAttempt() {}

    public PaymentAttempt(Payment payment, int attemptNumber, Instant startedAt) {
        this.id = UUID.randomUUID();
        this.payment = payment;
        this.attemptNumber = attemptNumber;
        this.startedAt = startedAt;
    }

    public void finish(AttemptResult result, Integer httpStatus, String errorMessage, String rawResponse, Instant finishedAt) {
        this.result = result;
        this.httpStatus = httpStatus;
        this.errorMessage = errorMessage;
        this.rawResponse = rawResponse;
        this.finishedAt = finishedAt;
        this.durationMs = finishedAt.toEpochMilli() - startedAt.toEpochMilli();
    }

    public UUID getId() { return id; }
    public Payment getPayment() { return payment; }
    public int getAttemptNumber() { return attemptNumber; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getFinishedAt() { return finishedAt; }
    public AttemptResult getResult() { return result; }
    public Integer getHttpStatus() { return httpStatus; }
    public Long getDurationMs() { return durationMs; }
    public String getErrorMessage() { return errorMessage; }
    public String getRawResponse() { return rawResponse; }
}

