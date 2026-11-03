# Coord HTTP API (v1)

Base URL local: `http://localhost:8095`

## Actor headers (no auth yet)

| Header | Example | Notes |
|--------|---------|-------|
| `X-Actor-Name` | `sarah` | Who is acting |
| `X-Actor-Source` | `web` \| `api` \| `agent` | Defaults to `api` |

## Endpoints

- `GET /api/projects`
- `POST /api/projects` `{ "name", "description?" }`
- `GET /api/projects/{id}/board`
- `POST /api/projects/{id}/roadmap`
- `POST /api/projects/{id}/features`
- `POST /api/projects/{id}/tasks`
- `GET /api/features/{id}`
- `GET /api/features/{id}/revisions`
- `POST /api/features/{id}/revisions` (publish)
- `GET|POST /api/features/{id}/comments`
- `GET /api/tasks/{id}`
- `GET /api/tasks/{id}/revisions`
- `POST /api/tasks/{id}/revisions`
- `GET|POST /api/tasks/{id}/comments`
