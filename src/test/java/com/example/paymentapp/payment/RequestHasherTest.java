package com.example.paymentapp.payment;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.paymentapp.api.PaymentCreateRequest;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class RequestHasherTest {

    @Test
    void producesStableHashForEquivalentPaymentRequests() {
        PaymentCreateRequest first = new PaymentCreateRequest(
                new BigDecimal("12.30"), "EUR", "invoice-123", "same-key");
        PaymentCreateRequest second = new PaymentCreateRequest(
                new BigDecimal("12.300"), "eur", "invoice-123", "same-key");

        assertThat(RequestHasher.hash(first)).isEqualTo(RequestHasher.hash(second));
    }

    @Test
    void changesHashWhenPaymentMeaningChanges() {
        PaymentCreateRequest first = new PaymentCreateRequest(
                new BigDecimal("12.30"), "EUR", "invoice-123", "same-key");
        PaymentCreateRequest second = new PaymentCreateRequest(
                new BigDecimal("12.31"), "EUR", "invoice-123", "same-key");

        assertThat(RequestHasher.hash(first)).isNotEqualTo(RequestHasher.hash(second));
    }
}

