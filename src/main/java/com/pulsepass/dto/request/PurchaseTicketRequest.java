package com.pulsepass.dto.request;

import com.pulsepass.domain.TicketType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * FR-SVC-013. El precio NUNCA viaja en el request: lo calcula el sistema
 * (seccion 28 del PRD / TicketPricingPolicy).
 */
public record PurchaseTicketRequest(

        @NotBlank(message = "El email del usuario es obligatorio")
        @Email(message = "El email del usuario no tiene un formato valido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String userEmail,

        @NotBlank(message = "El codigo del evento es obligatorio")
        @Size(max = 30, message = "El codigo del evento no puede superar los 30 caracteres")
        String eventCode,

        @NotNull(message = "El tipo de ticket es obligatorio")
        TicketType type

) {
}