import { useEffect, useMemo, useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, type Board, type Group } from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { KanbanBoard, type BoardStatus } from '../components/KanbanBoard'
import { initials, relativeTime } from '../lib/ticketMeta'

type Tab = 'board' | 'planning' | 'roadmap' | 'features' | 'activity'

export function ProjectPage() {
  const { projectId = '' } = useParams()
  const { orgId, user } = useAuth()
  const [board, setBoard] = useState<Board | null>(null)
  const [members, setMembers] = useState<{ displayName: string; email: string }[]>([])
  const [groups, setGroups] = useState<Group[]>([])
  const [error, setError] = useState('')
  const [tab, setTab] = useState<Tab>('board')
  const [taskTitle, setTaskTitle] = useState('')
  const [taskDue, setTaskDue] = useState('')
  const [showComposer, setShowComposer] = useState(false)
  const [featureTitle, setFeatureTitle] = useState('')
  const [roadmapTitle, setRoadmapTitle] = useState('')
  const [roadmapSummary, setRoadmapSummary] = useState('')
  const [tagFilter, setTagFilter] = useState('')
  const [newTag, setNewTag] = useState('')
  const [newGroup, setNewGroup] = useState('')
  const [selectedGroupIds, setSelectedGroupIds] = useState<string[]>([])
  const [showSettings, setShowSettings] = useState(false)
  const composerRef = useRef<HTMLInputElement>(null)

  async function load(tagId?: string) {
    try {
      const next = await api.board(projectId, tagId || undefined)
      setBoard(next)
      setError('')
      if (orgId) {
        const [m, g] = await Promise.all([api.listOrgMembers(orgId), api.listGroups(orgId)])
        setMembers(m)
        setGroups(g)
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load board')
    }
  }

  useEffect(() => {
    void load(tagFilter)
  }, [projectId, orgId, tagFilter])

  useEffect(() => {
    setTab('board')
    setShowComposer(false)
    setShowSettings(false)
  }, [projectId])

  useEffect(() => {
    if (showComposer) composerRef.current?.focus()
  }, [showComposer])

  const agentCount = useMemo(
    () => new Set((board?.activity ?? []).filter((a) => a.source === 'AGENT').map((a) => a.actorName)).size,
    [board],
  )

  async function addTask(e: React.FormEvent) {
    e.preventDefault()
    if (!taskTitle.trim()) return
    await api.createTask(projectId, {
      title: taskTitle.trim(),
      status: 'TODO',
      estimatedDeliveryDate: taskDue || undefined,
      changeMessage: 'Initial task',
    })
    setTaskTitle('')
    setTaskDue('')
    setShowComposer(false)
    await load(tagFilter)
  }

  async function placeTask(taskId: string, status: BoardStatus, sortOrder: number) {
    setBoard((prev) =>
      prev
        ? {
            ...prev,
            tasks: prev.tasks.map((t) => (t.id === taskId ? { ...t, status, sortOrder } : t)),
          }
        : prev,
    )
    try {
      await api.placeTask(taskId, { status, sortOrder })
      await load(tagFilter)
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to move task')
      await load(tagFilter)
      throw e
    }
  }

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

  async function addRoadmapItem(e: React.FormEvent) {
    e.preventDefault()
    if (!roadmapTitle.trim()) return
    await api.addRoadmapItem(projectId, {
      title: roadmapTitle.trim(),
      summary: roadmapSummary.trim() || undefined,
      sequenceIndex: board?.roadmap.length ?? 0,
    })
    setRoadmapTitle('')
    setRoadmapSummary('')
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

  if (!board && !error) return <p className="page-loading">Loading board…</p>
  if (error && !board) return <p className="error pad">{error}</p>
  if (!board) return null

  const visibleMembers = members.slice(0, 4)
  const extraMembers = Math.max(0, members.length - visibleMembers.length)

  return (
    <div className="project-layout">
      <div className="project-main">
        <header className="project-header">
          <div className="project-header-top">
            <div className="project-title-block">
              <div className="project-icon" aria-hidden>
                <svg width="22" height="22" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.8">
                  <path d="M12 3l9 5-9 5-9-5 9-5z" />
                  <path d="M3 13l9 5 9-5M3 18l9 5 9-5" />
                </svg>
              </div>
              <div>
                <div className="project-title-row">
                  <h1>{board.project.name}</h1>
                  <span className="pill-badge">{board.project.visibility}</span>
                </div>
                <p className="project-sub">
                  {board.project.description || 'Ticket and planning platform for humans and AI agents.'}
                </p>
              </div>
            </div>

            <div className="project-header-actions">
              <div className="avatar-stack" title="Workspace members">
                {visibleMembers.map((m) => (
                  <span key={m.email} className="avatar" title={m.displayName}>
                    {initials(m.displayName)}
                  </span>
                ))}
                {extraMembers > 0 && <span className="avatar more">+{extraMembers}</span>}
              </div>
              <span className="agents-pill">
                <span className="live-dot" />
                {agentCount} agent{agentCount === 1 ? '' : 's'} connected
              </span>
              <label className="search-field">
                <span className="sr-only">Search</span>
                <input placeholder="Search…" disabled />
                <kbd>⌘K</kbd>
              </label>
              <button className="btn primary" type="button" onClick={() => setShowComposer(true)}>
                + New ticket
              </button>
              <button className="btn ghost" type="button" onClick={() => setShowSettings((v) => !v)}>
                ···
              </button>
            </div>
          </div>

          <div className="tabs">
            {(
              [
                ['board', 'Board'],
                ['planning', 'Planning'],
                ['features', 'Features'],
                ['roadmap', 'Roadmap'],
                ['activity', 'Activity'],
              ] as const
            ).map(([id, label]) => (
              <button
                key={id}
                type="button"
                className={`tab${tab === id ? ' active' : ''}`}
                onClick={() => setTab(id)}
              >
                {label}
              </button>
            ))}
          </div>
        </header>

        {error && <p className="error pad">{error}</p>}

        {showSettings && (
          <section className="panel-inset stack tight">
            <h2>Project settings</h2>
            <div className="toolbar">
              <label>
                Visibility
                <select
                  value={board.project.visibility}
                  onChange={(e) => void setVisibility(e.target.value as 'ORG' | 'RESTRICTED')}
                >
                  <option value="ORG">Entire workspace</option>
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
                    <input value={newTag} onChange={(e) => setNewTag(e.target.value)} placeholder="e.g. Payments" />
                    <button className="btn secondary" type="submit">
                      Add
                    </button>
                  </div>
                </label>
              </form>
            </div>
            {board.project.visibility === 'RESTRICTED' && (
              <div className="stack">
                <form onSubmit={createGroup} className="control-row">
                  <input value={newGroup} onChange={(e) => setNewGroup(e.target.value)} placeholder="New group" />
                  <button className="btn secondary" type="submit">
                    Add group
                  </button>
                </form>
                <div className="list">
                  {groups.map((g) => (
                    <label key={g.id} className="item row">
                      <input
                        type="checkbox"
                        checked={selectedGroupIds.includes(g.id)}
                        onChange={(e) =>
                          setSelectedGroupIds((prev) =>
                            e.target.checked ? [...prev, g.id] : prev.filter((id) => id !== g.id),
                          )
                        }
                      />
                      {g.name}
                    </label>
                  ))}
                </div>
                <button className="btn secondary" type="button" onClick={() => void saveRestrictedAccess()}>
                  Save restricted access
                </button>
              </div>
            )}
          </section>
        )}

        {showComposer && (
          <form className="composer" onSubmit={addTask}>
            <input
              ref={composerRef}
              value={taskTitle}
              onChange={(e) => setTaskTitle(e.target.value)}
              placeholder="Ticket title"
            />
            <input type="date" value={taskDue} onChange={(e) => setTaskDue(e.target.value)} title="Estimated delivery" />
            <button className="btn primary" type="submit">
              Create
            </button>
            <button className="btn ghost" type="button" onClick={() => setShowComposer(false)}>
              Cancel
            </button>
          </form>
        )}

        {tab === 'board' && (
          <KanbanBoard
            projectName={board.project.name}
            tasks={board.tasks}
            userDisplayName={user?.displayName}
            onPlace={placeTask}
          />
        )}

        {tab === 'planning' && (
          <PlanningBoard
            board={board}
            onAddFeature={addFeature}
            featureTitle={featureTitle}
            setFeatureTitle={setFeatureTitle}
          />
        )}

        {tab === 'roadmap' && (
          <RoadmapView
            board={board}
            title={roadmapTitle}
            summary={roadmapSummary}
            setTitle={setRoadmapTitle}
            setSummary={setRoadmapSummary}
            onAdd={addRoadmapItem}
          />
        )}

        {tab === 'features' && (
          <section className="panel-inset stack">
            <div className="settings-title-row">
              <div>
                <h2>Features</h2>
                <p className="muted">All themes for this project. Open one to edit outcome and revisions.</p>
              </div>
            </div>
            <form onSubmit={addFeature} className="control-row">
              <input
                value={featureTitle}
                onChange={(e) => setFeatureTitle(e.target.value)}
                placeholder="New feature title"
              />
              <button className="btn secondary" type="submit">
                Add feature
              </button>
            </form>
            <div className="card-list">
              {board.features.map((f) => {
                const linked = board.tasks.filter((t) => t.featureId === f.id)
                return (
                  <Link key={f.id} className="soft-card" to={`/features/${f.id}`}>
                    <strong>{f.title}</strong>
                    <span className="muted">
                      {linked.length} ticket{linked.length === 1 ? '' : 's'} · Updated{' '}
                      {new Date(f.updatedAt).toLocaleString()}
                    </span>
                  </Link>
                )
              })}
              {board.features.length === 0 && <p className="muted">No features yet.</p>}
            </div>
          </section>
        )}

        {tab === 'activity' && (
          <section className="panel-inset">
            <ActivityList items={board.activity} />
          </section>
        )}
      </div>

      <aside className="activity-rail">
        <h2>Latest activity</h2>
        <ActivityList items={board.activity.slice(0, 12)} />
      </aside>
    </div>
  )
}

function PlanningBoard({
  board,
  onAddFeature,
  featureTitle,
  setFeatureTitle,
}: {
  board: Board
  onAddFeature: (e: React.FormEvent) => Promise<void>
  featureTitle: string
  setFeatureTitle: (v: string) => void
}) {
  const features = board.features
  const third = Math.max(1, Math.ceil(features.length / 3) || 1)
  const now = features.slice(0, third)
  const next = features.slice(third, third * 2)
  const later = features.slice(third * 2)
  const openTickets = board.tasks.filter((t) => t.status !== 'DONE').length

  const columns = [
    { key: 'now', label: 'Now', hint: 'Active themes this cycle', tone: 'now', items: now },
    { key: 'next', label: 'Next', hint: 'Queued features', tone: 'next', items: next },
    { key: 'later', label: 'Later', hint: 'Future themes', tone: 'later', items: later },
  ] as const

  return (
    <div className="planning-page stack">
      <div className="settings-title-row">
        <div>
          <h2>Planning</h2>
          <p className="muted">
            Feature themes for {board.project.name} — what epics humans and agents should work under.
          </p>
        </div>
        <form onSubmit={onAddFeature} className="composer" style={{ maxWidth: 420 }}>
          <input
            value={featureTitle}
            onChange={(e) => setFeatureTitle(e.target.value)}
            placeholder="New feature"
          />
          <button className="btn primary" type="submit">
            + New feature
          </button>
        </form>
      </div>

      <div className="stat-row">
        <div className="stat-card">
          <strong>{features.length} features</strong>
          <span className="muted">Themes / epics on this project.</span>
        </div>
        <div className="stat-card">
          <strong>{openTickets} open tickets</strong>
          <span className="muted">On the board under these themes.</span>
        </div>
      </div>

      <div className="planning-kanban">
        {columns.map((col) => (
          <section key={col.key} className={`plan-col ${col.tone}`}>
            <header>
              <strong>{col.label}</strong>
              <span className="muted">{col.hint}</span>
            </header>
            <div className="card-list">
              {col.items.map((f) => {
                const linked = board.tasks.filter((t) => t.featureId === f.id)
                const done = linked.filter((t) => t.status === 'DONE').length
                const pct = linked.length ? Math.round((done / linked.length) * 100) : 0
                return (
                  <Link key={f.id} className="plan-card" to={`/features/${f.id}`}>
                    <strong>{f.title}</strong>
                    <div className="progress">
                      <span style={{ width: `${pct}%` }} />
                    </div>
                    <div className="plan-meta">
                      <span className="muted">{linked.length} tickets</span>
                      <span className="tag slate">{pct}%</span>
                    </div>
                  </Link>
                )
              })}
              {col.items.length === 0 && <p className="muted">Nothing here yet.</p>}
            </div>
          </section>
        ))}
      </div>
    </div>
  )
}

function RoadmapView({
  board,
  title,
  summary,
  setTitle,
  setSummary,
  onAdd,
}: {
  board: Board
  title: string
  summary: string
  setTitle: (v: string) => void
  setSummary: (v: string) => void
  onAdd: (e: React.FormEvent) => Promise<void>
}) {
  const ordered = [...board.roadmap].sort((a, b) => a.sequenceIndex - b.sequenceIndex)

  return (
    <div className="planning-page stack">
      <div className="settings-title-row">
        <div>
          <h2>Roadmap</h2>
          <p className="muted">
            Ordered milestones for {board.project.name}. Separate from feature themes on Planning.
          </p>
        </div>
      </div>

      <form onSubmit={onAdd} className="composer" style={{ maxWidth: 560 }}>
        <input value={title} onChange={(e) => setTitle(e.target.value)} placeholder="Milestone title" />
        <input
          value={summary}
          onChange={(e) => setSummary(e.target.value)}
          placeholder="Short summary (optional)"
        />
        <button className="btn primary" type="submit">
          + Add milestone
        </button>
      </form>

      <section className="panel-inset">
        <div className="card-list">
          {ordered.map((item, i) => {
            const linked = item.linkedFeatureId
              ? board.features.find((f) => f.id === item.linkedFeatureId)
              : undefined
            return (
              <div key={item.id} className="soft-card">
                <strong>
                  {i + 1}. {item.title}
                </strong>
                {item.summary ? <span className="muted">{item.summary}</span> : null}
                {linked ? (
                  <span className="muted">
                    Linked feature:{' '}
                    <Link to={`/features/${linked.id}`}>{linked.title}</Link>
                  </span>
                ) : null}
              </div>
            )
          })}
          {ordered.length === 0 && <p className="muted">No roadmap milestones yet.</p>}
        </div>
      </section>
    </div>
  )
}

function ActivityList({
  items,
}: {
  items: { id: string; summary: string; actorName: string; source: string; createdAt: string }[]
}) {
  if (!items.length) return <p className="muted">No activity yet.</p>
  return (
    <ul className="activity-list">
      {items.map((a) => {
        const agent = a.source === 'AGENT' || a.source === 'API'
        return (
          <li key={a.id}>
            <span className={`avatar sm${agent ? ' agent' : ''}`}>
              {agent ? '✦' : initials(a.actorName)}
            </span>
            <div>
              <p>
                <strong>{a.actorName}</strong> {a.summary}
              </p>
              <time>{relativeTime(a.createdAt)}</time>
            </div>
          </li>
        )
      })}
    </ul>
  )
}
