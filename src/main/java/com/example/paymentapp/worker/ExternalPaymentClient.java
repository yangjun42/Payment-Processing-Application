package com.example.paymentapp.worker;

import com.example.paymentapp.domain.Payment;
import com.example.paymentapp.payment.PaymentProcessingProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

@Profile("!simulator")
@Component
public class ExternalPaymentClient {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public ExternalPaymentClient(RestClient.Builder builder, PaymentProcessingProperties properties, ObjectMapper objectMapper) {
        this.restClient = builder.baseUrl(properties.paymentServiceBaseUrl()).build();
        this.objectMapper = objectMapper;
    }

    public ExternalPaymentResult charge(Payment payment) {
        String idempotencyKey = Optional.ofNullable(payment.getIdempotencyKey()).orElse(payment.getId().toString());
        ExternalPaymentRequest request = new ExternalPaymentRequest(
                payment.getId(),
                idempotencyKey,
                payment.getAmount(),
                payment.getCurrency(),
                payment.getReference());
        try {
            ResponseEntity<ExternalPaymentResponse> response = restClient.post()
                    .uri("/simulator/payments")
                    .header("Idempotency-Key", idempotencyKey)
                    .body(request)
                    .retrieve()
                    .toEntity(ExternalPaymentResponse.class);
            ExternalPaymentResponse body = response.getBody();
            if (body == null) {
                throw new TechnicalPaymentException("Payment Service returned an empty body",
                        response.getStatusCode().value(), null);
            }
            return new ExternalPaymentResult(
                    body.externalPaymentId(),
                    body.status(),
                    body.message(),
                    response.getStatusCode().value(),
                    serialize(body));
        } catch (RestClientResponseException exception) {
            if (isRetryable(exception.getStatusCode())) {
                throw new TechnicalPaymentException(
                        "Retryable Payment Service failure: " + exception.getStatusCode().value(),
                        exception.getStatusCode().value(),
                        exception.getResponseBodyAsString());
            }
            return new ExternalPaymentResult(
                    null,
                    "DECLINED",
                    "Non-retryable Payment Service response: " + exception.getStatusCode().value(),
                    exception.getStatusCode().value(),
                    exception.getResponseBodyAsString());
        }
    }

    private boolean isRetryable(HttpStatusCode statusCode) {
        return statusCode.is5xxServerError() || statusCode.value() == 429;
    }

    private String serialize(ExternalPaymentResponse body) {
        try {
            return objectMapper.writeValueAsString(body);
        } catch (JsonProcessingException exception) {
            return body.toString();
        }
    }
}

