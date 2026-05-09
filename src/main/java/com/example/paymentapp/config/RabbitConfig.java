package com.example.paymentapp.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Profile("!simulator")
@Configuration
public class RabbitConfig {

    @Bean
    DirectExchange paymentExchange(PaymentRabbitProperties properties) {
        return new DirectExchange(properties.exchange(), true, false);
    }

    @Bean
    Queue paymentQueue(PaymentRabbitProperties properties) {
        return new Queue(properties.queue(), true);
    }

    @Bean
    Binding paymentBinding(Queue paymentQueue, DirectExchange paymentExchange, PaymentRabbitProperties properties) {
        return BindingBuilder.bind(paymentQueue).to(paymentExchange).with(properties.routingKey());
    }

    @Bean
    MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);
        factory.setDefaultRequeueRejected(true);
        return factory;
    }
}
