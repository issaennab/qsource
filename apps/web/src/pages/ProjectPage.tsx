import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, type Board, type Group, type Tag } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export function ProjectPage() {
  const { projectId = '' } = useParams()
  const { orgId } = useAuth()
  const [board, setBoard] = useState<Board | null>(null)
  const [groups, setGroups] = useState<Group[]>([])
  const [error, setError] = useState('')
  const [featureTitle, setFeatureTitle] = useState('')
  const [taskTitle, setTaskTitle] = useState('')
  const [tagFilter, setTagFilter] = useState('')
  const [newTag, setNewTag] = useState('')
  const [newGroup, setNewGroup] = useState('')
  const [selectedGroupIds, setSelectedGroupIds] = useState<string[]>([])

  async function load(tagId?: string) {
    try {
      const next = await api.board(projectId, tagId || undefined)
      setBoard(next)
      setError('')
      if (orgId) {
        setGroups(await api.listGroups(orgId))
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load board')
    }
  }

  useEffect(() => {
    void load(tagFilter)
  }, [projectId, orgId, tagFilter])

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
    await load(tagFilter)
  }

  async function addTask(e: React.FormEvent) {
    e.preventDefault()
    if (!taskTitle.trim()) return
    await api.createTask(projectId, { title: taskTitle.trim(), status: 'TODO', changeMessage: 'Initial task' })
    setTaskTitle('')
    await load(tagFilter)
  }

  async function createTag(e: React.FormEvent) {
    e.preventDefault()
    if (!orgId || !newTag.trim()) return
    await api.createTag(orgId, { name: newTag.trim() })
    setNewTag('')
    await load(tagFilter)
  }

  async function createGroup(e: React.FormEvent) {
    e.preventDefault()
    if (!orgId || !newGroup.trim()) return
    await api.createGroup(orgId, newGroup.trim())
    setNewGroup('')
    setGroups(await api.listGroups(orgId))
  }

  async function setVisibility(visibility: 'ORG' | 'RESTRICTED') {
    await api.updateProject(projectId, { visibility })
    await load(tagFilter)
  }

  async function saveRestrictedAccess() {
    await api.replaceProjectAccess(
      projectId,
      selectedGroupIds.map((id) => ({ type: 'GROUP' as const, id, permission: 'WRITE' as const })),
    )
    await load(tagFilter)
  }

  function renderTags(tags?: Tag[]) {
    if (!tags?.length) return null
    return (
      <span className="meta">
        {tags.map((t) => (
          <span key={t.id} className="badge" style={{ marginRight: 4 }}>
            {t.name}
          </span>
        ))}
      </span>
    )
  }

  if (!board && !error) return <p className="meta">Loading board…</p>
  if (error) return <p className="error">{error}</p>
  if (!board) return null

  return (
    <div className="stack loose">
      <div className="page-header">
        <Link className="meta" to="/">
          ← Projects
        </Link>
        <h1>{board.project.name}</h1>
        <p className="meta">{board.project.description}</p>
      </div>

      <section className="panel stack tight">
        <h2>Access & tags</h2>
        <div className="toolbar">
          <label>
            Visibility
            <select
              value={board.project.visibility}
              onChange={(e) => void setVisibility(e.target.value as 'ORG' | 'RESTRICTED')}
            >
              <option value="ORG">Entire company</option>
              <option value="RESTRICTED">Restricted</option>
            </select>
          </label>
          <label>
            Filter by tag
            <select value={tagFilter} onChange={(e) => setTagFilter(e.target.value)}>
              <option value="">All</option>
              {(board.orgTags ?? []).map((t) => (
                <option key={t.id} value={t.id}>
                  {t.name}
                </option>
              ))}
            </select>
          </label>
          <form onSubmit={createTag} className="grow">
            <label>
              New tag
              <div className="control-row">
                <input
                  value={newTag}
                  onChange={(e) => setNewTag(e.target.value)}
                  placeholder="e.g. client-support"
                />
                <button className="btn secondary" type="submit">
                  Add tag
                </button>
              </div>
            </label>
          </form>
        </div>
        {board.project.visibility === 'RESTRICTED' && (
          <div className="stack">
            <form onSubmit={createGroup} className="control-row">
              <input value={newGroup} onChange={(e) => setNewGroup(e.target.value)} placeholder="New group name" />
              <button className="btn secondary" type="submit">
                Add group
              </button>
            </form>
            <div className="list">
              {groups.map((g) => {
                const checked = selectedGroupIds.includes(g.id)
                return (
                  <label key={g.id} className="item" style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem' }}>
                    <input
                      type="checkbox"
                      checked={checked}
                      onChange={(e) => {
                        setSelectedGroupIds((prev) =>
                          e.target.checked ? [...prev, g.id] : prev.filter((id) => id !== g.id),
                        )
                      }}
                    />
                    <span>{g.name}</span>
                  </label>
                )
              })}
            </div>
            <button className="btn secondary" type="button" onClick={() => void saveRestrictedAccess()}>
              Save restricted access
            </button>
          </div>
        )}
      </section>

      <div className="grid">
        <section className="panel">
          <h2>Roadmap</h2>
          <div className="list">
            {board.roadmap.map((item) => (
              <div key={item.id} className="item">
                <strong>
                  {item.sequenceIndex + 1}. {item.title}
                </strong>
                <span className="meta">{item.summary}</span>
              </div>
            ))}
            {board.roadmap.length === 0 && <p className="meta">No roadmap items yet.</p>}
          </div>
        </section>
        <section className="panel stack tight">
          <h2>Features</h2>
          <form onSubmit={addFeature} className="control-row">
            <input value={featureTitle} onChange={(e) => setFeatureTitle(e.target.value)} placeholder="New feature title" />
            <button className="btn secondary" type="submit">
              Add
            </button>
          </form>
          <div className="list">
            {board.features.map((f) => (
              <Link key={f.id} className="item" to={`/features/${f.id}`}>
                <strong>{f.title}</strong>
                {renderTags(f.tags)}
                <span className="meta">Updated {new Date(f.updatedAt).toLocaleString()}</span>
              </Link>
            ))}
          </div>
        </section>
        <section className="panel stack tight">
          <h2>Tasks</h2>
          <form onSubmit={addTask} className="control-row">
            <input value={taskTitle} onChange={(e) => setTaskTitle(e.target.value)} placeholder="New task title" />
            <button className="btn secondary" type="submit">
              Add
            </button>
          </form>
          <div className="list">
            {board.tasks.map((t) => (
              <div key={t.id} className="item">
                <strong>{t.title}</strong>
                {renderTags(t.tags)}
                <span className="meta">
                  <span className={`badge ${t.status.toLowerCase()}`}>{t.status.replace('_', ' ')}</span>
                </span>
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
