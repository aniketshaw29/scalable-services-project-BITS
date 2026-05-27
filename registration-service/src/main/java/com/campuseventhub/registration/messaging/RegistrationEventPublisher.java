package com.campuseventhub.registration.messaging;

import com.campuseventhub.registration.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RegistrationEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishRegistrationCompleted(RegistrationCompletedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
        log.info("Published registration.completed for registrationId={}", event.getRegistrationId());
    }
}
