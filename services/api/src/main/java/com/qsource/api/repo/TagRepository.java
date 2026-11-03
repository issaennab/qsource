package com.qsource.api.repo;

import com.qsource.api.domain.Tag;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TagRepository extends JpaRepository<Tag, UUID> {
    List<Tag> findByOrganizationIdOrderByNameAsc(UUID organizationId);
    Optional<Tag> findByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);
}
