package com.example.paymentapp.payment;

import com.example.paymentapp.domain.PaymentAttempt;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentAttemptRepository extends JpaRepository<PaymentAttempt, UUID> {

    List<PaymentAttempt> findByPaymentIdOrderByAttemptNumberAsc(UUID paymentId);
}

