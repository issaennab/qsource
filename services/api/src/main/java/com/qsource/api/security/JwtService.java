package com.qsource.api.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessTtlMinutes;

    public JwtService(
            @Value("${qsource.jwt.secret}") String secret,
            @Value("${qsource.jwt.access-ttl-minutes:60}") long accessTtlMinutes) {
        this.key = Keys.hmacShaKeyFor(pad(secret));
        this.accessTtlMinutes = accessTtlMinutes;
    }

    private static byte[] pad(String secret) {
        byte[] raw = secret.getBytes(StandardCharsets.UTF_8);
        if (raw.length >= 32) {
            return raw;
        }
        byte[] out = new byte[32];
        for (int i = 0; i < 32; i++) {
            out[i] = raw[i % raw.length];
        }
        return out;
    }

    public String createAccessToken(UUID userId, String email, String displayName) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("name", displayName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(accessTtlMinutes * 60)))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
