package com.campuseventhub.notification.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE             = "campus.events";
    public static final String NOTIFICATION_QUEUE   = "campus.notification.queue";
    public static final String DLX                  = "campus.dlx";
    public static final String DLQ                  = "campus.dead-letters";

    @Bean
    public TopicExchange campusExchange() {
        return new TopicExchange(EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(DLX, true, false);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(DLQ).build();
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange).with("dead-letter");
    }

    @Bean
    public Queue notificationQueue() {
        return QueueBuilder.durable(NOTIFICATION_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX)
                .withArgument("x-dead-letter-routing-key", "dead-letter")
                .build();
    }

    // Bind to all three routing keys this service handles
    @Bean
    public Binding notificationBindingRegistration(Queue notificationQueue, TopicExchange campusExchange) {
        return BindingBuilder.bind(notificationQueue).to(campusExchange).with("registration.completed");
    }

    @Bean
    public Binding notificationBindingAnnouncement(Queue notificationQueue, TopicExchange campusExchange) {
        return BindingBuilder.bind(notificationQueue).to(campusExchange).with("announcement.created");
    }

    @Bean
    public Binding notificationBindingResults(Queue notificationQueue, TopicExchange campusExchange) {
        return BindingBuilder.bind(notificationQueue).to(campusExchange).with("results.published");
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory,
                                         Jackson2JsonMessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
