package com.qsource.api.config;

import com.qsource.api.domain.RevisionSource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class ActorFilter extends OncePerRequestFilter {
    public static final String NAME_HEADER = "X-Actor-Name";
    public static final String SOURCE_HEADER = "X-Actor-Source";

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String name = request.getHeader(NAME_HEADER);
            if (name == null || name.isBlank()) {
                name = "anonymous";
            }
            RevisionSource source = RevisionSource.API;
            String rawSource = request.getHeader(SOURCE_HEADER);
            if (rawSource != null && !rawSource.isBlank()) {
                try {
                    source = RevisionSource.valueOf(rawSource.trim().toUpperCase());
                } catch (IllegalArgumentException ignored) {
                    source = RevisionSource.API;
                }
            }
            ActorContext.set(new Actor(name.trim(), source));
            filterChain.doFilter(request, response);
        } finally {
            ActorContext.clear();
        }
    }
}
