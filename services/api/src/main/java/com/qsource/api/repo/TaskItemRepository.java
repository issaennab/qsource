package com.qsource.api.repo;

import com.qsource.api.domain.TaskItem;
import com.qsource.api.domain.TaskStatus;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskItemRepository extends JpaRepository<TaskItem, UUID> {
    List<TaskItem> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);

    List<TaskItem> findByProjectIdOrderByStatusAscSortOrderAscCreatedAtAsc(UUID projectId);

    List<TaskItem> findByFeatureIdOrderByUpdatedAtDesc(UUID featureId);

    List<TaskItem> findByProjectIdAndStatusOrderBySortOrderAscCreatedAtAsc(UUID projectId, TaskStatus status);
}
