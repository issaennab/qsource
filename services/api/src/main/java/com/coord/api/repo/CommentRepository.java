package com.coord.api.repo;

import com.coord.api.domain.Comment;
import com.coord.api.domain.TargetType;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommentRepository extends JpaRepository<Comment, UUID> {
    List<Comment> findByTargetTypeAndTargetIdOrderByCreatedAtAsc(TargetType targetType, UUID targetId);
}
