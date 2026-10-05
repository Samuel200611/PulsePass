package com.pulsepass.dto.request;

import java.time.LocalDate;

/**
 * FR-SVC-010. Registra User y UserProfile en la misma operacion
 * (BR-USER-004).
 */
public record RegisterUserRequest(

        String username,

        String email,

        String firstName,

        String lastName,

        String phone,

        String city,

        LocalDate birthDate

) {
}
