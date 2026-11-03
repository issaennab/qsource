# Coord

**Coord** is a lightweight product-planning and work-coordination platform for humans and AI coding agents.

Instead of silently overwriting tickets in Notion/Jira, Coord stores **immutable revisions** of features and tasks. Each revision records the full snapshot, who made the change (human or agent), where it came from (`web` / `api` / `agent`), what changed, and which revision it was based on. Comments stay exploratory until you publish a revision.

## Who it's for

Solo builders and small product/engineering teams who want a shared source of truth that agents can read and update without destroying history.

## Stack

| Layer | Tech |
|-------|------|
| API | Java 17, Spring Boot 3, JPA, H2 (local) / Postgres-ready |
| Web | React, TypeScript, Vite |
| Later | React Native + TestFlight, Python MCP, JWT auth |

> **Auth:** none in v1 (keeps testing simple). Pass `X-Actor-Name` and `X-Actor-Source` so every revision stays attributable. JWT/OAuth is on the roadmap.

## Monorepo layout

```
coord/
├── apps/web          # React web console
├── services/api      # Spring Boot API
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
# seeded project: "Coord itself"
```

### Web

```bash
cd apps/web
npm install
npm run dev
# http://localhost:5173  (proxies /api → :8095)
```

Use the top-bar **Acting as** / **Source** controls to publish as a human or agent.

### Tests

```bash
cd services/api
./gradlew test
```

## License

MIT — see [LICENSE](LICENSE).
