package com.qsource.api.config;

import com.qsource.api.domain.RevisionSource;
import com.qsource.api.security.AuthPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ActorFilter extends OncePerRequestFilter {
    public static final String SOURCE_HEADER = "X-Actor-Source";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            RevisionSource source = RevisionSource.API;
            String rawSource = request.getHeader(SOURCE_HEADER);
            if (rawSource != null && !rawSource.isBlank()) {
                try {
                    source = RevisionSource.valueOf(rawSource.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    source = RevisionSource.API;
                }
            }

            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null
                    && authentication.isAuthenticated()
                    && authentication.getPrincipal() instanceof AuthPrincipal principal) {
                if (principal.apiKey()) {
                    source = RevisionSource.AGENT;
                } else if (source == RevisionSource.API && "web".equalsIgnoreCase(rawSource)) {
                    source = RevisionSource.WEB;
                }
                ActorContext.set(new Actor(principal.displayName(), source, principal.userId()));
            } else {
                ActorContext.set(Actor.anonymous());
            }
            filterChain.doFilter(request, response);
        } finally {
            ActorContext.clear();
        }
    }
}
