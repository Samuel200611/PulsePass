package com.pulsepass.controller;

import com.pulsepass.dto.request.PurchaseTicketRequest;
import com.pulsepass.dto.response.TicketResponse;
import com.pulsepass.service.TicketService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;


@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    //POST /api/tickets -> 201 Created.


    @PostMapping
    public ResponseEntity<TicketResponse> purchase(
            @Valid @RequestBody PurchaseTicketRequest request) {

        TicketResponse response = ticketService.purchase(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    // GET /api/tickets/{ticketCode} -> 200 OK, o 404.
    @GetMapping("/{ticketCode}")
    public ResponseEntity<TicketResponse> findByCode(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.findByCode(ticketCode));
    }

    // GET /api/tickets/by-user?email=... -> 200 OK.
    @GetMapping("/by-user")
    public ResponseEntity<List<TicketResponse>> findByUserEmail(
            @RequestParam String email) {
        return ResponseEntity.ok(ticketService.findByUserEmail(email));
    }
    //PATCH /api/tickets/{ticketCode}/cancel -> 200 OK, o 409

    @PatchMapping("/{ticketCode}/cancel")
    public ResponseEntity<TicketResponse> cancel(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.cancel(ticketCode));
    }

    // PATCH /api/tickets/{ticketCode}/use -> 200 OK, o 409

    @PatchMapping("/{ticketCode}/use")
    public ResponseEntity<TicketResponse> markAsUsed(@PathVariable String ticketCode) {
        return ResponseEntity.ok(ticketService.markAsUsed(ticketCode));
    }
}
