package com.example.paymentapp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.rabbit")
public record PaymentRabbitProperties(String exchange, String queue, String routingKey) {

    public PaymentRabbitProperties {
        if (exchange == null || exchange.isBlank()) {
            exchange = "payments.exchange";
        }
        if (queue == null || queue.isBlank()) {
            queue = "payments.processing";
        }
        if (routingKey == null || routingKey.isBlank()) {
            routingKey = "payments.process";
        }
    }
}

