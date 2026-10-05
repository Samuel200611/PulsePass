package com.pulsepass.exception;

/**
 * El recurso existe, pero la operacion solicitada viola una regla de
 * negocio (ej. "User does not meet minimum age.").
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
