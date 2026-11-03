package com.coord.api.seed;

import com.coord.api.config.Actor;
import com.coord.api.config.ActorContext;
import com.coord.api.domain.RevisionSource;
import com.coord.api.dto.Dtos.*;
import com.coord.api.domain.TaskStatus;
import com.coord.api.repo.ProjectRepository;
import com.coord.api.service.ProjectService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoDataLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final boolean seed;

    public DemoDataLoader(
            ProjectService projectService,
            ProjectRepository projectRepository,
            @Value("${coord.seed:true}") boolean seed) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.seed = seed;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seed || !projectRepository.findAll().isEmpty()) {
            return;
        }
        try {
            ActorContext.set(new Actor("sarah", RevisionSource.WEB));
            ProjectResponse project = projectService.createProject(new CreateProjectRequest(
                    "Coord itself",
                    "Dogfood project: plan Coord using Coord. Versioned features for humans and agents."));

            projectService.addRoadmapItem(
                    project.id(),
                    new CreateRoadmapItemRequest(
                            "Thin vertical loop",
                            "Roadmap + features + tasks + immutable revisions + web UI",
                            0,
                            null));
            projectService.addRoadmapItem(
                    project.id(),
                    new CreateRoadmapItemRequest(
                            "Agent API + MCP", "Scoped agent access and Python MCP wrapper", 1, null));
            projectService.addRoadmapItem(
                    project.id(),
                    new CreateRoadmapItemRequest("Mobile", "React Native + TestFlight for on-the-go review", 2, null));

            FeatureDetail feature = projectService.createFeature(
                    project.id(),
                    new CreateFeatureRequest(
                            "Immutable revisions",
                            "Every meaningful edit creates an attributable revision; latest accepted is the working view.",
                            "Feature and task snapshots; actor + source; parent revision; change message; history API.",
                            "A product lead and an agent can each publish revisions without overwriting history.",
                            "Initial product definition"));

            ActorContext.set(new Actor("chatgpt", RevisionSource.API));
            projectService.publishFeature(
                    feature.id(),
                    new PublishFeatureRevisionRequest(
                            "Immutable revisions",
                            "Every meaningful edit creates an attributable revision; latest accepted is the working view.",
                            "Feature and task snapshots; actor + source; parent revision; change message; history API.\n"
                                    + "Comments stay non-authoritative until promoted.",
                            "A product lead and an agent can each publish revisions without overwriting history.\n"
                                    + "History drawer shows v1..vn with actor and source.",
                            "Clarified requirements with ChatGPT"));

            ActorContext.set(new Actor("eng-agent", RevisionSource.AGENT));
            projectService.publishFeature(
                    feature.id(),
                    new PublishFeatureRevisionRequest(
                            "Immutable revisions",
                            "Every meaningful edit creates an attributable revision; latest accepted is the working view.",
                            "Store content as JSON CLOB; version is monotonic per target; currentRevisionId on Feature/Task.",
                            "API tests cover create + publish + history order; web shows history drawer.",
                            "Added technical findings from coding agent"));

            ActorContext.set(new Actor("sarah", RevisionSource.WEB));
            projectService.createTask(
                    project.id(),
                    new CreateTaskRequest(
                            feature.id(),
                            "Wire feature history drawer in web UI",
                            "Show version, actor, source, message; open snapshot.",
                            TaskStatus.IN_PROGRESS,
                            "Initial task"));

            projectService.createTask(
                    project.id(),
                    new CreateTaskRequest(
                            feature.id(),
                            "Document agent header contract",
                            "X-Actor-Name + X-Actor-Source for agents calling the API.",
                            TaskStatus.TODO,
                            "Initial task"));

            log.info("Seeded demo project 'Coord itself' id={}", project.id());
        } finally {
            ActorContext.clear();
        }
    }
}
