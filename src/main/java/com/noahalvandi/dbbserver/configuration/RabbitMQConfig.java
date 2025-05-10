package com.noahalvandi.dbbserver.configuration;

import com.noahalvandi.dbbserver.util.GlobalConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class RabbitMQConfig {

    public static final String RESERVATION_QUEUE = "reservation.expire.queue";
    public static final String RESERVATION_EXCHANGE = "reservation.exchange";
    public static final String RESERVATION_ROUTING_KEY = "reservation.expire";

    @Bean
    public Queue reservationQueue() {
        return QueueBuilder.durable(RESERVATION_QUEUE)
                .withArgument("x-dead-letter-exchange", RESERVATION_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", RESERVATION_ROUTING_KEY)
                .withArgument("x-message-ttl", GlobalConstants.HOURS_UNTIL_RESERVATION_EXPIRES * 3600 * 1000)
                .build();
    }

    @Bean
    public DirectExchange reservationExchange() {
        return new DirectExchange(RESERVATION_EXCHANGE);
    }

    @Bean
    public Binding binding() {
        return BindingBuilder.bind(reservationQueue())
                .to(reservationExchange())
                .with(RESERVATION_ROUTING_KEY);
    }
}

