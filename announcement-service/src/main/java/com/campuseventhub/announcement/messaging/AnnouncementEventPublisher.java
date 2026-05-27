package com.campuseventhub.announcement.messaging;

import com.campuseventhub.announcement.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnnouncementEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishAnnouncementCreated(AnnouncementCreatedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
    }
}
