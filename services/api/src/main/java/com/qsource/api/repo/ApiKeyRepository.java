package com.qsource.api.repo;

import com.qsource.api.domain.ApiKey;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    Optional<ApiKey> findByKeyHashAndRevokedAtIsNull(String keyHash);
    List<ApiKey> findByOrganizationIdAndRevokedAtIsNullOrderByCreatedAtDesc(UUID organizationId);
}
