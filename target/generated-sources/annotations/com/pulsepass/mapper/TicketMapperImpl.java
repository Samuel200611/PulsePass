package com.pulsepass.mapper;

import com.pulsepass.domain.Event;
import com.pulsepass.domain.Ticket;
import com.pulsepass.domain.TicketStatus;
import com.pulsepass.domain.TicketType;
import com.pulsepass.domain.User;
import com.pulsepass.dto.response.TicketResponse;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-10-05T14:38:45-0500",
    comments = "version: 1.6.3, compiler: javac, environment: Java 21.0.12.1 (Oracle Corporation)"
)
@Component
public class TicketMapperImpl implements TicketMapper {

    @Override
    public TicketResponse toResponse(Ticket ticket) {
        if ( ticket == null ) {
            return null;
        }

        String userEmail = null;
        String eventCode = null;
        String eventName = null;
        Long id = null;
        String ticketCode = null;
        TicketType type = null;
        BigDecimal price = null;
        TicketStatus status = null;
        LocalDateTime purchaseDate = null;

        userEmail = ticketUserEmail( ticket );
        eventCode = ticketEventEventCode( ticket );
        eventName = ticketEventName( ticket );
        id = ticket.getId();
        ticketCode = ticket.getTicketCode();
        type = ticket.getType();
        price = ticket.getPrice();
        status = ticket.getStatus();
        purchaseDate = ticket.getPurchaseDate();

        TicketResponse ticketResponse = new TicketResponse( id, ticketCode, type, price, status, purchaseDate, userEmail, eventCode, eventName );

        return ticketResponse;
    }

    private String ticketUserEmail(Ticket ticket) {
        User user = ticket.getUser();
        if ( user == null ) {
            return null;
        }
        return user.getEmail();
    }

    private String ticketEventEventCode(Ticket ticket) {
        Event event = ticket.getEvent();
        if ( event == null ) {
            return null;
        }
        return event.getEventCode();
    }

    private String ticketEventName(Ticket ticket) {
        Event event = ticket.getEvent();
        if ( event == null ) {
            return null;
        }
        return event.getName();
    }
}
