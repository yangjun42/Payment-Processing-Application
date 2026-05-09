package com.example.paymentapp.payment;

import com.example.paymentapp.domain.Payment;
import com.example.paymentapp.domain.PaymentState;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdempotencyKey(String idempotencyKey);

    List<Payment> findTop50ByStateAndNextRetryAtBeforeOrderByUpdatedAtAsc(PaymentState state, Instant now);

    List<Payment> findTop50ByStateAndUpdatedAtBeforeOrderByUpdatedAtAsc(PaymentState state, Instant threshold);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update Payment p
            set p.state = :nextState,
                p.updatedAt = :now
            where p.id = :id
              and p.state in :currentStates
            """)
    int updateStateIfCurrentStateIn(
            @Param("id") UUID id,
            @Param("nextState") PaymentState nextState,
            @Param("currentStates") Collection<PaymentState> currentStates,
            @Param("now") Instant now);
}
