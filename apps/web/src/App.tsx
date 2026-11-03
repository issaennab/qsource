import { BrowserRouter, Link, Route, Routes } from 'react-router-dom'
import { ActorBar } from './components/ActorBar'
import { FeaturePage } from './pages/FeaturePage'
import { HomePage } from './pages/HomePage'
import { ProjectPage } from './pages/ProjectPage'
import './styles/app.css'

export default function App() {
  return (
    <BrowserRouter>
      <div className="app-shell">
        <header className="topbar">
          <Link to="/" className="brand">Q<span>Source</span></Link>
          <ActorBar />
        </header>
        <Routes>
          <Route path="/" element={<HomePage />} />
          <Route path="/projects/:projectId" element={<ProjectPage />} />
          <Route path="/features/:featureId" element={<FeaturePage />} />
        </Routes>
      </div>
    </BrowserRouter>
  )
}
