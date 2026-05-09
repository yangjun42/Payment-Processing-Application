package com.example.paymentapp.simulator;

import com.example.paymentapp.worker.ExternalPaymentRequest;
import com.example.paymentapp.worker.ExternalPaymentResponse;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("simulator")
@RestController
@RequestMapping("/simulator/payments")
public class SimulatorPaymentController {

    private final PaymentSimulatorProperties properties;
    private final Map<String, ExternalPaymentResponse> responsesByKey = new ConcurrentHashMap<>();

    public SimulatorPaymentController(PaymentSimulatorProperties properties) {
        this.properties = properties;
    }

    @PostMapping
    public ResponseEntity<ExternalPaymentResponse> create(
            @RequestHeader(name = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody ExternalPaymentRequest request) throws InterruptedException {
        delay();
        String key = idempotencyKey == null || idempotencyKey.isBlank()
                ? request.paymentId().toString()
                : idempotencyKey;
        if (responsesByKey.containsKey(key)) {
            return ResponseEntity.ok(responsesByKey.get(key));
        }
        if (shouldTrigger(properties.technicalFailureRate()) || request.reference().toUpperCase().contains("ERROR")) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ExternalPaymentResponse(null, "TECHNICAL_FAILURE", "simulated technical failure"));
        }
        ExternalPaymentResponse response = buildBusinessResponse(request);
        responsesByKey.put(key, response);
        return ResponseEntity.ok(response);
    }

    private ExternalPaymentResponse buildBusinessResponse(ExternalPaymentRequest request) {
        boolean decline = request.reference().toUpperCase().contains("DECLINE")
                || shouldTrigger(properties.businessDeclineRate());
        if (decline) {
            return new ExternalPaymentResponse(null, "DECLINED", "simulated business decline");
        }
        return new ExternalPaymentResponse(UUID.randomUUID().toString(), "APPROVED", "simulated approval");
    }

    private void delay() throws InterruptedException {
        long min = properties.minDelay().toMillis();
        long max = Math.max(min, properties.maxDelay().toMillis());
        long delay = ThreadLocalRandom.current().nextLong(min, max + 1);
        Thread.sleep(delay);
    }

    private boolean shouldTrigger(double rate) {
        return rate > 0.0 && ThreadLocalRandom.current().nextDouble() < rate;
    }
}

