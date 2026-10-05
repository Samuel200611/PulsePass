package com.pulsepass.domain;

/**
 * Catalogo de categorias de evento (FR-EVT-004, seccion 6.2 del PRD).
 * Se persiste con @Enumerated(EnumType.STRING) para cumplir BR-008:
 * el valor almacenado es el nombre estable, no el ordinal.
 */
public enum EventCategory {
    MUSIC,
    SPORTS,
    TECHNOLOGY,
    EDUCATION,
    CULTURE,
    ENTERTAINMENT
}
