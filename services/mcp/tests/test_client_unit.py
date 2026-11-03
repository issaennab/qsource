from qsource_mcp.client import QSourceClient


def test_headers_default_agent_without_api_key():
    client = QSourceClient(actor_name="unit", actor_source="agent")
    headers = client._headers()
    assert headers["X-Actor-Name"] == "unit"
    assert headers["X-Actor-Source"] == "agent"
    assert "X-Api-Key" not in headers
    assert "Authorization" not in headers


def test_headers_prefer_api_key():
    client = QSourceClient(api_key="qs_test", actor_source="agent")
    headers = client._headers()
    assert headers["X-Api-Key"] == "qs_test"
    assert headers["X-Actor-Source"] == "agent"
    assert "Authorization" not in headers
    assert "X-Actor-Name" not in headers


def test_headers_bearer_token():
    client = QSourceClient(access_token="jwt-token", actor_source="agent")
    headers = client._headers()
    assert headers["Authorization"] == "Bearer jwt-token"
    assert "X-Api-Key" not in headers
