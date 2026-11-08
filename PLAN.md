# QSource — agent handoff plan

**Repo:** https://github.com/issaennab/qsource  
**Local path:** `/Users/issaennab/dev/work/workspace/qsource`  
**License:** MIT  
**Owner:** personal sole-owner side project ([qsource.io](https://qsource.io)). Real client/startup dogfood stays **out of this public repo** — see [docs/private-data.md](docs/private-data.md).

Use this file as the source of truth for what exists, how to run it, and what to build next. Prefer small commits by phase. **Over-design the model, under-deliver the UI** — ship use case by use case; avoid migrations that could have been designed in.

---

## Product

QSource is a lightweight **ticket + planning** platform for **humans and AI agents**.

One place for all work: grocery lists, personal todos, startup projects, and company projects. Humans use the web; agents use **MCP / API** with a scoped key. Collaboration is “add people to a workspace/project,” not Jira.

### Core loop

- **Tickets** move **TODO → IN_PROGRESS → DONE** (primary UX)
- Features / roadmaps / immutable revisions sit underneath for deeper planning and agent-safe history
- Comments are non-authoritative until promoted into a revision
- Activity timeline for project events

Goal: shared source of truth that coding agents can read and update without silently overwriting requirements.

---

## Domain model (design for it)

| Layer | Meaning | Ship now | Design for later (schema OK, UI thin) |
|-------|---------|----------|----------------------------------------|
| **Workspace** (`Organization`) | Personal *or* company (same type) | Create on signup; switch between workspaces | Billing, SSO |
| **Project** | Bucket of tickets | Create/list; visibility ORG \| RESTRICTED | Fine-grained “who can edit” |
| **Ticket** (`Task`) | TODO → IN_PROGRESS → DONE | Create + status; `createdAt`, description, `estimatedDeliveryDate` | Assignees, priority, custom fields |
| **Feature / Roadmap** | Deeper planning + revisions | Keep APIs + secondary UI | Diff UI, sequencing |
| **Member + role** | OWNER / ADMIN / MEMBER | Membership exists | Per-project permissions, invite UX |
| **Groups + ACL** | RESTRICTED project access | Backend + collapsed settings | Full ACL UI |
| **Tags** | Org-scoped labels | Backend + collapsed filter | Conventions, board polish |
| **Agent API key** | Scoped credential for MCP | Create / list / revoke (API exists) | Expiry, project scope UI, rotate |

**Public sample seed** (`issa@qsource.io` / `demo1234`):

| Workspace | What lives there |
|-----------|------------------|
| **QSource** | Single project with board + features + planning + roadmap — the real QSource backlog from `PLAN.md` / `ROADMAP.md` |

Create more workspaces anytime from the header. Private client projects are never seeded here.

---

## Three major use cases (build order)

### UC1 — Personal ticket loop
Ticket-first board: add tickets, drag TODO / IN_PROGRESS / DONE. Personal *and* company use the same path.

### UC2 — Agent access via unique key
Settings: create / list / revoke API keys (show secret once). Wire MCP with `X-Api-Key`. Optional expiry + project scope. Humans re-login with email/password; keys are separate.

### UC3 — Invite people ← current slice
Add members to a workspace by email (OWNER/ADMIN). V1: invitee must already have an account; email magic-links later. Sample teammate: `alex@qsource.io`.

Then: MCP/API ticket design polish, tag conventions, hosted deploy. Native mobile is **not** a near-term focus.

---

## API key security (defaults)

A key is a long random secret (`qs_…`), stored as a **hash**, shown **once** at creation. It identifies a workspace (and optionally a project)—not a human password.

| Practice | Rule |
|----------|------|
| Storage | Hash only; never log or re-display full secret |
| Show once | Copy at create; list shows prefix + name + dates |
| Revoke | Immediate kill; create a new key when needed |
| Rotate | Revoke old + create new (do not refresh same secret) |
| Expiry | Design for optional TTL (default **90 days** when we add it); early dogfood may omit |
| Human vs agent | Login ≠ key; Settings → API keys is the agent door |
| Scope | Prefer project-scoped keys for agents when possible |
| Client storage | MCP `env` / local secrets only — never commit keys |

**Not needed yet:** OAuth-for-agents, device flow, short-lived JWT exchange for agents. Hash + show-once + revoke + optional expiry is enough and migration-friendly.

---

## Monorepo layout

```
qsource/
├── apps/web              # React + Vite + TypeScript console
├── services/api          # Java 17 / Spring Boot 3 API (port 8095)
├── services/mcp          # Python MCP server (stdio) wrapping the API
├── docs/                 # architecture, API, MCP, CC-CLI notes
├── PLAN.md               # this file
├── ROADMAP.md            # longer backlog
├── LICENSE               # MIT
└── README.md
```

**Clients**

- **Web (`apps/web`)** — full-width responsive desktop/laptop console (kanban drag-and-drop). Use the full viewport; stack or horizontal-scroll on narrow breakpoints. This is *not* a phone-first app.
- **Mobile (`apps/mobile`, later)** — separate React Native experience (touch-native board, different navigation). Do not force the web layout onto phones.

---

## Done

### Phase 0 — bootstrap
- Public GitHub repo, MIT, README, ROADMAP, monorepo scaffold

### Phase 1 — thin vertical loop
- Spring Boot API: org/project/roadmap/feature/task/revision/comment/activity
- Revision publish + history APIs
- H2 local DB (Postgres-ready later); seed when `qsource.seed=true`
- React web: project list, board, feature detail with publish + history + comments
- API test: `RevisionFlowTest`

### Phase 2 — MCP
- `services/mcp` FastMCP/`MCPServer` stdio server
- Tools: list projects/board/feature/tasks, publish revisions, comments
- Auth via `X-Api-Key` (replacing unsigned actor headers for agents)

### Phase 3 — SaaS multi-tenant (backend + thin UI)
- Users, JWT signup/login/refresh, org membership + roles
- Org-scoped projects; ProjectVisibility ORG \| RESTRICTED
- Groups + project_access ACL enforcement
- Org tags + feature/task taggings + board filter
- Web: auth, org/workspace switcher, project privacy + tag UI
- API keys: create / list / revoke (hashed); MCP client uses key

---

## Identity model

| Client | Auth | Notes |
|--------|------|-------|
| Web / human | `Authorization: Bearer <JWT>` | Signup creates a workspace; actor attribution still on revisions |
| MCP / agent | `X-Api-Key: qs_…` | Key maps to org (+ optional project); never commit |
| Attribution | `X-Actor-Source: web \| api \| agent` | Kept on revisions/activity |

Legacy unsigned `X-Actor-Name` without JWT/key is **not** the path forward.

---

## How to run locally

### API (required for web + MCP)

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home   # or your JDK 17
cd services/api
./gradlew bootRun
# http://localhost:8095/actuator/health
```

Port **8095**. If H2 schema drifts after entity changes, clear `services/api/data/` and restart. If port is busy, kill the leftover `bootRun` JVM.

### Web

```bash
cd apps/web
npm install
npm run dev
# http://localhost:5173  (Vite proxies /api → :8095)
```

### MCP

```bash
cd services/mcp
python3 -m venv .venv && source .venv/bin/activate
pip install -e .
export QSOURCE_API_BASE_URL=http://localhost:8095
export QSOURCE_API_KEY=qs_…   # from Settings / create API key
qsource-mcp   # stdio
```

Cursor MCP config: see `services/mcp/README.md`.

### Tests

```bash
cd services/api && ./gradlew test
cd services/mcp && source .venv/bin/activate && pytest
```

---

## Key API surface

Base: `http://localhost:8095`

- Auth: `POST /api/auth/signup|login|refresh|logout`, `GET /api/me`
- Orgs / projects: `/api/orgs`, `/api/orgs/{id}/projects`, `/api/projects/{id}`
- Board / features / tasks / revisions / comments (existing)
- Groups / access / tags / API keys under `/api/orgs/{id}/…` and `/api/projects/{id}/access`

Details: [docs/api.md](docs/api.md). Controllers: `com.qsource.api.web`.

---

## Suggested next work

1. **UC1 — Ticket-first UX**
   - [x] Board hero + drag-and-drop status
   - [x] Full-width web shell; planning secondary
   - [x] Workspace copy (personal + company same path)

2. **UC3 — Multi-workspace + invites** ← current
   - [x] Slack-like workspace switcher + create workspace
   - [x] People panel: list / invite by email / remove (existing accounts)
   - [x] Seed: QSource project from PLAN/ROADMAP (board / features / planning / roadmap)
   - [ ] Email invite links for users who have not signed up yet
   - [ ] Per-project edit roles later

3. **UC2 — API keys UI + MCP docs**
   - [x] Settings → Agent API keys (create show-once, list, revoke)
   - [ ] Optional `expiresAt` on `api_keys`
   - [x] MCP env snippet in Settings UI

Also shipped UI screens: ticket detail + discussion, revision history/compare, planning NOW/NEXT/LATER.

4. **Polish / infra**
   - Revision diff UI, Postgres + docker-compose, OpenAPI
   - Hosted deploy
   - [x] Generic `application-prod.yml` (env placeholders; secrets stay private)

See also `ROADMAP.md` and [docs/private-data.md](docs/private-data.md).

---

## Conventions for agents

- **Do not** commit secrets, real client tickets, or production connection values; use `.env.example` / `application-prod.yml` placeholders only
- Prefer phase-sized commits; avoid large unrelated refactors
- Keep comments separate from revisions; publishing requires a `changeMessage`
- Prefer editing existing patterns in `ProjectService` / web pages over new frameworks
- Design for roles/ACL/expiry in schema; ship thin UI per use case
- Client/startup dogfood data is **out of scope** for this public repo
- Ask before expanding fixtures/scope into large unrelated refactors

---

## CC-CLI context

BYOP/CC-CLI candidate: public GitHub [issaennab/qsource](https://github.com/issaennab/qsource), MIT, real commits, runway in `ROADMAP.md`. Form project name: **qsource**. Personal sole owner (not a company product).
