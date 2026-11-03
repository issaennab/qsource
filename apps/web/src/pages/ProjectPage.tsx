import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, type Board } from '../api/client'

export function ProjectPage() {
  const { projectId = '' } = useParams()
  const [board, setBoard] = useState<Board | null>(null)
  const [error, setError] = useState('')
  const [featureTitle, setFeatureTitle] = useState('')
  const [taskTitle, setTaskTitle] = useState('')

  async function load() {
    try {
      setBoard(await api.board(projectId))
      setError('')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load board')
    }
  }

  useEffect(() => {
    void load()
  }, [projectId])

  async function addFeature(e: React.FormEvent) {
    e.preventDefault()
    if (!featureTitle.trim()) return
    await api.createFeature(projectId, {
      title: featureTitle.trim(),
      outcome: '',
      requirements: '',
      successCriteria: '',
      changeMessage: 'Initial revision',
    })
    setFeatureTitle('')
    await load()
  }

  async function addTask(e: React.FormEvent) {
    e.preventDefault()
    if (!taskTitle.trim()) return
    await api.createTask(projectId, { title: taskTitle.trim(), status: 'TODO', changeMessage: 'Initial task' })
    setTaskTitle('')
    await load()
  }

  if (!board && !error) return <p className="meta">Loading board…</p>
  if (error) return <p className="error">{error}</p>
  if (!board) return null

  return (
    <div className="stack">
      <div>
        <Link className="meta" to="/">← Projects</Link>
        <h1 style={{ fontFamily: 'Fraunces, Georgia, serif', margin: '0.35rem 0' }}>{board.project.name}</h1>
        <p className="meta">{board.project.description}</p>
      </div>
      <div className="grid">
        <section className="panel">
          <h2>Roadmap</h2>
          <div className="list">
            {board.roadmap.map((item) => (
              <div key={item.id} className="item">
                <strong>{item.sequenceIndex + 1}. {item.title}</strong>
                <span className="meta">{item.summary}</span>
              </div>
            ))}
            {board.roadmap.length === 0 && <p className="meta">No roadmap items yet.</p>}
          </div>
        </section>
        <section className="panel stack">
          <h2>Features</h2>
          <form onSubmit={addFeature} className="stack">
            <input value={featureTitle} onChange={(e) => setFeatureTitle(e.target.value)} placeholder="New feature title" />
            <button className="btn secondary" type="submit">Add feature</button>
          </form>
          <div className="list">
            {board.features.map((f) => (
              <Link key={f.id} className="item" to={`/features/${f.id}`}>
                <strong>{f.title}</strong>
                <span className="meta">Updated {new Date(f.updatedAt).toLocaleString()}</span>
              </Link>
            ))}
          </div>
        </section>
        <section className="panel stack">
          <h2>Tasks</h2>
          <form onSubmit={addTask} className="stack">
            <input value={taskTitle} onChange={(e) => setTaskTitle(e.target.value)} placeholder="New task title" />
            <button className="btn secondary" type="submit">Add task</button>
          </form>
          <div className="list">
            {board.tasks.map((t) => (
              <div key={t.id} className="item">
                <strong>{t.title}</strong>
                <span className="meta"><span className="badge">{t.status}</span></span>
              </div>
            ))}
          </div>
        </section>
      </div>
      <section className="panel">
        <h2>Activity</h2>
        <div className="list">
          {board.activity.map((a) => (
            <div key={a.id} className="item">
              <strong>{a.summary}</strong>
              <span className="meta">
                {a.actorName} · <span className={`badge ${a.source.toLowerCase()}`}>{a.source}</span> ·{' '}
                {new Date(a.createdAt).toLocaleString()}
              </span>
            </div>
          ))}
        </div>
      </section>
    </div>
  )
}
