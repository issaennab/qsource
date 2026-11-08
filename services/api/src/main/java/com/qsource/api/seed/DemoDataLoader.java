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
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
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

/**
 * Public sample: one QSource workspace + project mirroring PLAN.md / ROADMAP.md.
 * No personal Life project and no real client backlogs (see docs/private-data.md).
 */
@Component
public class DemoDataLoader implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(DemoDataLoader.class);

    public static final String DEMO_EMAIL = "issa@qsource.io";
    public static final String DEMO_PASSWORD = "demo1234";
    public static final String TEAMMATE_EMAIL = "alex@qsource.io";

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
        if (!seed) {
            return;
        }

        UserAccount issa = ensureUser(DEMO_EMAIL, "Issa", DEMO_PASSWORD);
        UserAccount teammate = ensureUser(TEAMMATE_EMAIL, "Alex", DEMO_PASSWORD);
        log.info(
                "Sample logins ready: {} / {} (teammate {} / {})",
                DEMO_EMAIL,
                DEMO_PASSWORD,
                TEAMMATE_EMAIL,
                DEMO_PASSWORD);

        if (!projectRepository.findAll().isEmpty()) {
            return;
        }
        try {
            AuthPrincipal principal = AuthPrincipal.user(issa.getId(), issa.getDisplayName(), issa.getEmail());
            SecurityContextHolder.getContext()
                    .setAuthentication(new UsernamePasswordAuthenticationToken(
                            principal, null, List.of(new SimpleGrantedAuthority("ROLE_USER"))));
            ActorContext.set(new Actor(issa.getDisplayName(), RevisionSource.WEB, issa.getId()));

            Organization qsourceOrg = ensureOrgOwnedBy("QSource", "qsource", issa);
            if (!organizationMemberRepository.existsByOrganizationIdAndUserId(qsourceOrg.getId(), teammate.getId())) {
                OrganizationMember member = new OrganizationMember();
                member.setOrganizationId(qsourceOrg.getId());
                member.setUserId(teammate.getId());
                member.setRole(OrgRole.MEMBER);
                organizationMemberRepository.save(member);
            }

            seedQSourceProject(qsourceOrg.getId());

            log.info("Seeded QSource workspace + project from PLAN/ROADMAP. Login {} / {}", DEMO_EMAIL, DEMO_PASSWORD);
        } finally {
            ActorContext.clear();
            SecurityContextHolder.clearContext();
        }
    }

    private void seedQSourceProject(UUID orgId) {
        ProjectResponse project = projectService.createOrgProject(
                orgId,
                new CreateProjectRequest(
                        "QSource",
                        "Build QSource itself — ticket board, planning, revisions, agents (PLAN.md / ROADMAP.md).",
                        ProjectVisibility.ORG));

        TagResponse platform = tagService.create(orgId, new CreateTagRequest("platform", "#1d4ed8"));
        TagResponse agents = tagService.create(orgId, new CreateTagRequest("agents", "#0f766e"));
        TagResponse ux = tagService.create(orgId, new CreateTagRequest("ux", "#7c3aed"));
        TagResponse infra = tagService.create(orgId, new CreateTagRequest("infra", "#475569"));

        FeatureDetail uc1 = projectService.createFeature(
                project.id(),
                new CreateFeatureRequest(
                        "UC1 — Ticket-first board",
                        "Daily loop: TODO → IN_PROGRESS → DONE across workspaces.",
                        "Kanban board; drag across columns; reorder within column; full-width web shell; ticket detail.",
                        "Humans move and prioritize tickets without leaving the board.",
                        "Aligned with PLAN UC1"));
        tagService.setFeatureTags(uc1.id(), new SetTagsRequest(List.of(platform.id(), ux.id())));

        FeatureDetail uc3 = projectService.createFeature(
                project.id(),
                new CreateFeatureRequest(
                        "UC3 — Workspaces & invites",
                        "Slack-style workspaces and people invites for collaboration.",
                        "Workspace switcher; create workspace; People panel invite existing users; MEMBER role.",
                        "Owners add teammates by email without sharing passwords.",
                        "Aligned with PLAN UC3"));
        tagService.setFeatureTags(uc3.id(), new SetTagsRequest(List.of(platform.id())));

        FeatureDetail uc2 = projectService.createFeature(
                project.id(),
                new CreateFeatureRequest(
                        "UC2 — Agent API & MCP",
                        "Agents use scoped API keys and MCP against the same source of truth.",
                        "X-Api-Key; hashed secrets; show-once create; Settings keys UI; MCP stdio tools.",
                        "An agent can list projects, create tickets, and publish revisions with a key.",
                        "Aligned with PLAN UC2"));
        tagService.setFeatureTags(uc2.id(), new SetTagsRequest(List.of(agents.id())));

        FeatureDetail revisions = projectService.createFeature(
                project.id(),
                new CreateFeatureRequest(
                        "Immutable revisions",
                        "Meaningful edits publish attributable revisions; comments stay non-authoritative until promoted.",
                        "Task/feature snapshots; actor + source; change message; history API; compare UI.",
                        "Product and agents can each publish without silently overwriting history.",
                        "Core safety model"));
        tagService.setFeatureTags(revisions.id(), new SetTagsRequest(List.of(platform.id())));

        FeatureDetail planning = projectService.createFeature(
                project.id(),
                new CreateFeatureRequest(
                        "Planning & features",
                        "Themes above the board so humans and agents share the same epic context.",
                        "Features; Planning Now/Next/Later; roadmap sequence; tickets linked to features.",
                        "Opening Planning shows themed work, not only a flat backlog.",
                        "Agent-readable product structure"));
        tagService.setFeatureTags(planning.id(), new SetTagsRequest(List.of(ux.id())));

        FeatureDetail polish = projectService.createFeature(
                project.id(),
                new CreateFeatureRequest(
                        "Polish & infrastructure",
                        "Production-ready deploy path and board/planning polish.",
                        "Postgres prod profile; Docker Compose; OpenAPI; hosted deploy; activity filters.",
                        "A contributor can run prod-shaped config from env vars with no secrets in git.",
                        "ROADMAP polish / infra"));
        tagService.setFeatureTags(polish.id(), new SetTagsRequest(List.of(infra.id())));

        FeatureDetail later = projectService.createFeature(
                project.id(),
                new CreateFeatureRequest(
                        "Later — mobile & integrations",
                        "Future surfaces after the web + agent core loop is solid.",
                        "React Native; webhooks; Slack/Discord; Markdown/CSV import; decision records.",
                        "Not near-term; kept visible for agents so they do not invent duplicate epics.",
                        "ROADMAP later"));

        // Roadmap = ordered milestones (not a duplicate of the feature list).
        projectService.addRoadmapItem(
                project.id(),
                new CreateRoadmapItemRequest(
                        "Ship ticket-first loop",
                        "Milestone: board + DnD + shell usable for daily work",
                        0,
                        uc1.id()));
        projectService.addRoadmapItem(
                project.id(),
                new CreateRoadmapItemRequest(
                        "Collaborate in workspaces",
                        "Milestone: switcher + invite existing teammates",
                        1,
                        uc3.id()));
        projectService.addRoadmapItem(
                project.id(),
                new CreateRoadmapItemRequest(
                        "Agents on MCP",
                        "Milestone: keys in Settings + MCP tools documented",
                        2,
                        uc2.id()));
        projectService.addRoadmapItem(
                project.id(),
                new CreateRoadmapItemRequest(
                        "Themes agents can read",
                        "Milestone: Planning populated so epics are unambiguous",
                        3,
                        planning.id()));
        projectService.addRoadmapItem(
                project.id(),
                new CreateRoadmapItemRequest(
                        "Production-ready deploy",
                        "Milestone: Postgres profile + hosted API/web",
                        4,
                        polish.id()));
        projectService.addRoadmapItem(
                project.id(),
                new CreateRoadmapItemRequest(
                        "Mobile & outbound integrations",
                        "Milestone: RN + webhooks/notifications (later)",
                        5,
                        later.id()));

        LocalDate base = LocalDate.now();
        record Item(UUID featureId, String title, String description, TaskStatus status, LocalDate due) {}

        List<Item> items = List.of(
                // UC1 — shipped
                new Item(
                        uc1.id(),
                        "SaaS multi-tenant backend (JWT, orgs, groups, tags, API keys)",
                        "Spring Boot API: memberships, visibility, tags, hashed API keys.",
                        TaskStatus.DONE,
                        base.minusDays(20)),
                new Item(
                        uc1.id(),
                        "Ticket board as primary UX",
                        "TODO / IN_PROGRESS / DONE columns as the daily loop.",
                        TaskStatus.DONE,
                        base.minusDays(18)),
                new Item(
                        uc1.id(),
                        "Drag-and-drop status moves (web)",
                        "Cross-column DnD via @dnd-kit; PUT /api/tasks/{id}/placement.",
                        TaskStatus.DONE,
                        base.minusDays(12)),
                new Item(
                        uc1.id(),
                        "Reorder tickets within a column",
                        "Priority order inside a status; persist sortOrder.",
                        TaskStatus.DONE,
                        base.minusDays(12)),
                new Item(
                        uc1.id(),
                        "Full-width responsive web shell",
                        "Desktop console; stack/scroll on narrow breakpoints — not phone-first.",
                        TaskStatus.DONE,
                        base.minusDays(16)),
                new Item(
                        uc1.id(),
                        "Collapse Access & Tags / planning behind secondary UI",
                        "Board first; settings/planning secondary tabs.",
                        TaskStatus.DONE,
                        base.minusDays(15)),
                new Item(
                        uc1.id(),
                        "Ticket detail: show parent feature + multi-assignee",
                        "Assignees are not revision authors. Multi-assignee on detail + board avatars.",
                        TaskStatus.TODO,
                        base.plusDays(7)),

                // UC3
                new Item(
                        uc3.id(),
                        "Slack-like workspace switcher + create workspace",
                        "One login; switch and create workspaces from the header.",
                        TaskStatus.DONE,
                        base.minusDays(14)),
                new Item(
                        uc3.id(),
                        "People: invite existing users by email",
                        "OWNER/ADMIN invite; MEMBER role; remove member.",
                        TaskStatus.DONE,
                        base.minusDays(10)),
                new Item(
                        uc3.id(),
                        "Public sample seed (QSource project only)",
                        "issa@qsource.io sample; no client backlogs in the public repo.",
                        TaskStatus.DONE,
                        base.minusDays(1)),
                new Item(
                        uc3.id(),
                        "Email invite for users who have not signed up yet",
                        "Magic-link / accept flow for net-new accounts.",
                        TaskStatus.TODO,
                        base.plusDays(21)),
                new Item(
                        uc3.id(),
                        "Per-project edit roles",
                        "Fine-grained who can edit beyond workspace membership.",
                        TaskStatus.TODO,
                        base.plusDays(28)),

                // UC2
                new Item(
                        uc2.id(),
                        "Settings: create / list / revoke API keys",
                        "Show secret once; hashed at rest; MCP env snippet in Settings.",
                        TaskStatus.DONE,
                        base.minusDays(9)),
                new Item(
                        uc2.id(),
                        "MCP stdio server + Cursor wiring docs",
                        "services/mcp tools over HTTP with X-Api-Key.",
                        TaskStatus.DONE,
                        base.minusDays(8)),
                new Item(
                        uc2.id(),
                        "Optional API key expiry (expiresAt)",
                        "Design default ~90 days when enabled; early dogfood may omit.",
                        TaskStatus.TODO,
                        base.plusDays(10)),
                new Item(
                        uc2.id(),
                        "Project-scoped agent keys in UI",
                        "Prefer project scope for agents when creating keys.",
                        TaskStatus.TODO,
                        base.plusDays(14)),

                // Revisions
                new Item(
                        revisions.id(),
                        "Publish feature/task revision with change message",
                        "Immutable snapshots; actor + source attribution.",
                        TaskStatus.DONE,
                        base.minusDays(19)),
                new Item(
                        revisions.id(),
                        "Ticket History UI for single-revision tickets",
                        "No fake v1↔v1 compare; show snapshot/timeline until v2 exists.",
                        TaskStatus.TODO,
                        base.plusDays(5)),
                new Item(
                        revisions.id(),
                        "Side-by-side revision compare with field highlights",
                        "Diff UI when comparing two versions.",
                        TaskStatus.TODO,
                        base.plusDays(14)),

                // Planning
                new Item(
                        planning.id(),
                        "Planning Now / Next / Later surface",
                        "Features grouped for product + agent orientation.",
                        TaskStatus.DONE,
                        base.minusDays(11)),
                new Item(
                        planning.id(),
                        "Seed features + roadmap from PLAN/ROADMAP",
                        "Public demo mirrors real QSource work themes.",
                        TaskStatus.DONE,
                        base.minusDays(1)),
                new Item(
                        planning.id(),
                        "Link tickets to features from create/edit",
                        "Pick parent feature when creating or editing a ticket.",
                        TaskStatus.TODO,
                        base.plusDays(8)),
                new Item(
                        planning.id(),
                        "Roadmap drag-and-drop sequencing",
                        "Reorder roadmap items; persist sequenceIndex.",
                        TaskStatus.TODO,
                        base.plusDays(18)),

                // Polish / infra
                new Item(
                        polish.id(),
                        "Postgres-ready production profile",
                        "application-prod.yml + env placeholders; seed off in prod.",
                        TaskStatus.DONE,
                        base.minusDays(1)),
                new Item(
                        polish.id(),
                        "Document private vs public data",
                        "docs/private-data.md — secrets and client data stay out of git.",
                        TaskStatus.DONE,
                        base.minusDays(1)),
                new Item(
                        polish.id(),
                        "Docker Compose for local Postgres",
                        "Compose file for contributors who want Postgres locally.",
                        TaskStatus.TODO,
                        base.plusDays(12)),
                new Item(
                        polish.id(),
                        "API OpenAPI export + Postman collection",
                        "Generated OpenAPI and a checked-in collection.",
                        TaskStatus.TODO,
                        base.plusDays(20)),
                new Item(
                        polish.id(),
                        "Activity feed filters (human vs agent)",
                        "Filter activity by actor source.",
                        TaskStatus.TODO,
                        base.plusDays(16)),
                new Item(
                        polish.id(),
                        "Hosted deploy (API + web)",
                        "Document deploy; keep AWS/DB credentials in private env only.",
                        TaskStatus.TODO,
                        base.plusDays(30)),

                // Later
                new Item(
                        later.id(),
                        "Fine-grained RBAC UI",
                        "Who can edit / do — beyond OWNER/ADMIN/MEMBER.",
                        TaskStatus.TODO,
                        base.plusDays(45)),
                new Item(
                        later.id(),
                        "React Native + TestFlight",
                        "Separate mobile UX; do not force web layout onto phones.",
                        TaskStatus.TODO,
                        base.plusDays(60)),
                new Item(
                        later.id(),
                        "Webhooks for revision published",
                        "Notify external systems when a revision is accepted.",
                        TaskStatus.TODO,
                        base.plusDays(50)),
                new Item(
                        later.id(),
                        "Slack / Discord notifications",
                        "Optional channel notifications for board events.",
                        TaskStatus.TODO,
                        base.plusDays(55)),
                new Item(
                        later.id(),
                        "Import from Markdown / CSV",
                        "Bulk import tickets and features.",
                        TaskStatus.TODO,
                        base.plusDays(65)));

        for (Item item : items) {
            projectService.createTask(
                    project.id(),
                    new CreateTaskRequest(
                            item.featureId(),
                            item.title(),
                            item.description(),
                            item.status(),
                            item.due(),
                            "From PLAN.md / ROADMAP.md"));
        }
    }

    private UserAccount ensureUser(String email, String displayName, String password) {
        return userAccountRepository
                .findByEmailIgnoreCase(email)
                .map(existing -> {
                    if (!passwordEncoder.matches(password, existing.getPasswordHash())) {
                        existing.setPasswordHash(passwordEncoder.encode(password));
                    }
                    if (existing.getDisplayName() == null || existing.getDisplayName().isBlank()) {
                        existing.setDisplayName(displayName);
                    }
                    return userAccountRepository.save(existing);
                })
                .orElseGet(() -> {
                    UserAccount created = new UserAccount();
                    created.setEmail(email.toLowerCase());
                    created.setDisplayName(displayName);
                    created.setPasswordHash(passwordEncoder.encode(password));
                    return userAccountRepository.save(created);
                });
    }

    private Organization ensureOrgOwnedBy(String name, String slug, UserAccount owner) {
        Organization org = organizationRepository.findBySlug(slug).orElseGet(() -> {
            Organization created = authService.createOrganization(name);
            created.setSlug(slug);
            return organizationRepository.save(created);
        });
        if (!organizationMemberRepository.existsByOrganizationIdAndUserId(org.getId(), owner.getId())) {
            OrganizationMember member = new OrganizationMember();
            member.setOrganizationId(org.getId());
            member.setUserId(owner.getId());
            member.setRole(OrgRole.OWNER);
            organizationMemberRepository.save(member);
        }
        return org;
    }
}
