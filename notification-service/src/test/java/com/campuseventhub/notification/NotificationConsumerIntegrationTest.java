package com.campuseventhub.notification;

import com.campuseventhub.notification.entity.NotificationType;
import com.campuseventhub.notification.messaging.IncomingMessage;
import com.campuseventhub.notification.messaging.NotificationMessageConsumer;
import com.campuseventhub.notification.repository.NotificationRepository;
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
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the notification consumer path.
 * Delivers IncomingMessage objects directly to the consumer and asserts
 * that rows are persisted and readable via the HTTP API.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class NotificationConsumerIntegrationTest {

    @Autowired NotificationMessageConsumer consumer;
    @Autowired NotificationRepository       notificationRepository;
    @Autowired MockMvc                      mockMvc;

    @MockBean RabbitTemplate rabbitTemplate;

    static final String STUDENT_ID    = "STU-NOT-001";
    static final String STUDENT_EMAIL = "notify@college.edu";
    static final String EVENT_TITLE   = "Notification Integration Workshop";

    // ── registration.completed ────────────────────────────────────────────────

    @Test @Order(1)
    void registrationCompleted_savesNotificationWithCorrectType() {
        consumer.handleMessage(IncomingMessage.builder()
                .eventType("registration.completed")
                .timestamp(LocalDateTime.now())
                .payload(Map.of(
                        "studentId",    STUDENT_ID,
                        "studentEmail", STUDENT_EMAIL,
                        "eventTitle",   EVENT_TITLE
                ))
                .build());

        var notifications = notificationRepository.findByRecipientId(STUDENT_ID);
        assertThat(notifications).hasSize(1);
        assertThat(notifications.get(0).getType()).isEqualTo(NotificationType.REGISTRATION);
        assertThat(notifications.get(0).getSubject()).contains(EVENT_TITLE);
        assertThat(notifications.get(0).getRecipientEmail()).isEqualTo(STUDENT_EMAIL);
    }

    @Test @Order(2)
    void registrationCompleted_isRetrievableViaHttp() throws Exception {
        mockMvc.perform(get("/api/notifications/student/{id}", STUDENT_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$[0].type").value("REGISTRATION"))
                .andExpect(jsonPath("$[0].subject").value(containsString(EVENT_TITLE)));
    }

    @Test @Order(3)
    void registrationCompleted_multipleDeliveries_createMultipleNotifications() {
        // Deliver for a second student
        consumer.handleMessage(IncomingMessage.builder()
                .eventType("registration.completed")
                .timestamp(LocalDateTime.now())
                .payload(Map.of(
                        "studentId",    "STU-NOT-002",
                        "studentEmail", "other@college.edu",
                        "eventTitle",   "Second Event"
                ))
                .build());

        assertThat(notificationRepository.findByRecipientId("STU-NOT-002")).hasSize(1);
        // Original student's notification still there
        assertThat(notificationRepository.findByRecipientId(STUDENT_ID)).hasSize(1);
    }

    // ── announcement.created ─────────────────────────────────────────────────

    @Test @Order(4)
    void announcementCreated_savesAnnouncementNotification() {
        consumer.handleMessage(IncomingMessage.builder()
                .eventType("announcement.created")
                .timestamp(LocalDateTime.now())
                .payload(Map.of(
                        "title",   "Campus Holiday Notice",
                        "content", "Campus will be closed next Monday."
                ))
                .build());

        var byType = notificationRepository.findByType(NotificationType.ANNOUNCEMENT);
        assertThat(byType).isNotEmpty();
        assertThat(byType.get(0).getSubject()).isEqualTo("Campus Holiday Notice");
        assertThat(byType.get(0).getMessage()).contains("closed next Monday");
    }

    @Test @Order(5)
    void announcementNotification_retrievableByType() throws Exception {
        mockMvc.perform(get("/api/notifications/type/ANNOUNCEMENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].type").value("ANNOUNCEMENT"));
    }

    // ── results.published ────────────────────────────────────────────────────

    @Test @Order(6)
    void resultsPublished_savesResultNotification() {
        consumer.handleMessage(IncomingMessage.builder()
                .eventType("results.published")
                .timestamp(LocalDateTime.now())
                .payload(Map.of("eventTitle", "Hackathon 2026"))
                .build());

        var byType = notificationRepository.findByType(NotificationType.RESULT);
        assertThat(byType).isNotEmpty();
        assertThat(byType.get(0).getSubject()).contains("Hackathon 2026");
    }

    // ── unknown event type ────────────────────────────────────────────────────

    @Test @Order(7)
    void unknownEventType_doesNotPersistAnything() {
        long before = notificationRepository.count();

        consumer.handleMessage(IncomingMessage.builder()
                .eventType("some.unknown.event")
                .timestamp(LocalDateTime.now())
                .payload(Map.of("foo", "bar"))
                .build());

        assertThat(notificationRepository.count()).isEqualTo(before);
    }
}
