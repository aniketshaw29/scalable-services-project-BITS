package com.campuseventhub.ticket;

import com.campuseventhub.ticket.messaging.TicketMessageConsumer;
import com.campuseventhub.ticket.service.TicketService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class TicketServiceApplicationTests {

    @Autowired MockMvc mockMvc;
    @Autowired TicketService ticketService;
    @MockBean  RabbitTemplate rabbitTemplate;
    @MockBean  TicketMessageConsumer ticketMessageConsumer;

    static Long createdTicketId;
    static final Long REGISTRATION_ID = 10L;
    static final String STUDENT_ID    = "STU-001";
    static final Long EVENT_ID        = 1L;

    @Test @Order(1)
    void contextLoads() { }

    @Test @Order(2)
    void generateTicket_createsTicketWithQrCode() {
        var ticket = ticketService.generateTicket(REGISTRATION_ID, STUDENT_ID, EVENT_ID);
        createdTicketId = ticket.getId();

        assertThat(ticket.getId()).isNotNull();
        assertThat(ticket.getRegistrationId()).isEqualTo(REGISTRATION_ID);
        assertThat(ticket.getStudentId()).isEqualTo(STUDENT_ID);
        assertThat(ticket.getQrCode()).startsWith("data:image/png;base64,");
        assertThat(ticket.getQrCode().length()).isGreaterThan(50);
    }

    @Test @Order(3)
    void generateTicket_idempotent_doesNotCreateDuplicate() {
        var ticket1 = ticketService.generateTicket(REGISTRATION_ID, STUDENT_ID, EVENT_ID);
        var ticket2 = ticketService.generateTicket(REGISTRATION_ID, STUDENT_ID, EVENT_ID);
        assertThat(ticket1.getId()).isEqualTo(ticket2.getId());
    }

    @Test @Order(4)
    void getByRegistrationId_returnsTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/registration/{id}", REGISTRATION_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.registrationId").value(REGISTRATION_ID))
                .andExpect(jsonPath("$.qrCode").value(startsWith("data:image/png;base64,")))
                .andExpect(jsonPath("$.status").value("VALID"));
    }

    @Test @Order(5)
    void getByRegistrationId_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/tickets/registration/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("99999")));
    }

    @Test @Order(6)
    void validate_returnsTicket() throws Exception {
        mockMvc.perform(get("/api/tickets/{id}/validate", createdTicketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(createdTicketId))
                .andExpect(jsonPath("$.status").value("VALID"));
    }

    @Test @Order(7)
    void markUsed_changesStatusToUsed() throws Exception {
        mockMvc.perform(put("/api/tickets/{id}/mark-used", createdTicketId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("USED"));
    }
}
