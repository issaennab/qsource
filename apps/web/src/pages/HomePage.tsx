import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { api, type Project } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { initials } from '../lib/ticketMeta'

type Member = { userId: string; email: string; displayName: string; role: string }

export function HomePage() {
  const { orgId, orgs, user, refresh } = useAuth()
  const [projects, setProjects] = useState<Project[]>([])
  const [members, setMembers] = useState<Member[]>([])
  const [name, setName] = useState('')
  const [inviteEmail, setInviteEmail] = useState('')
  const [inviteRole, setInviteRole] = useState<'ADMIN' | 'MEMBER'>('MEMBER')
  const [error, setError] = useState('')
  const [inviteError, setInviteError] = useState('')
  const [inviteOk, setInviteOk] = useState('')
  const [loading, setLoading] = useState(true)

  const membership = orgs.find((o) => o.organizationId === orgId)
  const orgName = membership?.name
  const canInvite = membership?.role === 'OWNER' || membership?.role === 'ADMIN'

  async function load() {
    if (!orgId) {
      setProjects([])
      setMembers([])
      setLoading(false)
      return
    }
    setLoading(true)
    setError('')
    try {
      const [nextProjects, nextMembers] = await Promise.all([
        api.listOrgProjects(orgId),
        api.listOrgMembers(orgId),
      ])
      setProjects(nextProjects)
      setMembers(nextMembers)
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
        visibility: 'ORG',
      })
      setName('')
      window.location.href = `/projects/${project.id}`
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Create failed')
    }
  }

  async function invite(e: React.FormEvent) {
    e.preventDefault()
    if (!orgId || !inviteEmail.trim()) return
    setInviteError('')
    setInviteOk('')
    try {
      await api.addOrgMember(orgId, { email: inviteEmail.trim(), role: inviteRole })
      setInviteEmail('')
      setInviteOk('Added to workspace.')
      await load()
      await refresh()
    } catch (err) {
      setInviteError(err instanceof Error ? err.message : 'Invite failed')
    }
  }

  async function removeMember(userId: string) {
    if (!orgId || userId === user?.id) return
    try {
      await api.removeOrgMember(orgId, userId)
      await load()
    } catch (err) {
      setInviteError(err instanceof Error ? err.message : 'Remove failed')
    }
  }

  return (
    <div className="home-page">
      <header className="project-header home-header">
        <div className="project-title-block">
          <div className="project-icon" aria-hidden>
            <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
              <path d="M4 7h16M4 12h16M4 17h10" />
            </svg>
          </div>
          <div>
            <div className="project-title-row">
              <h1>{orgName ?? 'Projects'}</h1>
            </div>
            <p className="project-sub">Projects and people in this workspace. Switch workspaces from the sidebar.</p>
          </div>
        </div>
      </header>

      {error && <p className="error pad">{error}</p>}

      <div className="home-grid">
        <section className="panel-inset stack">
          <div className="section-head">
            <h2>Projects</h2>
          </div>
          <form className="composer" onSubmit={createProject}>
            <input
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="New project — e.g. Life, QSource"
            />
            <button className="btn primary" type="submit">
              Create
            </button>
          </form>
          <div className="card-list">
            {loading && <p className="muted">Loading…</p>}
            {!loading && projects.length === 0 && <p className="muted">No projects yet.</p>}
            {projects.map((p) => (
              <Link key={p.id} className="soft-card project-link" to={`/projects/${p.id}`}>
                <strong>{p.name}</strong>
                <span className="muted">{p.description || 'Open board'}</span>
              </Link>
            ))}
          </div>
        </section>

        <section className="panel-inset stack">
          <div className="section-head">
            <h2>People</h2>
          </div>
          {canInvite ? (
            <form className="composer wrap" onSubmit={invite}>
              <input
                value={inviteEmail}
                onChange={(e) => setInviteEmail(e.target.value)}
                type="email"
                placeholder="teammate@example.com"
              />
              <select value={inviteRole} onChange={(e) => setInviteRole(e.target.value as 'ADMIN' | 'MEMBER')}>
                <option value="MEMBER">Member</option>
                <option value="ADMIN">Admin</option>
              </select>
              <button className="btn primary" type="submit">
                Invite
              </button>
            </form>
          ) : (
            <p className="muted">Only owners and admins can invite.</p>
          )}
          {inviteError && <p className="error">{inviteError}</p>}
          {inviteOk && <p className="muted">{inviteOk}</p>}
          <div className="card-list">
            {members.map((m) => (
              <div key={m.userId} className="soft-card member-card">
                <span className="avatar">{initials(m.displayName)}</span>
                <div>
                  <strong>{m.displayName}</strong>
                  <span className="muted">
                    {m.email} · {m.role}
                  </span>
                </div>
                {canInvite && m.role !== 'OWNER' && m.userId !== user?.id && (
                  <button className="btn ghost" type="button" onClick={() => void removeMember(m.userId)}>
                    Remove
                  </button>
                )}
              </div>
            ))}
          </div>
        </section>
      </div>
    </div>
  )
}
