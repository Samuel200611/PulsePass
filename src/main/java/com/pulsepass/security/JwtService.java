package com.pulsepass.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/**
 * Responsabilidad unica (tabla 7.1 del PRD): emitir y validar firma,
 * issuer, tipo, fechas y sujeto. NO consulta tickets ni ejecuta negocio —
 * ni siquiera consulta la base de datos; todo lo que necesita vive en
 * JwtProperties y en los claims del propio token.
 *
 * Sigue literalmente el patron de la seccion 14.4 del PRD: Jwts.builder()
 * ...signWith(key, Jwts.SIG.HS256) para emitir, y
 * Jwts.parser().verifyWith(key).requireIssuer(...) para validar. Nunca se
 * usan las APIs obsoletas de JJWT (setSigningKey/parseClaimsJws).
 */
@Service
public class JwtService {

    /** SEC004: Base64 de al menos 32 bytes -> 256 bits para HS256. */
    private static final int MINIMUM_KEY_BYTES = 32;

    private final JwtProperties properties;
    private final SecretKey signingKey;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        this.signingKey = buildAndValidateKey(properties.secretBase64());
    }

    /**
     * SEC004: "la aplicacion debe fallar al arrancar si la clave esta
     * ausente o es insuficiente". La ausencia total ya la cubre Spring al
     * resolver el placeholder (sin valor por defecto en el YAML); aqui
     * cubrimos el caso de una clave presente pero demasiado corta o
     * invalida en Base64 — ambos casos lanzan una excepcion desde el
     * constructor, lo que aborta la creacion del bean y detiene el
     * arranque del contexto.
     */
    private SecretKey buildAndValidateKey(String secretBase64) {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalStateException(
                    "PULSEPASS_JWT_SECRET_BASE64 is not configured.");
        }

        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secretBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(
                    "PULSEPASS_JWT_SECRET_BASE64 is not valid Base64.", ex);
        }

        if (keyBytes.length < MINIMUM_KEY_BYTES) {
            throw new IllegalStateException(
                    "PULSEPASS_JWT_SECRET_BASE64 must decode to at least "
                            + MINIMUM_KEY_BYTES + " bytes (got " + keyBytes.length + ").");
        }

        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * FR-SEC-AUTH-005: claims minimos sub, iss, iat, exp, jti; firma HS256
     * explicita.
     */
    public String generateAccessToken(Long userId) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.accessTokenTtl());

        return Jwts.builder()
                .subject(userId.toString())
                .issuer(properties.issuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .id(UUID.randomUUID().toString())
                .signWith(signingKey, Jwts.SIG.HS256)
                .compact();
    }

    /**
     * FR-SEC-AUTH-006: verifica criptograficamente la firma (con la MISMA
     * clave con la que se firmo — JJWT rechaza automaticamente cualquier
     * alg distinto de HS256 porque solo se le entrego una SecretKey
     * simetrica, nunca "alg=none" ni un token sin firmar) y exige que el
     * issuer coincida exactamente con el configurado.
     *
     * Las excepciones de JJWT (ExpiredJwtException, MalformedJwtException,
     * SignatureException, UnsupportedJwtException, IllegalArgumentException
     * para un token vacio/nulo) se dejan propagar deliberadamente: quien
     * llama (JwtAuthenticationFilter) decide que token invalido significa
     * "no autenticar", sin exponer el detalle al cliente.
     */
    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .requireIssuer(properties.issuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Para AuthResponse.expiresIn (seccion 10.2 del PRD: 900 segundos). */
    public long getAccessTokenTtlSeconds() {
        return properties.accessTokenTtl().toSeconds();
    }
}
