package com.campuseventhub.ticket.service;

import com.campuseventhub.ticket.dto.TicketResponse;
import com.campuseventhub.ticket.entity.Ticket;
import com.campuseventhub.ticket.entity.TicketStatus;
import com.campuseventhub.ticket.exception.TicketNotFoundException;
import com.campuseventhub.ticket.repository.TicketRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final TicketRepository ticketRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public Ticket generateTicket(Long registrationId, String studentId, Long eventId) {
        // Idempotency: don't regenerate if ticket already exists
        return ticketRepository.findByRegistrationId(registrationId).orElseGet(() -> {
            String qrContent = buildQrContent(registrationId, studentId, eventId);
            String qrBase64 = generateQrBase64(qrContent);
            Ticket ticket = Ticket.builder()
                    .registrationId(registrationId)
                    .studentId(studentId)
                    .eventId(eventId)
                    .qrCode("data:image/png;base64," + qrBase64)
                    .build();
            Ticket saved = ticketRepository.save(ticket);
            log.info("Generated ticket id={} for registrationId={}", saved.getId(), registrationId);
            return saved;
        });
    }

    public TicketResponse getByRegistrationId(Long registrationId) {
        Ticket ticket = ticketRepository.findByRegistrationId(registrationId)
                .orElseThrow(() -> new TicketNotFoundException("No ticket for registrationId: " + registrationId));
        return toResponse(ticket);
    }

    public TicketResponse getById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + id));
        return toResponse(ticket);
    }

    @Transactional
    public TicketResponse markUsed(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + id));
        ticket.setStatus(TicketStatus.USED);
        return toResponse(ticketRepository.save(ticket));
    }

    private String buildQrContent(Long registrationId, String studentId, Long eventId) {
        try {
            return objectMapper.writeValueAsString(
                    Map.of("registrationId", registrationId, "studentId", studentId, "eventId", eventId)
            );
        } catch (Exception e) {
            return "registrationId=" + registrationId + "&studentId=" + studentId + "&eventId=" + eventId;
        }
    }

    private String generateQrBase64(String content) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix matrix = writer.encode(content, BarcodeFormat.QR_CODE, 200, 200);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return Base64.getEncoder().encodeToString(out.toByteArray());
        } catch (Exception e) {
            log.error("QR generation failed", e);
            return "";
        }
    }

    private TicketResponse toResponse(Ticket t) {
        return TicketResponse.builder()
                .id(t.getId())
                .registrationId(t.getRegistrationId())
                .studentId(t.getStudentId())
                .eventId(t.getEventId())
                .qrCode(t.getQrCode())
                .generatedAt(t.getGeneratedAt())
                .status(t.getStatus())
                .build();
    }
}
