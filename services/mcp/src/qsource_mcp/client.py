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
        api_key: str | None = None,
        access_token: str | None = None,
    ) -> None:
        self.base_url = (base_url or os.getenv("QSOURCE_API_BASE_URL") or DEFAULT_BASE).rstrip("/")
        self.actor_name = actor_name or os.getenv("QSOURCE_ACTOR_NAME") or "mcp-agent"
        self.actor_source = (actor_source or os.getenv("QSOURCE_ACTOR_SOURCE") or "agent").lower()
        self.api_key = api_key or os.getenv("QSOURCE_API_KEY")
        self.access_token = access_token or os.getenv("QSOURCE_ACCESS_TOKEN")

    def _headers(self) -> dict[str, str]:
        headers = {
            "Content-Type": "application/json",
            "X-Actor-Source": self.actor_source,
        }
        if self.api_key:
            headers["X-Api-Key"] = self.api_key
        elif self.access_token:
            headers["Authorization"] = f"Bearer {self.access_token}"
        else:
            # Legacy unsigned actor headers are no longer enough for SaaS mode.
            headers["X-Actor-Name"] = self.actor_name
        return headers

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

    def create_feature(
        self,
        project_id: str,
        *,
        title: str,
        outcome: str = "",
        requirements: str = "",
        success_criteria: str = "",
        change_message: str = "",
    ) -> Any:
        return self._request(
            "POST",
            f"/api/projects/{project_id}/features",
            {
                "title": title,
                "outcome": outcome,
                "requirements": requirements,
                "successCriteria": success_criteria,
                "changeMessage": change_message,
            },
        )

    def create_task(
        self,
        project_id: str,
        *,
        title: str,
        feature_id: str | None = None,
        description: str = "",
        status: str = "TODO",
        estimated_delivery_date: str | None = None,
        change_message: str = "",
    ) -> Any:
        body: dict[str, Any] = {
            "title": title,
            "description": description,
            "status": status,
            "changeMessage": change_message,
        }
        if feature_id is not None:
            body["featureId"] = feature_id
        if estimated_delivery_date is not None:
            body["estimatedDeliveryDate"] = estimated_delivery_date
        return self._request("POST", f"/api/projects/{project_id}/tasks", body)

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

    def create_api_key(self, org_id: str, *, name: str, project_id: str | None = None) -> Any:
        body: dict[str, Any] = {"name": name}
        if project_id is not None:
            body["projectId"] = project_id
        return self._request("POST", f"/api/orgs/{org_id}/api-keys", body)
