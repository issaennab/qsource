package com.qsource.api.repo;

import com.qsource.api.domain.Comment;
import com.qsource.api.domain.TargetType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(TargetType targetType, UUID targetId);
}
