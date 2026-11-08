# QSource architecture

Living overview. Product intent and build order live in [PLAN.md](../PLAN.md).

## Goals

- **Tickets first** — TODO / IN_PROGRESS / DONE as the daily loop
- **Workspaces** — personal and company (same `Organization` type); Slack-style switcher
- **Safe agent edits** — immutable revisions with actor + source; comments stay non-authoritative until published
- **Over-design / under-deliver** — roles, ACL, key expiry designed in; ship thin UI use case by use case

## Domain

```
UserAccount
  └── OrganizationMember (OWNER | ADMIN | MEMBER)
        └── Organization  (= workspace: QSource, …)
              ├── Project (visibility ORG | RESTRICTED)
              │     ├── Task (ticket) → Revision*
              │     ├── Feature → Revision*
              │     ├── RoadmapItem
              │     ├── ActivityEvent
              │     └── ProjectAccess (when RESTRICTED)
              ├── OrgGroup + members
              ├── Tag (+ feature/task taggings)
              └── ApiKey (hashed secret, optional project scope)
```

\* Features and tasks point at `currentRevisionId`. Each publish appends an immutable revision (JSON snapshot, change message, actor, source, parent, version).

## Clients

| Client | Role |
|--------|------|
| `apps/web` | Full-width desktop console: workspace switcher, projects, drag-and-drop kanban, people invite, settings |
| `services/mcp` | Stdio MCP tools over the HTTP API for coding agents |
| `apps/mobile` | Not started — React Native later; different UX from web |

## Auth

| Path | Mechanism |
|------|-----------|
| Human | Signup/login → JWT access + refresh; `Authorization: Bearer` |
| Agent | `X-Api-Key: qs_…` (preferred) or user JWT |
| Attribution | Authenticated principal + `X-Actor-Source` (`web` / `api` / `agent`) |

Unsigned `X-Actor-Name` alone is **not** accepted for mutating SaaS APIs.

API keys: random secret, **hash stored**, shown **once**, revoke = rotate by creating a new key. Optional TTL designed for later (default ~90 days when enabled).

## Access control (current)

- Org membership required for workspace data
- `OWNER` / `ADMIN` for invites, groups, API keys, restricted access admin
- Project `ORG` = all org members; `RESTRICTED` = `project_access` principals (user/group)
- Per-project fine-grained “who can edit” roles: designed for later, not fully UI’d

## Persistence

- Local: H2 file DB (`services/api/data/qsource`)
- `spring.jpa.hibernate.ddl-auto=update`
- Postgres-ready later via env (`QSOURCE_DB_URL`, etc.)

## Package map (API)

- `com.qsource.api.web` — controllers
- `com.qsource.api.service` — auth, orgs, projects, tags, keys, revisions
- `com.qsource.api.domain` — JPA entities
- `com.qsource.api.security` — JWT filter, API key filter, ACL helpers
- `com.qsource.api.seed` — dogfood users/workspaces when `qsource.seed=true`
