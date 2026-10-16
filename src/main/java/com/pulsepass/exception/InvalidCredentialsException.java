package com.pulsepass.exception;

/**
 * FR-SEC-AUTH-003: identifier o password incorrectos. SIEMPRE con un
 * mensaje generico — nunca revela si el problema fue el usuario
 * inexistente, la contraseña incorrecta o la cuenta inactiva (eso
 * permitiria a un atacante enumerar usuarios validos probando logins).
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
