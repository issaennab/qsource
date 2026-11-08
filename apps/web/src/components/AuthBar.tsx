import { useEffect, useRef, useState } from 'react'
import { Link } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export function AuthBar() {
  const { user, orgs, orgId, setOrgId, logout, loading, refresh } = useAuth()
  const [open, setOpen] = useState(false)
  const [creating, setCreating] = useState(false)
  const [newName, setNewName] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)
  const menuRef = useRef<HTMLDivElement>(null)
  const busyRef = useRef(false)

  const current = orgs.find((o) => o.organizationId === orgId)

  useEffect(() => {
    function onDocClick(e: MouseEvent) {
      if (busyRef.current) return
      if (!menuRef.current?.contains(e.target as Node)) {
        setOpen(false)
        setCreating(false)
        setError('')
      }
    }
    document.addEventListener('mousedown', onDocClick)
    return () => document.removeEventListener('mousedown', onDocClick)
  }, [])

  if (loading) {
    return <div className="actor-bar meta">Loading…</div>
  }

  if (!user) {
    return (
      <div className="actor-bar">
        <Link className="btn secondary" to="/login">
          Sign in
        </Link>
        <Link className="btn" to="/signup">
          Sign up
        </Link>
      </div>
    )
  }

  async function createWorkspace(e: React.FormEvent) {
    e.preventDefault()
    if (!newName.trim() || busyRef.current) return
    busyRef.current = true
    setBusy(true)
    setError('')
    try {
      const org = await api.createOrg({ name: newName.trim() })
      await refresh({ selectOrgId: org.id })
      setNewName('')
      setCreating(false)
      setOpen(false)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Could not create workspace')
    } finally {
      busyRef.current = false
      setBusy(false)
    }
  }

  return (
    <div className="actor-bar">
      <div className="ws-switcher" ref={menuRef}>
        <button
          className="btn secondary ws-trigger"
          type="button"
          aria-expanded={open}
          onClick={() => setOpen((v) => !v)}
        >
          <span className="ws-trigger-label">{current?.name ?? 'Select workspace'}</span>
          <span className="meta">▾</span>
        </button>
        {open && (
          <div className="ws-menu" role="menu">
            <p className="ws-menu-title">Workspaces</p>
            <p className="meta ws-menu-hint">Switch between personal and company spaces.</p>
            {orgs.map((o) => (
              <button
                key={o.organizationId}
                type="button"
                role="menuitem"
                className={`ws-option${o.organizationId === orgId ? ' active' : ''}`}
                onClick={() => {
                  setOrgId(o.organizationId)
                  setOpen(false)
                }}
              >
                <strong>{o.name}</strong>
                <span className="meta">{o.role.toLowerCase()}</span>
              </button>
            ))}
            <div className="ws-menu-divider" />
            {!creating ? (
              <button className="ws-option" type="button" onClick={() => setCreating(true)}>
                <strong>+ Create workspace</strong>
                <span className="meta">Personal, team, product…</span>
              </button>
            ) : (
              <form className="ws-create" onSubmit={(e) => void createWorkspace(e)}>
                <input
                  autoFocus
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  placeholder="e.g. Issa, Acme Studio, QSource"
                  disabled={busy}
                />
                {error && <p className="error">{error}</p>}
                <div className="control-row">
                  <button
                    className="btn secondary btn-sm"
                    type="button"
                    disabled={busy}
                    onClick={() => setCreating(false)}
                  >
                    Cancel
                  </button>
                  <button className="btn btn-sm" type="submit" disabled={busy}>
                    {busy ? 'Creating…' : 'Create'}
                  </button>
                </div>
              </form>
            )}
          </div>
        )}
      </div>
      <span className="user-chip">{user.displayName}</span>
      <button className="btn secondary" type="button" onClick={() => void logout()}>
        Sign out
      </button>
    </div>
  )
}
