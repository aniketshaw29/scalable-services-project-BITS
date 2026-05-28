package com.campuseventhub.certificate;

import com.campuseventhub.certificate.messaging.AttendanceCompletedEvent;
import com.campuseventhub.certificate.messaging.CertificateMessageConsumer;
import com.campuseventhub.certificate.repository.CertificateRepository;
import com.campuseventhub.certificate.service.CertificateService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for the certificate consumer path.
 * Delivers AttendanceCompletedEvent directly to the consumer and asserts
 * PDF generation, idempotency, and HTTP retrieval.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class CertificateConsumerIntegrationTest {

    @Autowired CertificateMessageConsumer consumer;
    @Autowired CertificateRepository      certificateRepository;
    @Autowired CertificateService         certificateService;
    @Autowired MockMvc                    mockMvc;

    @MockBean RabbitTemplate rabbitTemplate;

    static final Long REG_ID      = 9001L;
    static final Long EVENT_ID    = 8001L;
    static final String STU_ID    = "STU-CERT-001";
    static final String STU_NAME  = "Certificate Tester";
    static final String EVT_TITLE = "Certification Integration Event";

    private AttendanceCompletedEvent buildEvent(Long regId, Long evtId, String stuId, String stuName) {
        return AttendanceCompletedEvent.builder()
                .attendanceId(1000L)
                .registrationId(regId)
                .studentId(stuId)
                .studentName(stuName)
                .studentEmail(stuId.toLowerCase() + "@college.edu")
                .eventId(evtId)
                .eventTitle(EVT_TITLE)
                .markedAt(LocalDateTime.now())
                .build();
    }

    // ── consumer generates a valid certificate ────────────────────────────────

    @Test @Order(1)
    void consumer_generatesCertificate_withPdf() {
        consumer.handleAttendanceCompleted(buildEvent(REG_ID, EVENT_ID, STU_ID, STU_NAME));

        var opt = certificateRepository.findByRegistrationId(REG_ID);
        assertThat(opt).isPresent();
        assertThat(opt.get().getStudentName()).isEqualTo(STU_NAME);
        assertThat(opt.get().getPdfData()).isNotNull();
        assertThat(opt.get().getPdfData().length).isGreaterThan(100);
        assertThat(opt.get().getCertificateNumber()).isNotBlank();
    }

    @Test @Order(2)
    void consumer_idempotent_duplicateEventDoesNotCreateSecondCertificate() {
        long before = certificateRepository.count();

        consumer.handleAttendanceCompleted(buildEvent(REG_ID, EVENT_ID, STU_ID, STU_NAME));

        assertThat(certificateRepository.count()).isEqualTo(before);
    }

    @Test @Order(3)
    void consumer_differentRegistration_createsSeparateCertificate() {
        consumer.handleAttendanceCompleted(buildEvent(9002L, EVENT_ID, "STU-CERT-002", "Second Tester"));

        assertThat(certificateRepository.findByRegistrationId(9002L)).isPresent();
        assertThat(certificateRepository.count()).isEqualTo(2);
    }

    // ── HTTP retrieval ────────────────────────────────────────────────────────

    @Test @Order(4)
    void getByRegistrationId_returnsConsumerCreatedCertificate() throws Exception {
        mockMvc.perform(get("/api/certificates/registration/{id}", REG_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.studentName").value(STU_NAME))
                .andExpect(jsonPath("$.hasPdf").value(true))
                .andExpect(jsonPath("$.certificateNumber").isNotEmpty());
    }

    @Test @Order(5)
    void getByStudentId_returnsAllCertificatesForStudent() throws Exception {
        mockMvc.perform(get("/api/certificates/student/{id}", STU_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].studentId").value(STU_ID));
    }

    @Test @Order(6)
    void downloadPdf_returnsApplicationPdfContentType() throws Exception {
        var cert = certificateRepository.findByRegistrationId(REG_ID).orElseThrow();

        mockMvc.perform(get("/api/certificates/{id}/pdf", cert.getId()))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        org.hamcrest.Matchers.containsString("application/pdf")));
    }

    @Test @Order(7)
    void certNumber_isUniqueAcrossMultipleCertificates() {
        var cert1 = certificateRepository.findByRegistrationId(REG_ID).orElseThrow();
        var cert2 = certificateRepository.findByRegistrationId(9002L).orElseThrow();

        assertThat(cert1.getCertificateNumber()).isNotEqualTo(cert2.getCertificateNumber());
    }
}
