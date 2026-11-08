# QSource

Lightweight **ticket + planning** platform for humans and AI agents.

One login, Slack-style **workspaces** (personal and company), **projects** with a ticket board (**To do → In progress → Done**), and optional roadmap/features with **immutable revisions** so agents never silently overwrite history.

Public site / brand: **[qsource.io](https://qsource.io)**.

## Docs

| Doc | What it’s for |
|-----|----------------|
| **[PLAN.md](PLAN.md)** | Source of truth — product model, use cases, security, what’s next |
| **[ROADMAP.md](ROADMAP.md)** | Backlog checklist |
| **[docs/architecture.md](docs/architecture.md)** | Domain, auth, revisions |
| **[docs/api.md](docs/api.md)** | HTTP API overview |
| **[docs/mcp.md](docs/mcp.md)** | Agent MCP entry → [services/mcp/README.md](services/mcp/README.md) |
| **[docs/private-data.md](docs/private-data.md)** | What stays out of the public repo (secrets, client data) |

## Stack

| Layer | Tech |
|-------|------|
| API | Java 17, Spring Boot 3, JPA, H2 (local) / Postgres (prod profile) |
| Web | React, TypeScript, Vite (full-width desktop console) |
| Agents | Python MCP + `X-Api-Key` |
| Later | React Native (separate mobile UX), hosted deploy |

## Monorepo

```
qsource/
├── apps/web           # Web console
├── services/api       # Spring Boot API (:8095)
├── services/mcp       # MCP server (stdio)
├── docs/              # Architecture, API, MCP, private-data
├── PLAN.md
├── ROADMAP.md
└── README.md
```

## Quick start

### 1. API

```bash
cd services/api
./gradlew bootRun
# http://localhost:8095/actuator/health
```

If the schema drifts after entity changes, stop the API, clear `services/api/data/`, and restart.

### 2. Web

```bash
cd apps/web
npm install
npm run dev
# http://localhost:5173  (proxies /api → :8095)
```

**Sample login** (`qsource.seed=true`):

- `issa@qsource.io` / `demo1234`
- Teammate (for invite demos): `alex@qsource.io` / `demo1234`

Seeded workspace: **QSource** — one project with board, features, planning, and roadmap mirrored from `PLAN.md` / `ROADMAP.md`.

### 3. MCP (agents)

```bash
cd services/mcp
python3 -m venv .venv && source .venv/bin/activate
pip install -e .
export QSOURCE_API_BASE_URL=http://localhost:8095
export QSOURCE_API_KEY=qs_…   # POST /api/orgs/{orgId}/api-keys
qsource-mcp
```

Cursor wiring: [services/mcp/README.md](services/mcp/README.md).

### Production profile

Generic config lives in `application-prod.yml`. Provide values via env (see `.env.example` and [docs/private-data.md](docs/private-data.md)):

```bash
export QSOURCE_DB_URL=jdbc:postgresql://…
export QSOURCE_DB_USER=…
export QSOURCE_DB_PASSWORD=…
export QSOURCE_JWT_SECRET='…'   # ≥32 chars
./gradlew bootRun --args='--spring.profiles.active=prod'
```

### Tests

```bash
cd services/api && ./gradlew test
cd services/mcp && source .venv/bin/activate && pytest
```

## Auth (short)

| Client | How |
|--------|-----|
| Web | Email/password → JWT (`Authorization: Bearer …`) |
| Agents | API key (`X-Api-Key: qs_…`), hashed at rest, shown once |
| Attribution | `X-Actor-Source: web \| api \| agent` on revisions |

## License

MIT — see [LICENSE](LICENSE).
