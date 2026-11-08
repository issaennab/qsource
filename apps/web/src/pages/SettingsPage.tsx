import { useEffect, useState } from 'react'
import { api, type ApiKeyCreated, type ApiKeyInfo, type Project } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatDay } from '../lib/ticketMeta'

type SettingsTab = 'api-keys' | 'people' | 'general'

export function SettingsPage() {
  const { orgId, orgs, user } = useAuth()
  const [tab, setTab] = useState<SettingsTab>('api-keys')
  const [keys, setKeys] = useState<ApiKeyInfo[]>([])
  const [projects, setProjects] = useState<Project[]>([])
  const [error, setError] = useState('')
  const [showCreate, setShowCreate] = useState(false)
  const [name, setName] = useState('')
  const [projectId, setProjectId] = useState('')
  const [created, setCreated] = useState<ApiKeyCreated | null>(null)
  const [busy, setBusy] = useState(false)

  const org = orgs.find((o) => o.organizationId === orgId)
  const canManage = org?.role === 'OWNER' || org?.role === 'ADMIN'

  async function load() {
    if (!orgId) return
    try {
      const [k, p] = await Promise.all([api.listApiKeys(orgId), api.listOrgProjects(orgId)])
      setKeys(k)
      setProjects(p)
      setError('')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load settings')
    }
  }

  useEffect(() => {
    void load()
  }, [orgId])

  async function createKey(e: React.FormEvent) {
    e.preventDefault()
    if (!orgId || !name.trim()) return
    setBusy(true)
    try {
      const key = await api.createApiKey(orgId, {
        name: name.trim(),
        projectId: projectId || undefined,
      })
      setCreated(key)
      setName('')
      setProjectId('')
      setShowCreate(false)
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed')
    } finally {
      setBusy(false)
    }
  }

  async function revoke(id: string) {
    if (!orgId || !window.confirm('Revoke this API key? Agents using it will lose access.')) return
    await api.revokeApiKey(orgId, id)
    await load()
  }

  return (
    <div className="settings-page">
      <aside className="settings-nav">
        <h2>Settings</h2>
        {(
          [
            ['general', 'General'],
            ['people', 'People'],
            ['api-keys', 'API keys'],
          ] as const
        ).map(([id, label]) => (
          <button
            key={id}
            type="button"
            className={`settings-link${tab === id ? ' active' : ''}`}
            onClick={() => setTab(id)}
          >
            {label}
          </button>
        ))}
      </aside>

      <div className="settings-main">
        {tab === 'general' && (
          <section className="stack">
            <h1>General</h1>
            <p className="muted">Workspace: {org?.name}. Signed in as {user?.displayName}.</p>
            <div className="panel-inset">
              <p>
                <strong>Role:</strong> {org?.role}
              </p>
            </div>
          </section>
        )}

        {tab === 'people' && (
          <section className="stack">
            <h1>People</h1>
            <p className="muted">Manage members from the workspace home for now.</p>
            <a className="btn secondary" href="/">
              Open workspace home
            </a>
          </section>
        )}

        {tab === 'api-keys' && (
          <div className="api-keys-layout">
            <div className="stack">
              <div className="settings-title-row">
                <div>
                  <h1>Agent API keys</h1>
                  <p className="muted">Give agents scoped access to QSource through MCP or the API.</p>
                </div>
                {canManage && (
                  <button className="btn primary" type="button" onClick={() => setShowCreate(true)}>
                    + Create API key
                  </button>
                )}
              </div>

              <div className="info-banner success">
                Secrets are shown once and stored as a hash.
              </div>

              {error && <p className="error">{error}</p>}

              {created && (
                <div className="panel-inset stack secret-box">
                  <strong>Copy your key now — it won’t be shown again.</strong>
                  <code className="secret-value">{created.apiKey}</code>
                  <button
                    className="btn secondary"
                    type="button"
                    onClick={() => void navigator.clipboard.writeText(created.apiKey)}
                  >
                    Copy
                  </button>
                  <button className="btn ghost" type="button" onClick={() => setCreated(null)}>
                    Dismiss
                  </button>
                </div>
              )}

              <div className="panel-inset table-wrap">
                <table className="data-table">
                  <thead>
                    <tr>
                      <th>Name</th>
                      <th>Scope</th>
                      <th>Prefix</th>
                      <th>Created</th>
                      <th>Status</th>
                      <th />
                    </tr>
                  </thead>
                  <tbody>
                    {keys.map((k) => {
                      const scope = projects.find((p) => p.id === k.projectId)?.name ?? 'Workspace'
                      return (
                        <tr key={k.id}>
                          <td>{k.name}</td>
                          <td>{scope}</td>
                          <td>
                            <code>{k.keyPrefix}…</code>
                          </td>
                          <td>{formatDay(k.createdAt)}</td>
                          <td>
                            <span className="status-dot active">Active</span>
                          </td>
                          <td>
                            {canManage && (
                              <button className="btn ghost" type="button" onClick={() => void revoke(k.id)}>
                                Revoke
                              </button>
                            )}
                          </td>
                        </tr>
                      )
                    })}
                    {keys.length === 0 && (
                      <tr>
                        <td colSpan={6} className="muted">
                          No API keys yet.
                        </td>
                      </tr>
                    )}
                  </tbody>
                </table>
              </div>
            </div>

            <aside className="stack">
              <section className="rail-card">
                <h3>Connect with MCP</h3>
                <p className="muted">Use the key in your Cursor MCP env:</p>
                <pre className="code-block">{`QSOURCE_API_BASE_URL=http://localhost:8095
QSOURCE_API_KEY=qs_…`}</pre>
              </section>
              <section className="rail-card">
                <h3>Recommended defaults</h3>
                <ul className="check-list">
                  <li>Project-scoped when possible</li>
                  <li>Revoke and replace to rotate</li>
                  <li>Never commit keys to git</li>
                </ul>
              </section>
            </aside>
          </div>
        )}
      </div>

      {showCreate && (
        <div className="modal-backdrop" onClick={() => setShowCreate(false)}>
          <form
            className="modal stack"
            onClick={(e) => e.stopPropagation()}
            onSubmit={createKey}
          >
            <h2>Create API key</h2>
            <label>
              Name
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                placeholder="e.g. Cursor — QSource"
                required
              />
            </label>
            <label>
              Project scope
              <select value={projectId} onChange={(e) => setProjectId(e.target.value)}>
                <option value="">Entire workspace</option>
                {projects.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.name}
                  </option>
                ))}
              </select>
            </label>
            <p className="muted">Expiry UI coming later — revoke anytime to rotate.</p>
            <div className="modal-actions">
              <button className="btn ghost" type="button" onClick={() => setShowCreate(false)}>
                Cancel
              </button>
              <button className="btn primary" type="submit" disabled={busy}>
                {busy ? 'Creating…' : 'Create key'}
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  )
}
