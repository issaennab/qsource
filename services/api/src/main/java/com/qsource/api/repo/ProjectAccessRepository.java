package com.qsource.api.repo;

import com.qsource.api.domain.ProjectAccess;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectAccessRepository extends JpaRepository<ProjectAccess, UUID> {
    List<ProjectAccess> findByProjectId(UUID projectId);
    void deleteByProjectId(UUID projectId);
}
