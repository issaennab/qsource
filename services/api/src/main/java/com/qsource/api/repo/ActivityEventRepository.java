package com.qsource.api.repo;

import com.qsource.api.domain.ActivityEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ActivityEventRepository extends JpaRepository<ActivityEvent, UUID> {
    List<ActivityEvent> findByProjectIdOrderByCreatedAtDesc(UUID projectId);
}
