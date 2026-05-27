package com.campuseventhub.ticket.messaging;

import com.campuseventhub.ticket.config.RabbitMQConfig;
import com.campuseventhub.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TicketMessageConsumer {

    private final TicketService ticketService;

    @RabbitListener(queues = RabbitMQConfig.TICKET_QUEUE)
    public void handleRegistrationCompleted(RegistrationCompletedEvent event) {
        log.info("Received registration.completed for registrationId={}", event.getRegistrationId());
        ticketService.generateTicket(event.getRegistrationId(), event.getStudentId(), event.getEventId());
    }
}
