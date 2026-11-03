package com.coord.api.service;

import com.coord.api.config.ActorContext;
import com.coord.api.domain.ActivityEvent;
import com.coord.api.repo.ActivityEventRepository;
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
        event.setSource(actor.source());
        event.setRelatedId(relatedId);
        activityEventRepository.save(event);
    }
}
