import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, type Project } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export function HomePage() {
  const { orgId, orgs } = useAuth()
  const [projects, setProjects] = useState<Project[]>([])
  const [name, setName] = useState('')
  const [visibility, setVisibility] = useState<'ORG' | 'RESTRICTED'>('ORG')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  const orgName = orgs.find((o) => o.organizationId === orgId)?.name

  async function load() {
    if (!orgId) {
      setProjects([])
      setLoading(false)
      return
    }
    setLoading(true)
    setError('')
    try {
      setProjects(await api.listOrgProjects(orgId))
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [orgId])

  async function createProject(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim() || !orgId) return
    try {
      const project = await api.createOrgProject(orgId, {
        name: name.trim(),
        description: '',
        visibility,
      })
      setName('')
      await load()
      window.location.href = `/projects/${project.id}`
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed')
    }
  }

  return (
    <div className="stack loose">
      <div className="page-header">
        <h1>Projects</h1>
        <p className="meta">
          {orgName ? `${orgName} — ` : ''}
          Versioned roadmaps, features, and tasks — shared by humans and agents.
        </p>
      </div>
      {error && <p className="error">{error}</p>}
      <form className="panel stack tight" onSubmit={createProject}>
        <div className="toolbar">
          <label className="grow">
            New project
            <input value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. QSource itself" />
          </label>
          <label>
            Visibility
            <select value={visibility} onChange={(e) => setVisibility(e.target.value as 'ORG' | 'RESTRICTED')}>
              <option value="ORG">Entire company</option>
              <option value="RESTRICTED">Restricted (groups/members)</option>
            </select>
          </label>
          <button className="btn" type="submit">
            Create
          </button>
        </div>
      </form>
      <div className="list">
        {loading && <p className="meta">Loading…</p>}
        {!loading && projects.length === 0 && <p className="meta">No projects yet.</p>}
        {projects.map((p) => (
          <Link key={p.id} className="item" to={`/projects/${p.id}`}>
            <strong>{p.name}</strong>
            <span className="meta">
              {p.description || 'No description'} · <span className="badge">{p.visibility}</span>
            </span>
          </Link>
        ))}
      </div>
    </div>
  )
}
