package com.pulsepass.service.pricing;

import com.pulsepass.domain.TicketType;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Seccion 28 del PRD: el precio NUNCA viaja desde el cliente
 * (PurchaseTicketRequest no tiene campo price); lo calcula el sistema.
 *
 * Se deja como una clase utilitaria sin estado, con metodos estaticos, en
 * vez de un @Service inyectado: no tiene colaboradores (no llama a un
 * repository ni hace I/O), asi que no hay nada que un mock le aportaria a
 * la prueba. TicketServiceImpl la invoca directamente y esta clase se
 * prueba con un JUnit plano, sin Mockito (ver TicketPricingPolicyTest).
 *
 * La estrategia académica (seccion 28 del PRD):
 * GENERAL   -> precio base
 * STUDENT   -> descuento sobre el precio base
 * VIP       -> multiplicador sobre el precio base
 * BACKSTAGE -> multiplicador superior sobre el precio base
 */
public final class TicketPricingPolicy {

    /** Precio base para un ticket GENERAL, en la moneda de la plataforma. */
    public static final BigDecimal BASE_PRICE = new BigDecimal("100000.00");

    private static final BigDecimal STUDENT_DISCOUNT_FACTOR = new BigDecimal("0.50");
    private static final BigDecimal VIP_MULTIPLIER = new BigDecimal("2.00");
    private static final BigDecimal BACKSTAGE_MULTIPLIER = new BigDecimal("3.50");

    private TicketPricingPolicy() {
        // utilitaria: no se instancia
    }

    /**
     * BR-TICKET-009: el resultado nunca es negativo (todos los factores
     * son positivos) y siempre queda en NUMERIC(12,2) via setScale.
     */
    public static BigDecimal calculatePrice(TicketType type) {
        BigDecimal price = switch (type) {
            case GENERAL -> BASE_PRICE;
            case STUDENT -> BASE_PRICE.multiply(STUDENT_DISCOUNT_FACTOR);
            case VIP -> BASE_PRICE.multiply(VIP_MULTIPLIER);
            case BACKSTAGE -> BASE_PRICE.multiply(BACKSTAGE_MULTIPLIER);
        };

        return price.setScale(2, RoundingMode.HALF_UP);
    }
}
