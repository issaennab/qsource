import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function AuthBar() {
  const { user, orgs, orgId, setOrgId, logout, loading } = useAuth()

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

  return (
    <div className="actor-bar">
      {orgs.length > 0 && (
        <label>
          Company
          <select value={orgId ?? ''} onChange={(e) => setOrgId(e.target.value)}>
            {orgs.map((o) => (
              <option key={o.organizationId} value={o.organizationId}>
                {o.name}
              </option>
            ))}
          </select>
        </label>
      )}
      <span className="user-chip">{user.displayName}</span>
      <button className="btn secondary" type="button" onClick={() => void logout()}>
        Sign out
      </button>
    </div>
  )
}
