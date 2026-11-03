package com.qsource.api.repo;

import com.qsource.api.domain.RoadmapItem;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoadmapItemRepository extends JpaRepository<RoadmapItem, UUID> {
    List<RoadmapItem> findByProjectIdOrderBySequenceIndexAsc(UUID projectId);
}
