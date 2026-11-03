package com.qsource.api.security;

import java.util.UUID;

/** Authenticated caller: human user or API key. */
public record AuthPrincipal(
        UUID userId,
        String displayName,
        String email,
        UUID apiKeyId,
        UUID organizationId,
        UUID projectId,
        boolean apiKey) {

    public static AuthPrincipal user(UUID userId, String displayName, String email) {
        return new AuthPrincipal(userId, displayName, email, null, null, null, false);
    }

    public static AuthPrincipal apiKey(UUID apiKeyId, String name, UUID organizationId, UUID projectId) {
        return new AuthPrincipal(null, name, null, apiKeyId, organizationId, projectId, true);
    }
}
