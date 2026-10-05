package com.pulsepass.domain;

/**
 * Ciclo de vida del evento (FR-EVT-003, seccion 9.1 del PRD).
 * DRAFT -> PUBLISHED -> SOLD_OUT -> FINISHED, con CANCELLED como salida.
 * El MVP no implementa la maquina de estados (BR-010 / R-002).
 */
public enum EventStatus {
    DRAFT,
    PUBLISHED,
    SOLD_OUT,
    CANCELLED,
    FINISHED
}
