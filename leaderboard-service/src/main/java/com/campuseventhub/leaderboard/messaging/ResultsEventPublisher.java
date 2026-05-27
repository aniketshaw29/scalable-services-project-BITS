package com.campuseventhub.leaderboard.messaging;

import com.campuseventhub.leaderboard.config.RabbitMQConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ResultsEventPublisher {

    private final RabbitTemplate rabbitTemplate;

    public void publishResults(ResultsPublishedEvent event) {
        rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE, RabbitMQConfig.ROUTING_KEY, event);
    }
}
