package com.campuseventhub.ticket;

import com.campuseventhub.ticket.entity.Ticket;
import com.campuseventhub.ticket.entity.TicketStatus;
import com.campuseventhub.ticket.messaging.RegistrationCompletedEvent;
import com.campuseventhub.ticket.messaging.TicketMessageConsumer;
import com.campuseventhub.ticket.repository.TicketRepository;
import com.campuseventhub.ticket.service.TicketService;
import org.junit.jupiter.api.*;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the RabbitMQ consumer path.
 * Calls TicketMessageConsumer directly (bypassing the broker) and asserts
 * that TicketService + TicketRepository react correctly.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TicketConsumerIntegrationTest {

    @Autowired TicketMessageConsumer consumer;
    @Autowired TicketService         ticketService;
    @Autowired TicketRepository      ticketRepository;
    @Autowired MockMvc               mockMvc;

    @MockBean RabbitTemplate rabbitTemplate;

    static final Long REG_ID     = 5001L;
    static final Long EVENT_ID   = 7001L;
    static final String STU_ID   = "STU-IT-001";

    // ── consumer triggers correct DB write ────────────────────────────────────

    @Test @Order(1)
    void consumer_generateTicket_persistsRowAndQr() {
        RegistrationCompletedEvent event = RegistrationCompletedEvent.builder()
                .registrationId(REG_ID)
                .studentId(STU_ID)
                .studentName("Integration Tester")
                .studentEmail("it@college.edu")
                .eventId(EVENT_ID)
                .eventTitle("IT Workshop")
                .timestamp(LocalDateTime.now())
                .build();

        consumer.handleRegistrationCompleted(event);

        Optional<Ticket> opt = ticketRepository.findByRegistrationId(REG_ID);
        assertThat(opt).isPresent();
        Ticket t = opt.get();
        assertThat(t.getStudentId()).isEqualTo(STU_ID);
        assertThat(t.getEventId()).isEqualTo(EVENT_ID);
        assertThat(t.getStatus()).isEqualTo(TicketStatus.VALID);
        assertThat(t.getQrCode()).startsWith("data:image/png;base64,");
        assertThat(t.getQrCode().length()).isGreaterThan(50);
    }

    @Test @Order(2)
    void consumer_idempotent_duplicateMessageCreatesNoSecondRow() {
        long countBefore = ticketRepository.count();

        // Deliver the same event again
        RegistrationCompletedEvent event = RegistrationCompletedEvent.builder()
                .registrationId(REG_ID)
                .studentId(STU_ID)
                .eventId(EVENT_ID)
                .timestamp(LocalDateTime.now())
                .build();
        consumer.handleRegistrationCompleted(event);

        assertThat(ticketRepository.count()).isEqualTo(countBefore);
    }

    @Test @Order(3)
    void consumer_differentRegistrationId_createsSeparateTicket() {
        RegistrationCompletedEvent event = RegistrationCompletedEvent.builder()
                .registrationId(5002L)
                .studentId("STU-IT-002")
                .eventId(EVENT_ID)
                .timestamp(LocalDateTime.now())
                .build();
        consumer.handleRegistrationCompleted(event);

        assertThat(ticketRepository.findByRegistrationId(5002L)).isPresent();
        assertThat(ticketRepository.count()).isEqualTo(2);
    }

    // ── HTTP endpoints see the consumer-created ticket ─────────────────────────

    @Test @Order(4)
    void getByRegistrationId_returnsPreviouslyGeneratedTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/registration/{id}", REG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(REG_ID))
                .andExpect(jsonPath("$.studentId").value(STU_ID))
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.qrCode").value(org.hamcrest.Matchers.startsWith("data:image/png;base64,")));
    }

    @Test @Order(5)
    void validate_consumerCreatedTicket_returnsTicketWithValidStatus() throws Exception {
        Ticket t = ticketRepository.findByRegistrationId(REG_ID).orElseThrow();

        mockMvc.perform(get("/api/tickets/{id}/validate", t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"));
    }

    @Test @Order(6)
    void markUsed_changesStatusToUsed() throws Exception {
        Ticket t = ticketRepository.findByRegistrationId(REG_ID).orElseThrow();

        mockMvc.perform(put("/api/tickets/{id}/mark-used", t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));

        assertThat(ticketRepository.findById(t.getId()).orElseThrow().getStatus())
                .isEqualTo(TicketStatus.USED);
    }

    @Test @Order(7)
    void validate_usedTicket_returnsUsedStatus() throws Exception {
        Ticket t = ticketRepository.findByRegistrationId(REG_ID).orElseThrow();

        mockMvc.perform(get("/api/tickets/{id}/validate", t.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));
    }
}
