package com.pulsepass.dto.request;

import com.pulsepass.domain.EventCategory;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * FR-SVC-003. El status NO forma parte del request: BR-EVENT-005 fija
 * siempre DRAFT como estado inicial, sin importar lo que el cliente envie.
 */
public record CreateEventRequest(

        @NotBlank(message = "El codigo del evento es obligatorio")
        @Size(max = 30, message = "El codigo del evento no puede superar los 30 caracteres")
        String eventCode,

        @NotBlank(message = "El nombre del evento es obligatorio")
        @Size(max = 150, message = "El nombre del evento no puede superar los 150 caracteres")
        String name,

        @Size(max = 1000, message = "La descripcion no puede superar los 1000 caracteres")
        String description,

        @NotNull(message = "La categoria es obligatoria")
        EventCategory category,

        @NotNull(message = "La fecha del evento es obligatoria")
        @Future(message = "La fecha del evento debe ser futura")
        LocalDateTime eventDate,

        @Min(value = 0, message = "La edad minima no puede ser negativa")
        @Max(value = 120, message = "La edad minima no puede superar 120")
        Integer minimumAge,

        @NotBlank(message = "El codigo del recinto es obligatorio")
        @Size(max = 30, message = "El codigo del recinto no puede superar los 30 caracteres")
        String venueCode

) {
}