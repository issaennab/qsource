# QSource HTTP API

Base URL (local): `http://localhost:8095`

Prefer controllers under `com.qsource.api.web` if this file lags. Auth details also in [architecture.md](architecture.md).

## Auth

| Method | Path | Notes |
|--------|------|-------|
| POST | `/api/auth/signup` | `{ email, password, displayName, organizationName }` → tokens + first workspace |
| POST | `/api/auth/login` | `{ email, password }` → tokens |
| POST | `/api/auth/refresh` | `{ refreshToken }` → tokens |
| POST | `/api/auth/logout` | `{ refreshToken }` |
| GET | `/api/me` | Current user + workspace memberships |

**Human requests:** `Authorization: Bearer <accessToken>`  
**Agent requests:** `X-Api-Key: qs_…`  
**Attribution:** `X-Actor-Source: web | api | agent` (web client sets `web`)

## Workspaces (orgs)

| Method | Path |
|--------|------|
| GET | `/api/orgs` |
| POST | `/api/orgs` | `{ name }` — create workspace; caller becomes OWNER |
| GET | `/api/orgs/{orgId}` |
| GET/POST | `/api/orgs/{orgId}/members` | Invite: `{ email, role: ADMIN \| MEMBER }` (user must already exist) |
| PATCH/DELETE | `/api/orgs/{orgId}/members/{userId}` |
| GET/POST | `/api/orgs/{orgId}/projects` |
| GET/POST | `/api/orgs/{orgId}/groups` |
| GET/POST | `/api/orgs/{orgId}/tags` |
| GET/POST | `/api/orgs/{orgId}/api-keys` | Create returns raw key **once** |
| DELETE | `/api/orgs/{orgId}/api-keys/{keyId}` | Revoke |

## Projects & board

| Method | Path |
|--------|------|
| GET | `/api/projects` | Projects visible to caller |
| GET | `/api/projects/{id}/board` | Optional `?tagId=` |
| PATCH | `/api/projects/{id}` | name, description, visibility |
| GET/PUT | `/api/projects/{id}/access` | Restricted principals |
| POST | `/api/projects/{id}/roadmap` |
| POST | `/api/projects/{id}/features` |
| POST | `/api/projects/{id}/tasks` | Tickets: `{ title, status?, … }` |

## Features & tasks (revisions)

| Method | Path |
|--------|------|
| GET | `/api/features/{id}` |
| GET/POST | `/api/features/{id}/revisions` | Publish requires `changeMessage` |
| GET/POST | `/api/features/{id}/comments` |
| PUT | `/api/features/{id}/tags` |
| GET | `/api/tasks/{id}` |
| GET/POST | `/api/tasks/{id}/revisions` | Status moves publish a revision |
| GET/POST | `/api/tasks/{id}/comments` |
| PUT | `/api/tasks/{id}/tags` |

## Groups & tags (extras)

| Method | Path |
|--------|------|
| GET/DELETE | `/api/groups/{groupId}` |
| POST/DELETE | `/api/groups/{groupId}/members…` |
| PATCH/DELETE | `/api/tags/{tagId}` |

## Health

- `GET /actuator/health`
