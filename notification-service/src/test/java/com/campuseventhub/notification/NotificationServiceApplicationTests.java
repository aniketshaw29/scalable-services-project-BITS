package com.campuseventhub.notification;

import com.campuseventhub.notification.entity.NotificationType;
import com.campuseventhub.notification.messaging.NotificationMessageConsumer;
import com.campuseventhub.notification.service.NotificationService;
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
class NotificationServiceApplicationTests {

    @Autowired MockMvc mockMvc;
    @Autowired NotificationService notificationService;
    @MockBean  RabbitTemplate rabbitTemplate;
    @MockBean  NotificationMessageConsumer notificationMessageConsumer;

    static Long registrationNotifId;
    static Long announcementNotifId;

    @Test @Order(1)
    void contextLoads() { }

    @Test @Order(2)
    void saveRegistrationNotification_persistsCorrectly() {
        var notif = notificationService.save(
                "STU-001", "alice@college.edu",
                NotificationType.REGISTRATION,
                "Registration Confirmed: Spring Boot Workshop",
                "You have successfully registered for Spring Boot Workshop."
        );
        registrationNotifId = notif.getId();

        assertThat(notif.getId()).isNotNull();
        assertThat(notif.getType()).isEqualTo(NotificationType.REGISTRATION);
        assertThat(notif.getRecipientId()).isEqualTo("STU-001");
        assertThat(notif.getSubject()).contains("Spring Boot Workshop");
    }

    @Test @Order(3)
    void saveAnnouncementNotification_persistsCorrectly() {
        var notif = notificationService.save(
                null, null,
                NotificationType.ANNOUNCEMENT,
                "Venue Change Notice",
                "Workshop moved to Room 301."
        );
        announcementNotifId = notif.getId();

        assertThat(notif.getType()).isEqualTo(NotificationType.ANNOUNCEMENT);
        assertThat(notif.getRecipientId()).isNull();
    }

    @Test @Order(4)
    void getById_returnsNotification() throws Exception {
        mockMvc.perform(get("/api/notifications/{id}", registrationNotifId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(registrationNotifId))
                .andExpect(jsonPath("$.type").value("REGISTRATION"))
                .andExpect(jsonPath("$.status").value("SENT"));
    }

    @Test @Order(5)
    void getById_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/notifications/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value(containsString("99999")));
    }

    @Test @Order(6)
    void getByStudentId_returnsStudentNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications/student/{studentId}", "STU-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].recipientId").value("STU-001"));
    }

    @Test @Order(7)
    void getByType_registration_returnsOnlyRegistrationNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications/type/{type}", "REGISTRATION"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].type").value("REGISTRATION"));
    }

    @Test @Order(8)
    void getByType_announcement_returnsAnnouncementNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications/type/{type}", "ANNOUNCEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].type").value("ANNOUNCEMENT"));
    }

    @Test @Order(9)
    void consumerHandlesRegistrationEvent_viaDirectServiceCall() {
        // Simulate what the consumer does when it receives a registration.completed message
        var notif = notificationService.save(
                "STU-002", "bob@college.edu",
                NotificationType.REGISTRATION,
                "Registration Confirmed: Hackathon 2024",
                "You have successfully registered for Hackathon 2024."
        );
        assertThat(notif.getRecipientEmail()).isEqualTo("bob@college.edu");
        assertThat(notif.getSubject()).contains("Hackathon 2024");
    }
}
