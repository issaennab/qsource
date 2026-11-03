from qsource_mcp.client import QSourceClient


def test_headers_default_agent():
    client = QSourceClient(actor_name="unit", actor_source="agent")
    headers = client._headers()
    assert headers["X-Actor-Name"] == "unit"
    assert headers["X-Actor-Source"] == "agent"
