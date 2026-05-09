package com.example.paymentapp.api;

import com.example.paymentapp.payment.PaymentIntakeService;
import com.example.paymentapp.payment.PaymentQueryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Profile("!simulator")
@RestController
@RequestMapping("/payments")
public class PaymentController {

    private final PaymentIntakeService intakeService;
    private final PaymentQueryService queryService;

    public PaymentController(PaymentIntakeService intakeService, PaymentQueryService queryService) {
        this.intakeService = intakeService;
        this.queryService = queryService;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> create(@Valid @RequestBody PaymentCreateRequest request) {
        PaymentResponse response = intakeService.createPayment(request);
        URI statusUri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{paymentId}")
                .buildAndExpand(response.paymentId())
                .toUri();
        return ResponseEntity.accepted().location(statusUri).body(response);
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable UUID paymentId) {
        return queryService.getPayment(paymentId);
    }

    @GetMapping("/{paymentId}/attempts")
    public java.util.List<PaymentAttemptResponse> attempts(@PathVariable UUID paymentId) {
        return queryService.getAttempts(paymentId);
    }
}

