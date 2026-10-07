package com.pulsepass.dto.response;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * CTRL-008: contrato JSON uniforme para todos los errores de la API
 * (seccion 13 del PRD de Controllers). details va vacio salvo en errores
 * de validacion, donde lleva un par campo -> mensaje por cada violacion.
 */
public record ErrorResponse(

        LocalDateTime timestamp,

        int status,

        String error,

        String message,

        Map<String, String> details

) {
}
