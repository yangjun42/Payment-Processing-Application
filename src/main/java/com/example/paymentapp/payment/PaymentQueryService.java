package com.example.paymentapp.payment;

import com.example.paymentapp.api.PaymentAttemptResponse;
import com.example.paymentapp.api.PaymentResponse;
import com.example.paymentapp.domain.Payment;
import java.util.List;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Profile("!simulator")
@Service
public class PaymentQueryService {

    private final PaymentRepository payments;
    private final PaymentAttemptRepository attempts;

    public PaymentQueryService(PaymentRepository payments, PaymentAttemptRepository attempts) {
        this.payments = payments;
        this.attempts = attempts;
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(UUID paymentId) {
        Payment payment = payments.findById(paymentId).orElseThrow(() -> new PaymentNotFoundException(paymentId));
        return PaymentMapper.toResponse(payment, attempts.findByPaymentIdOrderByAttemptNumberAsc(paymentId));
    }

    @Transactional(readOnly = true)
    public List<PaymentAttemptResponse> getAttempts(UUID paymentId) {
        if (!payments.existsById(paymentId)) {
            throw new PaymentNotFoundException(paymentId);
        }
        return attempts.findByPaymentIdOrderByAttemptNumberAsc(paymentId).stream()
                .map(PaymentMapper::toResponse)
                .toList();
    }
}

