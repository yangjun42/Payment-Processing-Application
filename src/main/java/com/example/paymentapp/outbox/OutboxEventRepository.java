package com.example.paymentapp.outbox;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, UUID> {

    List<OutboxEvent> findTop20ByStatusInAndNextPublishAtBeforeOrderByCreatedAtAsc(
            Collection<OutboxStatus> statuses, Instant now);
}

