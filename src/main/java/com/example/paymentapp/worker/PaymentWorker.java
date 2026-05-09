package com.example.paymentapp.worker;

import com.rabbitmq.client.Channel;
import java.io.IOException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Profile("!simulator")
@Component
public class PaymentWorker {

    private final PaymentProcessor processor;

    public PaymentWorker(PaymentProcessor processor) {
        this.processor = processor;
    }

    @RabbitListener(queues = "${payment.rabbit.queue:payments.processing}", ackMode = "MANUAL")
    public void process(PaymentWorkMessage message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag)
            throws IOException {
        try {
            processor.process(message.paymentId());
            channel.basicAck(deliveryTag, false);
        } catch (RuntimeException exception) {
            channel.basicNack(deliveryTag, false, true);
            throw exception;
        }
    }
}

