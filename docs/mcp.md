# QSource MCP

Agents talk to QSource through this MCP server.

**Auth:** set `QSOURCE_API_KEY` (preferred) or `QSOURCE_ACCESS_TOKEN`. Unsigned actor-name headers are not sufficient in SaaS mode.

**Tools include** list/read board, `create_feature` / `create_task`, publish revisions, and feature comments.

Install, Cursor wiring, smoke steps: [services/mcp/README.md](../services/mcp/README.md).
