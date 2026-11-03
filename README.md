# QSource

**QSource** is a lightweight product-planning and work-coordination platform for humans and AI coding agents.

Instead of silently overwriting tickets in Notion/Jira, QSource stores **immutable revisions** of features and tasks. Each revision records the full snapshot, who made the change (human or agent), where it came from (`web` / `api` / `agent`), what changed, and which revision it was based on. Comments stay exploratory until you publish a revision.

## Who it's for

Solo builders and small product/engineering teams who want a shared source of truth that agents can read and update without destroying history.

## Stack

| Layer | Tech |
|-------|------|
| API | Java 17, Spring Boot 3, JPA, H2 (local) / Postgres-ready |
| Web | React, TypeScript, Vite |
| MCP | Python MCP wrapping the HTTP API |
| Later | React Native + TestFlight, JWT auth |

> **Auth:** email/password + JWT. Multi-company orgs, groups, restricted projects, and org tags. Agents authenticate with `X-Api-Key` (or a user JWT). Revisions stay attributable via authenticated actor + `X-Actor-Source`.

## Monorepo layout

```
qsource/
├── apps/web          # React web console
├── services/api      # Spring Boot API
├── services/mcp      # Python MCP for agents
├── docs/             # Architecture + API notes
├── LICENSE           # MIT
└── ROADMAP.md        # Planned work (≥20h)
```

## Quick start

### API (port 8095)

```bash
cd services/api
./gradlew bootRun
# health: http://localhost:8095/actuator/health
# seeded project: "QSource itself"
```

### Web

```bash
cd apps/web
npm install
npm run dev
# http://localhost:5173  (proxies /api → :8095)
```

Seeded login (when `qsource.seed=true`): **demo@qsource.local** / **demo1234**.

### MCP (agents)

```bash
cd services/mcp
python3 -m venv .venv && source .venv/bin/activate
pip install -e .
export QSOURCE_API_KEY=qs_your_key_here   # create via POST /api/orgs/{orgId}/api-keys
qsource-mcp   # stdio; see services/mcp/README.md for Cursor config
```

### Tests

```bash
cd services/api && ./gradlew test
cd services/mcp && source .venv/bin/activate && pytest
```

## License

MIT — see [LICENSE](LICENSE).
