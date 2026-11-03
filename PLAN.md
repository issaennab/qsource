# QSource — agent handoff plan

**Repo:** https://github.com/issaennab/qsource  
**Local path:** `/Users/issaennab/dev/work/workspace/qsource`  
**License:** MIT  
**Owner:** personal sole-owner side project (not AluFit / company product)

Use this file as the source of truth for what exists, how to run it, and what to build next. Prefer small commits by phase. Do not introduce JWT/auth unless explicitly asked (v1 uses actor headers only).

---

## Product

QSource is a lightweight product-planning platform for **humans and AI agents**.

- Roadmaps → Features → Tasks
- **Immutable revisions** on features/tasks (full snapshot + actor + source + change message + parent revision)
- Comments are non-authoritative until promoted into a revision
- Activity timeline for project events

Goal: shared source of truth that coding agents can read and update without silently overwriting requirements (better than Notion/Jira copy-paste for agent workflows).

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

Future (not started): `apps/mobile` (React Native + TestFlight).

---

## Done (Phase 0–2)

### Phase 0 — bootstrap
- Public GitHub repo, MIT, README, ROADMAP, monorepo scaffold

### Phase 1 — thin vertical loop
- Spring Boot API: org/project/roadmap/feature/task/revision/comment/activity
- Revision publish + history APIs
- H2 local DB (Postgres-ready later); seed project **“QSource itself”** when `qsource.seed=true`
- React web: project list, board (roadmap/features/tasks/activity), feature detail with publish + history + comments
- Actor bar in UI; no login
- API test: `RevisionFlowTest`

### Phase 2 — MCP (started)
- `services/mcp` FastMCP/`MCPServer` stdio server
- Tools: `list_projects`, `get_project_board`, `get_feature`, `list_feature_revisions`, `publish_feature_revision`, `list_tasks`, `publish_task_revision`, `add_feature_comment`
- Calls API with `X-Actor-Name` + `X-Actor-Source: agent`

---

## Identity model (important)

**No JWT / no Spring Security yet.** Attribution only:

| Header | Example | Notes |
|--------|---------|-------|
| `X-Actor-Name` | `sarah` / `mcp-agent` | Who acted |
| `X-Actor-Source` | `web` \| `api` \| `agent` | Defaults to `api` if missing |

Web sets these from the top-bar actor controls. MCP sets them from env (`QSOURCE_ACTOR_NAME`, `QSOURCE_ACTOR_SOURCE`).

---

## How to run locally

### API (required for web + MCP)

```bash
export JAVA_HOME=/Library/Java/JavaVirtualMachines/jdk-17.jdk/Contents/Home   # or your JDK 17
cd services/api
./gradlew bootRun
# http://localhost:8095/actuator/health
# Seeded login-less demo: project "QSource itself"
```

Port **8095** (8080 often taken). Override with `server.port` if needed.

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
export QSOURCE_ACTOR_NAME=mcp-agent
export QSOURCE_ACTOR_SOURCE=agent
qsource-mcp   # stdio
```

Cursor MCP config example: see `services/mcp/README.md`.

### Tests

```bash
cd services/api && ./gradlew test
cd services/mcp && source .venv/bin/activate && pytest
```

---

## Key API surface

Base: `http://localhost:8095`

- `GET/POST /api/projects`
- `GET /api/projects/{id}/board`
- `POST /api/projects/{id}/roadmap|features|tasks`
- `GET /api/features/{id}`
- `GET|POST /api/features/{id}/revisions`
- `GET|POST /api/features/{id}/comments`
- Same pattern for `/api/tasks/{id}/...`

Details: `docs/api.md`.

Java package root: `com.qsource.api`  
Main class: `com.qsource.api.QSourceApplication`

---

## Suggested next work (pick in order unless user redirects)

1. **MCP hardening**
   - Integration test against running API (or Testcontainers/WireMock)
   - Tool for `create_feature` / `create_task` if agents need bootstrap without web
   - Document/verify Cursor MCP wiring end-to-end

2. **Phase 1 polish (dogfood)**
   - Side-by-side revision diff in web
   - Postgres profile + `docker-compose.yml`
   - OpenAPI export / Postman collection
   - Roadmap reorder (sequenceIndex updates)

3. **Mobile**
   - `apps/mobile` Expo/React Native: read board + publish feature revision
   - Personal Apple account / TestFlight later

4. **Auth (only when asked)**
   - JWT or OAuth; keep actor attribution on revisions
   - Do not block current agent loop on auth

5. **Hosted deploy**
   - Cheap Postgres + API + static web

See also `ROADMAP.md` for the longer checklist.

---

## Conventions for agents

- **Do not** commit secrets; use `.env.example` only
- **Do not** squash history into one commit for CC-CLI optics; prefer phase-sized commits
- Keep comments separate from revisions; publishing requires a `changeMessage`
- Prefer editing existing patterns in `ProjectService` / web pages over new frameworks
- AluFit / fitness-core / company GitLab code is out of scope for this repo
- Ask before expanding fixtures/scope into large unrelated refactors

---

## CC-CLI context

This repo is intended as a BYOP/CC-CLI candidate: personal public GitHub, MIT, real commits, ≥20h runway in `ROADMAP.md`. Submit description (≤350 chars) already drafted in chat; form project name: **qsource**.

---

## Quick “first task” if unsure where to start

1. Start API + web; open seeded **QSource itself** project.
2. Wire Cursor to `services/mcp` and call `get_project_board` on the seeded project id.
3. Implement the highest-value gap you hit (usually create-feature via MCP, revision diff UI, or Postgres compose).
