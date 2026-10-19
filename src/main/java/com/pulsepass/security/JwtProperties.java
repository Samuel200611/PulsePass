package com.pulsepass.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Seccion 14.2 del PRD de Seguridad:
 *
 * <pre>
 * pulsepass:
 *   security:
 *     jwt:
 *       issuer: ${PULSEPASS_JWT_ISSUER:pulsepass-api}
 *       secret-base64: ${PULSEPASS_JWT_SECRET_BASE64}
 *       access-token-ttl: PT15M
 * </pre>
 *
 * Mismo patron que ApplicationProperties del PRD de Spring Framework:
 * configuracion propia agrupada -> @ConfigurationProperties con record,
 * nunca @Value disperso. secretBase64 no tiene valor por defecto en el
 * YAML (SEC004): si la variable de entorno falta, Spring falla al
 * resolver el placeholder y la aplicacion no arranca.
 */
@ConfigurationProperties(prefix = "pulsepass.security.jwt")
public record JwtProperties(

        String issuer,

        String secretBase64,

        Duration accessTokenTtl

) {
}
