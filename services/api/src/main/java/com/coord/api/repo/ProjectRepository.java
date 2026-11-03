package com.coord.api.repo;

import com.coord.api.domain.Project;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, UUID> {
    List<Project> findByOrganizationIdOrderByCreatedAtAsc(UUID organizationId);
}
