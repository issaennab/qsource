# Private data vs public repo

This repository is **public**. Treat everything committed here as shareable.

## What belongs in the public repo

| OK to commit | Notes |
|--------------|--------|
| Sample seed (`DemoDataLoader`) | QSource product backlog only (`issa@qsource.io`; PLAN/ROADMAP-derived) |
| `application.yml` | Local H2 defaults |
| `application-prod.yml` | Env **placeholders** only |
| `.env.example` | Variable names, no secrets |
| Docs / PLAN / ROADMAP | No real client backlogs or credentials |

## What stays private (local or a private repo)

| Keep out of git | Examples |
|-----------------|----------|
| Real client/startup workspaces & tickets | Studio/product backlogs, customer names |
| Secrets | JWT signing keys, API keys, DB passwords |
| Cloud wiring | AWS account IDs, connection strings, deploy tokens |
| Local overrides | `application-local.yml`, `.env`, `secrets/` |

## Suggested layout

```
# On your machine / private clone — not committed
.env                          # real values
services/api/src/main/resources/application-local.yml
secrets/                      # optional; gitignored
```

Copy from examples:

```bash
cp .env.example .env
cp services/api/src/main/resources/application-local.yml.example \
   services/api/src/main/resources/application-local.yml
```

Run production-shaped config with env vars only:

```bash
export QSOURCE_DB_URL=jdbc:postgresql://…
export QSOURCE_DB_USER=…
export QSOURCE_DB_PASSWORD=…
export QSOURCE_JWT_SECRET='long-random-string-at-least-32-chars'
cd services/api && ./gradlew bootRun --args='--spring.profiles.active=prod'
```

## Resetting the public sample DB

Local H2 lives under `services/api/data/` (gitignored). To re-seed the public sample:

1. Stop the API
2. Delete `services/api/data/`
3. Start with `qsource.seed=true` (default in `application.yml`)
