package com.qsource.api.repo;

import com.qsource.api.domain.OrgGroup;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrgGroupRepository extends JpaRepository<OrgGroup, UUID> {
    List<OrgGroup> findByOrganizationIdOrderByNameAsc(UUID organizationId);
}
