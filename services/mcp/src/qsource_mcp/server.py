"""QSource MCP server — stdio entrypoint for agents."""

from __future__ import annotations

import json
from typing import Any

from mcp.server.mcpserver import MCPServer

from qsource_mcp.client import QSourceClient

mcp = MCPServer("qsource")
client = QSourceClient()


def _dump(data: Any) -> str:
    return json.dumps(data, indent=2, default=str)


@mcp.tool()
def list_projects() -> str:
    """List all QSource projects."""
    return _dump(client.list_projects())


@mcp.tool()
def get_project_board(project_id: str) -> str:
    """Get roadmap, features, tasks, and recent activity for a project."""
    return _dump(client.get_board(project_id))


@mcp.tool()
def get_feature(feature_id: str) -> str:
    """Get a feature's current accepted revision content."""
    return _dump(client.get_feature(feature_id))


@mcp.tool()
def create_feature(
    project_id: str,
    title: str,
    outcome: str = "",
    requirements: str = "",
    success_criteria: str = "",
    change_message: str = "Initial revision",
) -> str:
    """Create a feature under a project with an initial revision."""
    return _dump(
        client.create_feature(
            project_id,
            title=title,
            outcome=outcome,
            requirements=requirements,
            success_criteria=success_criteria,
            change_message=change_message,
        )
    )


@mcp.tool()
def create_task(
    project_id: str,
    title: str,
    feature_id: str | None = None,
    description: str = "",
    status: str = "TODO",
    change_message: str = "Initial revision",
) -> str:
    """Create a task under a project (optionally linked to a feature). status: TODO | IN_PROGRESS | DONE | CANCELLED."""
    return _dump(
        client.create_task(
            project_id,
            title=title,
            feature_id=feature_id,
            description=description,
            status=status,
            change_message=change_message,
        )
    )


@mcp.tool()
def list_feature_revisions(feature_id: str) -> str:
    """List immutable revision history for a feature (newest first)."""
    return _dump(client.list_feature_revisions(feature_id))


@mcp.tool()
def publish_feature_revision(
    feature_id: str,
    title: str,
    change_message: str,
    outcome: str = "",
    requirements: str = "",
    success_criteria: str = "",
) -> str:
    """Publish a new immutable feature revision. Always include a clear change_message."""
    return _dump(
        client.publish_feature_revision(
            feature_id,
            title=title,
            outcome=outcome,
            requirements=requirements,
            success_criteria=success_criteria,
            change_message=change_message,
        )
    )


@mcp.tool()
def list_tasks(project_id: str, feature_id: str | None = None) -> str:
    """List tasks for a project; optionally filter by feature_id."""
    board = client.get_board(project_id)
    tasks = board.get("tasks") or []
    if feature_id:
        tasks = [t for t in tasks if t.get("featureId") == feature_id]
    return _dump(tasks)


@mcp.tool()
def publish_task_revision(
    task_id: str,
    title: str,
    status: str,
    change_message: str,
    description: str = "",
) -> str:
    """Publish a new immutable task revision. status: TODO | IN_PROGRESS | DONE | CANCELLED."""
    return _dump(
        client.publish_task_revision(
            task_id,
            title=title,
            description=description,
            status=status,
            change_message=change_message,
        )
    )


@mcp.tool()
def add_feature_comment(feature_id: str, body: str) -> str:
    """Add a non-authoritative comment on a feature (does not create a revision)."""
    return _dump(client.add_feature_comment(feature_id, body))


def main() -> None:
    mcp.run(transport="stdio")


if __name__ == "__main__":
    main()
