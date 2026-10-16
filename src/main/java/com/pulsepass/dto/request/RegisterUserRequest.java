package com.pulsepass.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * FR-SVC-010. Registra User y UserProfile en la misma operacion
 * (BR-USER-004).
 */
public record RegisterUserRequest(

        @NotBlank(message = "El username es obligatorio")
        @Size(min = 3, max = 50, message = "El username debe tener entre 3 y 50 caracteres")
        String username,

        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El email no tiene un formato valido")
        @Size(max = 150, message = "El email no puede superar los 150 caracteres")
        String email,

        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 72, message = "Password must be between 12 and 72 characters")
        String password,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no puede superar los 100 caracteres")
        String firstName,

        @NotBlank(message = "El apellido es obligatorio")
        @Size(max = 100, message = "El apellido no puede superar los 100 caracteres")
        String lastName,

        @Pattern(regexp = "^\\+?[0-9]{7,15}$",
                message = "El telefono debe tener entre 7 y 15 digitos, con '+' opcional al inicio")
        String phone,

        @Size(max = 100, message = "La ciudad no puede superar los 100 caracteres")
        String city,

        @NotNull(message = "La fecha de nacimiento es obligatoria")
        @Past(message = "La fecha de nacimiento debe ser anterior a hoy")
        LocalDate birthDate

) {
}