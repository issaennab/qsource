package com.qsource.api.seed;

import com.qsource.api.config.Actor;
import com.qsource.api.config.ActorContext;
import com.qsource.api.domain.OrgRole;
import com.qsource.api.domain.Organization;
import com.qsource.api.domain.OrganizationMember;
import com.qsource.api.domain.ProjectVisibility;
import com.qsource.api.domain.RevisionSource;
import com.qsource.api.domain.TaskStatus;
import com.qsource.api.domain.UserAccount;
import com.qsource.api.dto.Dtos.*;
import com.qsource.api.repo.OrganizationMemberRepository;
import com.qsource.api.repo.OrganizationRepository;
import com.qsource.api.repo.ProjectRepository;
import com.qsource.api.repo.UserAccountRepository;
import com.qsource.api.security.AuthPrincipal;
import com.qsource.api.service.AuthService;
import com.qsource.api.service.ProjectService;
import com.qsource.api.service.TagService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class DemoDataLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    private final ProjectService projectService;
    private final ProjectRepository projectRepository;
    private final UserAccountRepository userAccountRepository;
    private final OrganizationRepository organizationRepository;
    private final OrganizationMemberRepository organizationMemberRepository;
    private final AuthService authService;
    private final TagService tagService;
    private final PasswordEncoder passwordEncoder;
    private final boolean seed;

    public DemoDataLoader(
            ProjectService projectService,
            ProjectRepository projectRepository,
            UserAccountRepository userAccountRepository,
            OrganizationRepository organizationRepository,
            OrganizationMemberRepository organizationMemberRepository,
            AuthService authService,
            TagService tagService,
            PasswordEncoder passwordEncoder,
            @Value("${qsource.seed:true}") boolean seed) {
        this.projectService = projectService;
        this.projectRepository = projectRepository;
        this.userAccountRepository = userAccountRepository;
        this.organizationRepository = organizationRepository;
        this.organizationMemberRepository = organizationMemberRepository;
        this.authService = authService;
        this.tagService = tagService;
        this.passwordEncoder = passwordEncoder;
        this.seed = seed;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!seed || !projectRepository.findAll().isEmpty()) {
            return;
        }
        try {
            UserAccount user = userAccountRepository.findByEmailIgnoreCase("demo@qsource.local").orElseGet(() -> {
                UserAccount created = new UserAccount();
                created.setEmail("demo@qsource.local");
                created.setDisplayName("Sarah");
                created.setPasswordHash(passwordEncoder.encode("demo1234"));
                return userAccountRepository.save(created);
            });

            Organization org = organizationRepository.findBySlug("qsource-demo").orElseGet(() -> {
                Organization created = authService.createOrganization("QSource Demo");
                created.setSlug("qsource-demo");
                return organizationRepository.save(created);
            });

            if (!organizationMemberRepository.existsByOrganizationIdAndUserId(org.getId(), user.getId())) {
                OrganizationMember member = new OrganizationMember();
                member.setOrganizationId(org.getId());
                member.setUserId(user.getId());
                member.setRole(OrgRole.OWNER);
                organizationMemberRepository.save(member);
            }

            AuthPrincipal principal = AuthPrincipal.user(user.getId(), user.getDisplayName(), user.getEmail());
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            ActorContext.set(new Actor(user.getDisplayName(), RevisionSource.WEB, user.getId()));

            ProjectResponse project = projectService.createOrgProject(
                    org.getId(),
                    new CreateProjectRequest(
                            "QSource itself",
                            "Dogfood project: plan QSource using QSource. Versioned features for humans and agents.",
                            ProjectVisibility.ORG));

            TagResponse clientSupport = tagService.create(org.getId(), new CreateTagRequest("client-support", "#0f766e"));
            tagService.create(org.getId(), new CreateTagRequest("platform", "#1d4ed8"));

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
            tagService.setFeatureTags(feature.id(), new SetTagsRequest(java.util.List.of(clientSupport.id())));

            ActorContext.set(new Actor("chatgpt", RevisionSource.API, null));
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

            ActorContext.set(new Actor("eng-agent", RevisionSource.AGENT, null));
            projectService.publishFeature(
                    feature.id(),
                    new PublishFeatureRevisionRequest(
                            "Immutable revisions",
                            "Every meaningful edit creates an attributable revision; latest accepted is the working view.",
                            "Store content as JSON CLOB; version is monotonic per target; currentRevisionId on Feature/Task.",
                            "API tests cover create + publish + history order; web shows history drawer.",
                            "Added technical findings from coding agent"));

            ActorContext.set(new Actor(user.getDisplayName(), RevisionSource.WEB, user.getId()));
            TaskDetail task = projectService.createTask(
                    project.id(),
                    new CreateTaskRequest(
                            feature.id(),
                            "Wire feature history drawer in web UI",
                            "Show version, actor, source, message; open snapshot.",
                            TaskStatus.IN_PROGRESS,
                            "Initial task"));
            tagService.setTaskTags(task.id(), new SetTagsRequest(java.util.List.of(clientSupport.id())));

            projectService.createTask(
                    project.id(),
                    new CreateTaskRequest(
                            feature.id(),
                            "Document agent header contract",
                            "X-Api-Key / JWT for agents calling the API.",
                            TaskStatus.TODO,
                            "Initial task"));

            log.info(
                    "Seeded demo user demo@qsource.local / demo1234, org={}, project 'QSource itself' id={}",
                    org.getId(),
                    project.id());
        } finally {
            ActorContext.clear();
            SecurityContextHolder.clearContext();
        }
    }
}
