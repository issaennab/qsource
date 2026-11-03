import json

import httpx
import respx

from qsource_mcp.client import QSourceClient

BASE = "http://localhost:8095"


@respx.mock
def test_create_feature_posts_camel_case_body_and_actor_headers():
    route = respx.post(f"{BASE}/api/projects/proj-1/features").mock(
        return_value=httpx.Response(
            200,
            json={"id": "feat-1", "title": "Agent bootstrap"},
        )
    )
    client = QSourceClient(
        base_url=BASE,
        api_key="qs_test_key",
        actor_source="agent",
    )

    result = client.create_feature(
        "proj-1",
        title="Agent bootstrap",
        outcome="Agents can create features",
        requirements="MCP create_feature tool",
        success_criteria="POST succeeds",
        change_message="Initial revision",
    )

    assert result["id"] == "feat-1"
    assert route.called
    request = route.calls.last.request
    assert request.headers["X-Api-Key"] == "qs_test_key"
    assert request.headers["X-Actor-Source"] == "agent"
    assert request.headers["Content-Type"] == "application/json"
    payload = json.loads(request.content.decode())
    assert payload == {
        "title": "Agent bootstrap",
        "outcome": "Agents can create features",
        "requirements": "MCP create_feature tool",
        "successCriteria": "POST succeeds",
        "changeMessage": "Initial revision",
    }


@respx.mock
def test_create_task_includes_feature_id_when_set():
    route = respx.post(f"{BASE}/api/projects/proj-1/tasks").mock(
        return_value=httpx.Response(
            200,
            json={"id": "task-1", "title": "Wire MCP", "status": "TODO"},
        )
    )
    client = QSourceClient(base_url=BASE, api_key="qs_test_key", actor_source="agent")

    result = client.create_task(
        "proj-1",
        title="Wire MCP",
        feature_id="feat-1",
        description="Add create tools",
        status="TODO",
        change_message="Initial revision",
    )

    assert result["id"] == "task-1"
    assert route.called
    request = route.calls.last.request
    payload = json.loads(request.content.decode())
    assert payload == {
        "title": "Wire MCP",
        "description": "Add create tools",
        "status": "TODO",
        "changeMessage": "Initial revision",
        "featureId": "feat-1",
    }
    assert request.headers["X-Api-Key"] == "qs_test_key"
    assert request.headers["X-Actor-Source"] == "agent"


@respx.mock
def test_create_task_omits_feature_id_when_unset():
    route = respx.post(f"{BASE}/api/projects/proj-1/tasks").mock(
        return_value=httpx.Response(200, json={"id": "task-2", "title": "Standalone"})
    )
    client = QSourceClient(base_url=BASE)

    client.create_task("proj-1", title="Standalone")

    payload = json.loads(route.calls.last.request.content.decode())
    assert "featureId" not in payload
    assert payload["title"] == "Standalone"
    assert payload["status"] == "TODO"
