package com.qsource.api.security;

import com.qsource.api.domain.ApiKey;
import com.qsource.api.repo.ApiKeyRepository;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class AuthTokenFilter extends OncePerRequestFilter {
    public static final String API_KEY_HEADER = "X-Api-Key";

    private final JwtService jwtService;
    private final ApiKeyRepository apiKeyRepository;

    public AuthTokenFilter(JwtService jwtService, ApiKeyRepository apiKeyRepository) {
        this.jwtService = jwtService;
        this.apiKeyRepository = apiKeyRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String apiKey = request.getHeader(API_KEY_HEADER);
            if (apiKey != null && !apiKey.isBlank()) {
                authenticateApiKey(apiKey.trim());
            } else {
                String header = request.getHeader(HttpHeaders.AUTHORIZATION);
                if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
                    authenticateJwt(header.substring(7).trim());
                }
            }
            filterChain.doFilter(request, response);
        } finally {
            // SecurityContext cleared by Spring Security; Actor cleared in ActorFilter
        }
    }

    private void authenticateJwt(String token) {
        try {
            Claims claims = jwtService.parse(token);
            UUID userId = UUID.fromString(claims.getSubject());
            String email = claims.get("email", String.class);
            String name = claims.get("name", String.class);
            AuthPrincipal principal = AuthPrincipal.user(userId, name, email);
            var auth = new UsernamePasswordAuthenticationToken(
                    principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (Exception ignored) {
            SecurityContextHolder.clearContext();
        }
    }

    private void authenticateApiKey(String rawKey) {
        String hash = TokenHasher.sha256(rawKey);
        ApiKey key = apiKeyRepository.findByKeyHashAndRevokedAtIsNull(hash).orElse(null);
        if (key == null) {
            SecurityContextHolder.clearContext();
            return;
        }
        AuthPrincipal principal = AuthPrincipal.apiKey(
                key.getId(), key.getName(), key.getOrganizationId(), key.getProjectId());
        var auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_API_KEY")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
