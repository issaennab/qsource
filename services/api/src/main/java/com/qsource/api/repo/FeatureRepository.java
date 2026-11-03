package com.qsource.api.repo;

import com.qsource.api.domain.Feature;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeatureRepository extends JpaRepository<Feature, UUID> {
    List<Feature> findByProjectIdOrderByUpdatedAtDesc(UUID projectId);
}
