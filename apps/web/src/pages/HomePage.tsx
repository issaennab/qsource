import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, type Project } from '../api/client'

export function HomePage() {
  const [projects, setProjects] = useState<Project[]>([])
  const [name, setName] = useState('')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(true)

  async function load() {
    setLoading(true)
    setError('')
    try {
      setProjects(await api.listProjects())
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    void load()
  }, [])

  async function createProject(e: React.FormEvent) {
    e.preventDefault()
    if (!name.trim()) return
    try {
      const project = await api.createProject({ name: name.trim(), description: '' })
      setName('')
      await load()
      window.location.href = `/projects/${project.id}`
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed')
    }
  }

  return (
    <div className="stack">
      <h1 style={{ fontFamily: 'Fraunces, Georgia, serif', margin: 0 }}>Projects</h1>
      <p className="meta">Versioned roadmaps, features, and tasks — shared by humans and agents.</p>
      {error && <p className="error">{error}</p>}
      <form className="panel stack" onSubmit={createProject}>
        <label>
          New project
          <input value={name} onChange={(e) => setName(e.target.value)} placeholder="e.g. Coord itself" />
        </label>
        <button className="btn" type="submit">Create</button>
      </form>
      <div className="list">
        {loading && <p className="meta">Loading…</p>}
        {!loading && projects.length === 0 && <p className="meta">No projects yet.</p>}
        {projects.map((p) => (
          <Link key={p.id} className="item" to={`/projects/${p.id}`}>
            <strong>{p.name}</strong>
            <span className="meta">{p.description || 'No description'}</span>
          </Link>
        ))}
      </div>
    </div>
  )
}
