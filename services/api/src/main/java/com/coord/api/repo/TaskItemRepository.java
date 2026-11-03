package com.coord.api.repo;

import com.coord.api.domain.TaskItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskItemRepository extends JpaRepository<TaskItem, UUID> {
    List<TaskItem> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);
    List<TaskItem> findByFeatureIdOrderByUpdatedAtDesc(UUID featureId);
}
