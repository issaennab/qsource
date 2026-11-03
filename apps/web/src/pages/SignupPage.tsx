import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { api } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export function SignupPage() {
  const navigate = useNavigate()
  const { refresh } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [displayName, setDisplayName] = useState('')
  const [organizationName, setOrganizationName] = useState('')
  const [error, setError] = useState('')
  const [busy, setBusy] = useState(false)

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault()
    setBusy(true)
    setError('')
    try {
      await api.signup({
        email: email.trim(),
        password,
        displayName: displayName.trim(),
        organizationName: organizationName.trim(),
      })
      await refresh()
      navigate('/')
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Signup failed')
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="stack loose" style={{ maxWidth: 480, margin: '1rem auto' }}>
      <div className="page-header">
        <h1>Create account</h1>
        <p className="meta">Sign up creates your company workspace and makes you the owner.</p>
      </div>
      {error && <p className="error">{error}</p>}
      <form className="panel stack tight" onSubmit={onSubmit}>
        <label>
          Your name
          <input value={displayName} onChange={(e) => setDisplayName(e.target.value)} required />
        </label>
        <label>
          Work email
          <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
        </label>
        <label>
          Password
          <input
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            type="password"
            minLength={8}
            required
          />
        </label>
        <label>
          Company name
          <input value={organizationName} onChange={(e) => setOrganizationName(e.target.value)} required />
        </label>
        <button className="btn block" type="submit" disabled={busy}>
          {busy ? 'Creating…' : 'Create company'}
        </button>
      </form>
      <p className="meta">
        Already have an account? <Link to="/login">Sign in</Link>
      </p>
    </div>
  )
}
