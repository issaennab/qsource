package com.coord.api.repo;

import com.coord.api.domain.Revision;
import com.coord.api.domain.TargetType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RevisionRepository extends JpaRepository<Revision, UUID> {
    List<Revision> findByTargetTypeAndTargetIdOrderByVersionDesc(TargetType targetType, UUID targetId);
    Optional<Revision> findFirstByTargetTypeAndTargetIdOrderByVersionDesc(TargetType targetType, UUID targetId);
}
