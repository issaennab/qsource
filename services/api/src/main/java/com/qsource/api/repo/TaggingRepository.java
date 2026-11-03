package com.qsource.api.repo;

import com.qsource.api.domain.Tagging;
import com.qsource.api.domain.TargetType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaggingRepository extends JpaRepository<Tagging, UUID> {
    List<Tagging> findByTargetTypeAndTargetId(TargetType targetType, UUID targetId);
    List<Tagging> findByTagId(UUID tagId);
    void deleteByTargetTypeAndTargetId(TargetType targetType, UUID targetId);
    void deleteByTagId(UUID tagId);
}
