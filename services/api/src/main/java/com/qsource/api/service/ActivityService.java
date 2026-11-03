package com.qsource.api.service;

import com.qsource.api.config.ActorContext;
import com.qsource.api.domain.ActivityEvent;
import com.qsource.api.repo.ActivityEventRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private final ActivityEventRepository activityEventRepository;

    public ActivityService(ActivityEventRepository activityEventRepository) {
        this.activityEventRepository = activityEventRepository;
    }

    @Transactional
    public void record(UUID projectId, String kind, String summary, UUID relatedId) {
        var actor = ActorContext.get();
        ActivityEvent event = new ActivityEvent();
        event.setProjectId(projectId);
        event.setKind(kind);
        event.setSummary(summary);
        event.setActorName(actor.name());
        event.setActorUserId(actor.userId());
        event.setSource(actor.source());
        event.setRelatedId(relatedId);
        activityEventRepository.save(event);
    }
}
