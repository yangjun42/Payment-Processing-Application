package com.example.paymentapp.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.paymentapp.api.PaymentCreateRequest;
import com.example.paymentapp.api.PaymentResponse;
import com.example.paymentapp.domain.Payment;
import com.example.paymentapp.domain.PaymentState;
import com.example.paymentapp.outbox.OutboxEventRepository;
import com.example.paymentapp.outbox.OutboxPublisher;
import com.example.paymentapp.worker.ExternalPaymentClient;
import com.example.paymentapp.worker.ExternalPaymentResult;
import java.math.BigDecimal;
import java.time.Duration;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PaymentApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("payments")
            .withUsername("payments")
            .withPassword("payments");

    @Container
    static final RabbitMQContainer rabbitmq = new RabbitMQContainer("rabbitmq:3.13-management-alpine");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("spring.rabbitmq.host", rabbitmq::getHost);
        registry.add("spring.rabbitmq.port", rabbitmq::getAmqpPort);
        registry.add("spring.rabbitmq.username", rabbitmq::getAdminUsername);
        registry.add("spring.rabbitmq.password", rabbitmq::getAdminPassword);
        registry.add("payment.outbox.publish-delay", () -> "PT1H");
        registry.add("payment.retry.scan-delay", () -> "PT1H");
        registry.add("payment.retry.recovery-delay", () -> "PT1H");
    }

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate restTemplate;

    @Autowired
    PaymentRepository payments;

    @Autowired
    OutboxEventRepository outboxEvents;

    @Autowired
    OutboxPublisher outboxPublisher;

    @Autowired
    RabbitAdmin rabbitAdmin;

    @MockBean
    ExternalPaymentClient externalPaymentClient;

    @Test
    void acceptedPaymentCompletesThroughOutboxRabbitAndWorker() {
        when(externalPaymentClient.charge(any(Payment.class)))
                .thenReturn(new ExternalPaymentResult("ext-it-1", "APPROVED", null, 200, "{\"status\":\"APPROVED\"}"));

        PaymentCreateRequest request = new PaymentCreateRequest(
                new BigDecimal("19.99"), "EUR", "IT-APPROVED", "it-approved-key");
        ResponseEntity<PaymentResponse> created = restTemplate.postForEntity(
                url("/payments"), request, PaymentResponse.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(created.getBody()).isNotNull();

        outboxPublisher.publishDueEvents();

        PaymentResponse completed = Awaitility.await()
                .atMost(Duration.ofSeconds(20))
                .pollInterval(Duration.ofMillis(200))
                .until(
                        () -> restTemplate.getForObject(
                                url("/payments/" + created.getBody().paymentId()), PaymentResponse.class),
                        response -> response != null && response.state() == PaymentState.COMPLETED);

        assertThat(completed.attemptCount()).isEqualTo(1);
        assertThat(completed.paymentServiceStatus()).isEqualTo("APPROVED");
        assertThat(completed.paymentServiceResponseId()).isEqualTo("ext-it-1");
        assertThat(completed.attempts()).hasSize(1);

        assertThat(payments.findById(created.getBody().paymentId()))
                .get()
                .extracting(Payment::getState)
                .isEqualTo(PaymentState.COMPLETED);
        assertThat(outboxEvents.findAll())
                .allSatisfy(event -> assertThat(event.getStatus().name()).isEqualTo("PUBLISHED"));
        assertThat(rabbitAdmin.getQueueInfo("payments.processing").getMessageCount()).isZero();
    }

    @Test
    void duplicateIdempotencyConflictReturns409() {
        when(externalPaymentClient.charge(any(Payment.class)))
                .thenReturn(new ExternalPaymentResult("ext-it-2", "APPROVED", null, 200, "{\"status\":\"APPROVED\"}"));

        PaymentCreateRequest original = new PaymentCreateRequest(
                new BigDecimal("10.00"), "EUR", "IT-IDEMPOTENCY", "it-conflict-key");
        PaymentCreateRequest changed = new PaymentCreateRequest(
                new BigDecimal("11.00"), "EUR", "IT-IDEMPOTENCY", "it-conflict-key");

        ResponseEntity<PaymentResponse> created = restTemplate.postForEntity(
                url("/payments"), original, PaymentResponse.class);
        ResponseEntity<ProblemDetail> conflict = restTemplate.postForEntity(
                url("/payments"), changed, ProblemDetail.class);

        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(conflict.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(conflict.getBody()).isNotNull();
        assertThat(conflict.getBody().getDetail())
                .isEqualTo("Idempotency key already exists for a different request");
        assertThat(conflict.getBody().getStatus()).isEqualTo(409);
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
