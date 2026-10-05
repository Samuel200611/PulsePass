package com.pulsepass.exception;

/**
 * Existe un conflicto de unicidad (ej. "Username already exists.").
 * Distinta de BusinessRuleException: aqui el problema es que el recurso
 * YA EXISTE, no que la operacion este prohibida sobre un recurso valido.
 */
public class DuplicateResourceException extends RuntimeException {

    public DuplicateResourceException(String message) {
        super(message);
    }
}
