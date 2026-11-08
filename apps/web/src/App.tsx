import { BrowserRouter, Navigate, Route, Routes, useParams } from 'react-router-dom'
import { AppShell } from './components/AppShell'
import { AuthProvider, useAuth } from './auth/AuthContext'
import { FeaturePage } from './pages/FeaturePage'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { ProjectPage } from './pages/ProjectPage'
import { SettingsPage } from './pages/SettingsPage'
import { SignupPage } from './pages/SignupPage'
import { TaskPage } from './pages/TaskPage'
import './styles/app.css'

function RequireAuth({ children }: { children: React.ReactNode }) {
  const { user, loading } = useAuth()
  if (loading) return <p className="page-loading">Loading session…</p>
  if (!user) return <Navigate to="/login" replace />
  return <>{children}</>
}

function ShellLayout({ children }: { children: React.ReactNode }) {
  return (
    <RequireAuth>
      <AppShell>{children}</AppShell>
    </RequireAuth>
  )
}

function ProjectRoute() {
  const { projectId } = useParams()
  return (
    <ShellLayout>
      <ProjectPage key={projectId} />
    </ShellLayout>
  )
}

function AppRoutes() {
  return (
    <Routes>
      <Route path="/login" element={<LoginPage />} />
      <Route path="/signup" element={<SignupPage />} />
      <Route
        path="/"
        element={
          <ShellLayout>
            <HomePage />
          </ShellLayout>
        }
      />
      <Route path="/projects/:projectId" element={<ProjectRoute />} />
      <Route
        path="/tasks/:taskId"
        element={
          <ShellLayout>
            <TaskPage />
          </ShellLayout>
        }
      />
      <Route
        path="/features/:featureId"
        element={
          <ShellLayout>
            <FeaturePage />
          </ShellLayout>
        }
      />
      <Route
        path="/settings"
        element={
          <ShellLayout>
            <SettingsPage />
          </ShellLayout>
        }
      />
    </Routes>
  )
}

export default function App() {
  return (
    <AuthProvider>
      <BrowserRouter>
        <AppRoutes />
      </BrowserRouter>
    </AuthProvider>
  )
}
