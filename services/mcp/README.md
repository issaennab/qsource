# QSource MCP (Phase 2+)

Model Context Protocol server that wraps the QSource HTTP API so coding agents can read project context, create features/tasks, and publish attributed revisions.

## Auth (required in SaaS mode)

The API no longer trusts unsigned `X-Actor-Name` alone. Configure **one** of:

| Env | Purpose |
|-----|---------|
| `QSOURCE_API_KEY` | Org/project API key (`X-Api-Key`) — preferred for agents |
| `QSOURCE_ACCESS_TOKEN` | User JWT (`Authorization: Bearer …`) |

Create an API key after signing in (owner/admin):

`POST /api/orgs/{orgId}/api-keys` with `{ "name": "cursor-mcp", "projectId": "<optional>" }`.

## Tools

| Tool | Purpose |
|------|---------|
| `list_projects` | List visible projects |
| `get_project_board` | Roadmap, features, tasks, activity for a project |
| `get_feature` | Current accepted feature revision + content |
| `create_feature` | Create a feature (initial revision) under a project |
| `create_task` | Create a task under a project (optional `feature_id`) |
| `list_feature_revisions` | Immutable history for a feature |
| `publish_feature_revision` | Publish a new feature revision |
| `list_tasks` | Tasks on a project board (optional feature filter) |
| `publish_task_revision` | Publish a new task revision |
| `add_feature_comment` | Non-authoritative comment |

## Run (stdio — for Cursor / Claude Desktop)

```bash
cd services/mcp
python3 -m venv .venv
source .venv/bin/activate
pip install -e ".[dev]"
export QSOURCE_API_BASE_URL=http://localhost:8095
export QSOURCE_API_KEY=qs_your_key_here
export QSOURCE_ACTOR_SOURCE=agent
qsource-mcp
```

Requires the Java API running on port **8095**.

## Cursor MCP config

Add under Cursor MCP settings (`~/.cursor/mcp.json`, or project MCP settings). Use an **absolute** path:

```json
{
  "mcpServers": {
    "qsource": {
      "command": "/Users/issaennab/dev/work/workspace/qsource/services/mcp/.venv/bin/qsource-mcp",
      "env": {
        "QSOURCE_API_BASE_URL": "http://localhost:8095",
        "QSOURCE_API_KEY": "qs_your_key_here",
        "QSOURCE_ACTOR_SOURCE": "agent"
      }
    }
  }
}
```

### Smoke check

1. Start API; sign in on web as `demo@qsource.local` / `demo1234` (seeded).
2. Create an API key for the demo org.
3. Configure MCP with that key; call `list_projects`, then `create_feature`.

### Troubleshooting

| Symptom | Likely cause |
|---------|----------------|
| 401 Unauthorized | Missing/invalid `QSOURCE_API_KEY` or `QSOURCE_ACCESS_TOKEN` |
| Connection refused | API not on `:8095` |
| Empty project list | Key scoped to another org/project, or no membership |

## Tests

```bash
cd services/mcp
source .venv/bin/activate
pip install -e ".[dev]"
pytest
```
