# QSource roadmap

Work ahead aligned with `PLAN.md`. Prefer small use-case slices over big-bang features.

## Now — UC1 Ticket-first

- [x] SaaS multi-tenant backend (JWT, workspaces/orgs, groups, tags, API keys)
- [x] Ticket board as primary UX (TODO / IN_PROGRESS / DONE)
- [x] Drag-and-drop status moves (web)
- [x] Full-width responsive web shell
- [x] Workspace copy (personal + company same path)
- [x] Collapse Access & Tags / planning behind secondary UI

## Now — UC3 Multi-workspace + invites

- [x] Slack-like workspace switcher + create workspace
- [x] People: invite existing users by email (OWNER/ADMIN)
- [x] Public sample seed: QSource project (board / features / planning / roadmap from PLAN)
- [ ] Email invite for users who have not signed up yet
- [ ] Per-project edit roles later

## Next — UC2 Agent keys

- [ ] Settings: create / list / revoke API keys (show secret once)
- [ ] Optional key expiry (`expiresAt`, default 90d when enabled)
- [ ] MCP docs + Cursor wiring with `QSOURCE_API_KEY`

## Polish / infra

- [ ] Revision diff UI (side-by-side snapshots)
- [ ] Roadmap drag-and-drop sequencing
- [ ] Activity feed filters (human vs agent)
- [x] Postgres-ready production profile (`application-prod.yml` + env)
- [ ] Docker Compose for local Postgres
- [ ] Keep client/prod secrets in private env only (see docs/private-data.md)
- [ ] API OpenAPI export and Postman collection
- [ ] Hosted deploy (API + web)

## Later

- [ ] Fine-grained RBAC UI (who can edit / do)
- [ ] React Native + TestFlight
- [ ] Decision records linked to features
- [ ] Webhooks for revision published
- [ ] GitHub PR link attachments on revisions
- [ ] Slack / Discord notifications
- [ ] Import from Markdown / CSV
