package com.pulsepass.dto.response;

import java.time.LocalDate;

/**
 * Combina User + UserProfile (BR-USER-004: ambos se crean juntos).
 */
public record UserResponse(

        Long id,

        String username,

        String email,

        boolean active,

        String firstName,

        String lastName,

        String phone,

        String city,

        LocalDate birthDate

) {
}
