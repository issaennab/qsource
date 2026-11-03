package com.qsource.api.service;

import com.qsource.api.domain.ApiKey;
import com.qsource.api.domain.Project;
import com.qsource.api.dto.Dtos.*;
import com.qsource.api.repo.ApiKeyRepository;
import com.qsource.api.security.TokenHasher;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiKeyService {
    private final ApiKeyRepository apiKeyRepository;
    private final AccessService accessService;
    private final SecureRandom secureRandom = new SecureRandom();

    public ApiKeyService(ApiKeyRepository apiKeyRepository, AccessService accessService) {
        this.apiKeyRepository = apiKeyRepository;
        this.accessService = accessService;
    }

    @Transactional(readOnly = true)
    public List<ApiKeyResponse> list(UUID orgId) {
        accessService.requireOrgAdmin(orgId);
        return apiKeyRepository.findByOrganizationIdAndRevokedAtIsNullOrderByCreatedAtDesc(orgId).stream()
                .map(k -> new ApiKeyResponse(
                        k.getId(), k.getOrganizationId(), k.getProjectId(), k.getName(), k.getKeyPrefix(), k.getCreatedAt()))
                .toList();
    }

    @Transactional
    public ApiKeyCreatedResponse create(UUID orgId, CreateApiKeyRequest request) {
        accessService.requireOrgAdmin(orgId);
        if (request.projectId() != null) {
            Project project = accessService.requireProject(request.projectId());
            if (!project.getOrganizationId().equals(orgId)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Project not in organization");
            }
        }
        String raw = "qs_" + HexFormat.of().formatHex(randomBytes(24));
        String prefix = raw.substring(0, Math.min(12, raw.length()));
        ApiKey key = new ApiKey();
        key.setOrganizationId(orgId);
        key.setProjectId(request.projectId());
        key.setName(request.name().trim());
        key.setKeyPrefix(prefix);
        key.setKeyHash(TokenHasher.sha256(raw));
        key = apiKeyRepository.save(key);
        return new ApiKeyCreatedResponse(
                key.getId(),
                key.getOrganizationId(),
                key.getProjectId(),
                key.getName(),
                key.getKeyPrefix(),
                raw,
                key.getCreatedAt());
    }

    @Transactional
    public void revoke(UUID orgId, UUID keyId) {
        accessService.requireOrgAdmin(orgId);
        ApiKey key = apiKeyRepository
                .findById(keyId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found"));
        if (!key.getOrganizationId().equals(orgId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "API key not found");
        }
        key.setRevokedAt(Instant.now());
        apiKeyRepository.save(key);
    }

    private byte[] randomBytes(int n) {
        byte[] bytes = new byte[n];
        secureRandom.nextBytes(bytes);
        return bytes;
    }
}
