"""Thin HTTP client for the QSource API."""

from __future__ import annotations

import os
from typing import Any

import httpx

DEFAULT_BASE = "http://localhost:8095"


class QSourceClient:
    def __init__(
        self,
        base_url: str | None = None,
        actor_name: str | None = None,
        actor_source: str | None = None,
    ) -> None:
        self.base_url = (base_url or os.getenv("QSOURCE_API_BASE_URL") or DEFAULT_BASE).rstrip("/")
        self.actor_name = actor_name or os.getenv("QSOURCE_ACTOR_NAME") or "mcp-agent"
        self.actor_source = (actor_source or os.getenv("QSOURCE_ACTOR_SOURCE") or "agent").lower()

    def _headers(self) -> dict[str, str]:
        return {
            "Content-Type": "application/json",
            "X-Actor-Name": self.actor_name,
            "X-Actor-Source": self.actor_source,
        }

    def _request(self, method: str, path: str, json: dict[str, Any] | None = None) -> Any:
        url = f"{self.base_url}{path}"
        with httpx.Client(timeout=30.0) as client:
            response = client.request(method, url, headers=self._headers(), json=json)
            response.raise_for_status()
            if response.status_code == 204 or not response.content:
                return None
            return response.json()

    def list_projects(self) -> Any:
        return self._request("GET", "/api/projects")

    def get_board(self, project_id: str) -> Any:
        return self._request("GET", f"/api/projects/{project_id}/board")

    def get_feature(self, feature_id: str) -> Any:
        return self._request("GET", f"/api/features/{feature_id}")

    def list_feature_revisions(self, feature_id: str) -> Any:
        return self._request("GET", f"/api/features/{feature_id}/revisions")

    def publish_feature_revision(
        self,
        feature_id: str,
        *,
        title: str,
        outcome: str,
        requirements: str,
        success_criteria: str,
        change_message: str,
    ) -> Any:
        return self._request(
            "POST",
            f"/api/features/{feature_id}/revisions",
            {
                "title": title,
                "outcome": outcome,
                "requirements": requirements,
                "successCriteria": success_criteria,
                "changeMessage": change_message,
            },
        )

    def publish_task_revision(
        self,
        task_id: str,
        *,
        title: str,
        description: str,
        status: str,
        change_message: str,
    ) -> Any:
        return self._request(
            "POST",
            f"/api/tasks/{task_id}/revisions",
            {
                "title": title,
                "description": description,
                "status": status,
                "changeMessage": change_message,
            },
        )

    def add_feature_comment(self, feature_id: str, body: str) -> Any:
        return self._request("POST", f"/api/features/{feature_id}/comments", {"body": body})
