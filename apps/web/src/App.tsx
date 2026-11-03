import { BrowserRouter, Link, Navigate, Route, Routes } from 'react-router-dom'
import { AuthBar } from './components/AuthBar'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { FeaturePage } from './pages/FeaturePage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { ProjectPage } from './pages/ProjectPage'
import { SignupPage } from './pages/SignupPage'
import './styles/app.css'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <p className="meta">Loading session…</p>
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route
        path="/"
        element={
          <RequireAuth>
            <HomePage />
          </RequireAuth>
        }
      />
      <Route
        path="/projects/:projectId"
        element={
          <RequireAuth>
            <ProjectPage />
          </RequireAuth>
        }
      />
      <Route
        path="/features/:featureId"
        element={
          <RequireAuth>
            <FeaturePage />
          </RequireAuth>
        }
      />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <div className="app-shell">
          <header className="topbar">
            <Link to="/" className="brand">
              Q<span>Source</span>
            </Link>
            <AuthBar />
          </header>
          <AppRoutes />
        </div>
      </BrowserRouter>
    </AuthProvider>
  )
}
