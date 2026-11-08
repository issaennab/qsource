import { useEffect, useState } from 'react'
import { Link, NavLink, useLocation, useNavigate, useParams } from 'react-router-dom'
import { api, type Project } from '../api/client'
import { useAuth } from '../auth/AuthContext'

function Icon({ name }: { name: string }) {
  const common = { width: 18, height: 18, viewBox: '0 0 24 24', fill: 'none', stroke: 'currentColor', strokeWidth: 1.75 }
  switch (name) {
    case 'work':
      return (
        <svg {...common}>
          <rect x="3" y="5" width="18" height="14" rx="2" />
          <path d="M3 10h18" />
        </svg>
      )
    case 'projects':
      return (
        <svg {...common}>
          <path d="M4 7h16M4 12h16M4 17h10" />
        </svg>
      )
    case 'activity':
      return (
        <svg {...common}>
          <path d="M4 19V5M10 19V9M16 19V7M22 19V11" />
        </svg>
      )
    case 'planning':
      return (
        <svg {...common}>
          <rect x="4" y="4" width="16" height="16" rx="2" />
          <path d="M8 2v4M16 2v4M4 10h16" />
        </svg>
      )
    case 'settings':
      return (
        <svg {...common}>
          <circle cx="12" cy="12" r="3" />
          <path d="M12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4" />
        </svg>
      )
    case 'layers':
      return (
        <svg {...common}>
          <path d="M12 3l9 5-9 5-9-5 9-5z" />
          <path d="M3 13l9 5 9-5" />
          <path d="M3 18l9 5 9-5" />
        </svg>
      )
    default:
      return null
  }
}

export function AppShell({ children }: { children: React.ReactNode }) {
  const { user, orgs, orgId, setOrgId, logout } = useAuth()
  const { projectId } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const [projects, setProjects] = useState<Project[]>([])
  const [wsOpen, setWsOpen] = useState(false)

  const currentOrg = orgs.find((o) => o.organizationId === orgId)
  const projectsNavActive = location.pathname === '/' || location.pathname.startsWith('/projects/')

  useEffect(() => {
    if (!orgId) {
      setProjects([])
      return
    }
    void api.listOrgProjects(orgId).then(setProjects).catch(() => setProjects([]))
  }, [orgId])

  async function createWorkspace() {
    const name = window.prompt('Workspace name (e.g. Personal, Acme Studio)')
    if (!name?.trim()) return
    const org = await api.createOrg({ name: name.trim() })
    setOrgId(org.id)
    setWsOpen(false)
    navigate('/')
  }

  return (
    <div className="layout">
      <aside className="sidebar">
        <Link to="/" className="sidebar-brand">
          QSource
        </Link>

        <div className="ws-picker">
          <button type="button" className="ws-picker-btn" onClick={() => setWsOpen((v) => !v)}>
            <span>{currentOrg?.name ?? 'Workspace'}</span>
            <span className="chev">▾</span>
          </button>
          {wsOpen && (
            <div className="ws-picker-menu">
              {orgs.map((o) => (
                <button
                  key={o.organizationId}
                  type="button"
                  className={o.organizationId === orgId ? 'active' : ''}
                  onClick={() => {
                    setOrgId(o.organizationId)
                    setWsOpen(false)
                    navigate('/')
                  }}
                >
                  {o.name}
                </button>
              ))}
              <button type="button" className="create" onClick={() => void createWorkspace()}>
                + Create workspace
              </button>
            </div>
          )}
        </div>

        <nav className="sidebar-nav">
          <NavLink to="/" end className={({ isActive }) => (isActive && !projectId ? 'nav-item active' : 'nav-item')}>
            <Icon name="work" /> My work
          </NavLink>
          <NavLink to="/" className={() => (projectsNavActive ? 'nav-item active' : 'nav-item')}>
            <Icon name="projects" /> Projects
          </NavLink>
          <span className="nav-item disabled">
            <Icon name="activity" /> Activity
          </span>
          <span className="nav-item disabled">
            <Icon name="planning" /> Planning
          </span>
        </nav>

        <div className="sidebar-section">
          <p className="sidebar-label">Projects</p>
          <div className="sidebar-projects">
            {projects.map((p) => (
              <Link
                key={p.id}
                to={`/projects/${p.id}`}
                className={`sidebar-project${projectId === p.id ? ' active' : ''}`}
              >
                <span className="dot" />
                <span className="name">{p.name}</span>
              </Link>
            ))}
            {projects.length === 0 && <p className="sidebar-empty">No projects yet</p>}
          </div>
        </div>

        <div className="sidebar-foot">
          <div className="sidebar-user">
            <span className="avatar sm">{(user?.displayName ?? '?').slice(0, 1)}</span>
            <div>
              <strong>{user?.displayName}</strong>
              <button type="button" className="linkish" onClick={() => void logout()}>
                Sign out
              </button>
            </div>
          </div>
          <NavLink to="/settings" className={({ isActive }) => (isActive ? 'nav-item active' : 'nav-item')}>
            <Icon name="settings" /> Settings
          </NavLink>
        </div>
      </aside>

      <div className="layout-main">{children}</div>
    </div>
  )
}
