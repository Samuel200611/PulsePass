package com.pulsepass.domain;

/**
 * Ciclo de vida de la entrada (FR-TKT-005, seccion 9.2 del PRD).
 * RESERVED -> PAID -> USED, con CANCELLED como salida.
 * PAID representa una confirmacion externa: el MVP no procesa pagos.
 */
public enum TicketStatus {
    RESERVED,
    PAID,
    CANCELLED,
    USED
}
