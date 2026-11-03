package com.coord.api.service;

import com.coord.api.config.ActorContext;
import com.coord.api.domain.Revision;
import com.coord.api.domain.TargetType;
import com.coord.api.repo.RevisionRepository;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RevisionService {
    private final RevisionRepository revisionRepository;
    private final JsonMaps jsonMaps;

    public RevisionService(RevisionRepository revisionRepository, JsonMaps jsonMaps) {
        this.revisionRepository = revisionRepository;
        this.jsonMaps = jsonMaps;
    }

    @Transactional
    public Revision publish(TargetType type, UUID targetId, UUID parentId, Map<String, Object> content, String message) {
        var actor = ActorContext.get();
        int nextVersion = revisionRepository
                .findFirstByTargetTypeAndTargetIdOrderByVersionDesc(type, targetId)
                .map(r -> r.getVersion() + 1)
                .orElse(1);

        Revision revision = new Revision();
        revision.setTargetType(type);
        revision.setTargetId(targetId);
        revision.setVersion(nextVersion);
        revision.setContentJson(jsonMaps.write(content));
        revision.setMessage(message == null || message.isBlank() ? "Update" : message);
        revision.setActorName(actor.name());
        revision.setSource(actor.source());
        revision.setParentRevisionId(parentId);
        revision.setAccepted(true);
        return revisionRepository.save(revision);
    }

    @Transactional(readOnly = true)
    public List<Revision> history(TargetType type, UUID targetId) {
        return revisionRepository.findByTargetTypeAndTargetIdOrderByVersionDesc(type, targetId);
    }

    @Transactional(readOnly = true)
    public Revision require(UUID id) {
        return revisionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Revision not found"));
    }

    public Map<String, Object> contentOf(Revision revision) {
        return jsonMaps.read(revision.getContentJson());
    }
}
