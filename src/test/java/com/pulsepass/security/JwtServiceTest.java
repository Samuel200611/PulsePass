package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Seccion 17.1 del PRD de Seguridad: "firma correcta; claims correctos;
 * expiracion; issuer incorrecto; manipulacion; token vacio; clave debil."
 *
 * JwtService no tiene colaboradores (no llama repositories ni hace I/O):
 * es una funcion pura sobre JwtProperties, igual que TicketPricingPolicy
 * en la Fase 2 — por eso se prueba con JUnit puro, sin Mockito ni
 * contexto de Spring.
 */
class JwtServiceTest {

    private static final String VALID_SECRET =
            Base64.getEncoder().encodeToString("a".repeat(32).getBytes());

    private JwtService serviceWith(String secretBase64, String issuer, Duration ttl) {
        return new JwtService(new JwtProperties(issuer, secretBase64, ttl));
    }

    private JwtService defaultService() {
        return serviceWith(VALID_SECRET, "pulsepass-api", Duration.ofMinutes(15));
    }

    // ---------------------------------------------------------
    // SEC004 — clave ausente o insuficiente: falla al construir
    // ---------------------------------------------------------

    @Test
    void shouldFailToStartWhenSecretIsMissing() {
        assertThatThrownBy(() -> serviceWith(null, "pulsepass-api", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("PULSEPASS_JWT_SECRET_BASE64");
    }

    @Test
    void shouldFailToStartWhenSecretIsBlank() {
        assertThatThrownBy(() -> serviceWith("   ", "pulsepass-api", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void shouldFailToStartWhenSecretIsNotValidBase64() {
        assertThatThrownBy(() -> serviceWith("not-valid-base64!!!", "pulsepass-api", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Base64");
    }

    @Test
    void shouldFailToStartWhenKeyIsShorterThan32Bytes() {
        String shortSecret = Base64.getEncoder().encodeToString("too-short".getBytes());

        assertThatThrownBy(() -> serviceWith(shortSecret, "pulsepass-api", Duration.ofMinutes(15)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("32");
    }

    @Test
    void shouldStartSuccessfullyWithExactly32Bytes() {
        String exact32Bytes = Base64.getEncoder().encodeToString("x".repeat(32).getBytes());

        assertThatCode(() -> serviceWith(exact32Bytes, "pulsepass-api", Duration.ofMinutes(15)))
                .doesNotThrowAnyException();
    }

    // ---------------------------------------------------------
    // FR-SEC-AUTH-005 — emision: claims correctos
    // ---------------------------------------------------------

    @Test
    void shouldGenerateTokenWithAllRequiredClaims() {
        JwtService service = defaultService();

        String token = service.generateAccessToken(42L);
        Claims claims = service.parseAndValidate(token);

        assertThat(claims.getSubject()).isEqualTo("42");
        assertThat(claims.getIssuer()).isEqualTo("pulsepass-api");
        assertThat(claims.getId()).isNotBlank();
        assertThat(claims.getIssuedAt()).isNotNull();
        assertThat(claims.getExpiration()).isAfter(claims.getIssuedAt());
    }

    @Test
    void shouldGenerateDifferentJtiForEachToken() {
        JwtService service = defaultService();

        String firstToken = service.generateAccessToken(1L);
        String secondToken = service.generateAccessToken(1L);

        String firstJti = service.parseAndValidate(firstToken).getId();
        String secondJti = service.parseAndValidate(secondToken).getId();

        assertThat(firstJti).isNotEqualTo(secondJti);
    }

    @Test
    void shouldExposeConfiguredTtlInSeconds() {
        JwtService service = serviceWith(VALID_SECRET, "pulsepass-api", Duration.ofMinutes(15));

        assertThat(service.getAccessTokenTtlSeconds()).isEqualTo(900L);
    }

    // ---------------------------------------------------------
    // FR-SEC-AUTH-006 — validacion: rechazos
    // ---------------------------------------------------------

    @Test
    void shouldRejectExpiredToken() {
        JwtService shortLived = serviceWith(VALID_SECRET, "pulsepass-api", Duration.ofMillis(1));
        String token = shortLived.generateAccessToken(1L);

        await(5);

        assertThatThrownBy(() -> shortLived.parseAndValidate(token))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void shouldRejectTokenWithWrongIssuer() {
        JwtService issuerA = serviceWith(VALID_SECRET, "issuer-a", Duration.ofMinutes(15));
        JwtService issuerB = serviceWith(VALID_SECRET, "issuer-b", Duration.ofMinutes(15));

        String tokenFromA = issuerA.generateAccessToken(1L);

        assertThatThrownBy(() -> issuerB.parseAndValidate(tokenFromA))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectTokenSignedWithADifferentKey() {
        JwtService signer = defaultService();
        String otherSecret = Base64.getEncoder().encodeToString("b".repeat(32).getBytes());
        JwtService verifier = serviceWith(otherSecret, "pulsepass-api", Duration.ofMinutes(15));

        String token = signer.generateAccessToken(1L);

        assertThatThrownBy(() -> verifier.parseAndValidate(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void shouldRejectTamperedToken() {
        JwtService service = defaultService();
        String token = service.generateAccessToken(1L);

        // Se altera un caracter de la porcion de payload (segunda parte,
        // separada por puntos), simulando UC-SEC-09 del PRD.
        String[] parts = token.split("\\.");
        char[] payloadChars = parts[1].toCharArray();
        payloadChars[payloadChars.length / 2] =
                payloadChars[payloadChars.length / 2] == 'A' ? 'B' : 'A';
        String tamperedToken = parts[0] + "." + new String(payloadChars) + "." + parts[2];

        assertThatThrownBy(() -> service.parseAndValidate(tamperedToken))
                .isInstanceOf(JwtException.class);
    }

    @Test
    void shouldRejectEmptyToken() {
        JwtService service = defaultService();

        assertThatThrownBy(() -> service.parseAndValidate(""))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void shouldRejectMalformedToken() {
        JwtService service = defaultService();

        assertThatThrownBy(() -> service.parseAndValidate("not-a-jwt-at-all"))
                .isInstanceOf(JwtException.class);
    }

    private void await(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
