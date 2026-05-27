package com.campuseventhub.ticket.controller;

import com.campuseventhub.ticket.dto.TicketResponse;
import com.campuseventhub.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @GetMapping("/registration/{registrationId}")
    public ResponseEntity<TicketResponse> getByRegistrationId(@PathVariable Long registrationId) {
        return ResponseEntity.ok(ticketService.getByRegistrationId(registrationId));
    }

    @GetMapping("/{id}/validate")
    public ResponseEntity<TicketResponse> validate(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.getById(id));
    }

    @PutMapping("/{id}/mark-used")
    public ResponseEntity<TicketResponse> markUsed(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.markUsed(id));
    }
}
