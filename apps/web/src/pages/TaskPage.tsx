import { useEffect, useMemo, useState } from 'react'
import { Link, useNavigate, useParams, useSearchParams } from 'react-router-dom'
import {
  api,
  type Comment,
  type Project,
  type Revision,
  type Tag,
  type TaskSummary,
} from '../api/client'
import { useAuth } from '../auth/AuthContext'
import { formatDay, initials, relativeTime, ticketKey } from '../lib/ticketMeta'

type Tab = 'discussion' | 'history' | 'activity'

type TaskDetail = TaskSummary & {
  currentContent: {
    title?: string
    description?: string
    status?: string
    estimatedDeliveryDate?: string
  }
}

const STATUS_OPTS = ['TODO', 'IN_PROGRESS', 'DONE'] as const

export function TaskPage() {
  const { taskId = '' } = useParams()
  const [search] = useSearchParams()
  const navigate = useNavigate()
  const { user, orgId } = useAuth()
  const [task, setTask] = useState<TaskDetail | null>(null)
  const [project, setProject] = useState<Project | null>(null)
  const [history, setHistory] = useState<Revision[]>([])
  const [comments, setComments] = useState<Comment[]>([])
  const [orgTags, setOrgTags] = useState<Tag[]>([])
  const [error, setError] = useState('')
  const [tab, setTab] = useState<Tab>(search.get('tab') === 'history' ? 'history' : 'discussion')
  const [commentBody, setCommentBody] = useState('')
  const [editing, setEditing] = useState(false)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')
  const [due, setDue] = useState('')
  const [changeMessage, setChangeMessage] = useState('')
  const [saving, setSaving] = useState(false)
  const [leftVer, setLeftVer] = useState<number | null>(null)
  const [rightVer, setRightVer] = useState<number | null>(null)

  async function load() {
    try {
      const [t, h, c] = await Promise.all([
        api.getTask(taskId),
        api.taskHistory(taskId),
        api.taskComments(taskId),
      ])
      setTask(t)
      setHistory(h)
      setComments(c)
      setTitle(t.title)
      setDescription(t.description || String(t.currentContent?.description ?? ''))
      setDue(t.estimatedDeliveryDate ?? '')
      const sorted = [...h].sort((a, b) => a.version - b.version)
      if (sorted.length) {
        setRightVer(sorted[sorted.length - 1].version)
        setLeftVer(sorted.length > 1 ? sorted[sorted.length - 2].version : sorted[0].version)
      }
      const board = await api.board(t.projectId)
      setProject(board.project)
      if (orgId) setOrgTags(await api.listTags(orgId))
      setError('')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load ticket')
    }
  }

  useEffect(() => {
    void load()
  }, [taskId, orgId])

  const key = useMemo(
    () => (task && project ? ticketKey(project.name, task.id) : ''),
    [task, project],
  )

  const sortedHistory = useMemo(
    () => [...history].sort((a, b) => a.version - b.version),
    [history],
  )

  const left = sortedHistory.find((r) => r.version === leftVer) ?? null
  const right = sortedHistory.find((r) => r.version === rightVer) ?? null

  async function setStatus(status: string) {
    if (!task) return
    await api.publishTask(task.id, {
      title: task.title,
      description: task.description ?? '',
      status,
      estimatedDeliveryDate: task.estimatedDeliveryDate || undefined,
      changeMessage: `Status → ${status}`,
    })
    await load()
  }

  async function saveEdit(e: React.FormEvent) {
    e.preventDefault()
    if (!task || !changeMessage.trim()) {
      setError('Change message is required to publish a revision.')
      return
    }
    setSaving(true)
    try {
      await api.publishTask(task.id, {
        title: title.trim(),
        description,
        status: task.status,
        estimatedDeliveryDate: due || undefined,
        changeMessage: changeMessage.trim(),
      })
      setEditing(false)
      setChangeMessage('')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Save failed')
    } finally {
      setSaving(false)
    }
  }

  async function addComment(e: React.FormEvent) {
    e.preventDefault()
    if (!commentBody.trim()) return
    await api.addTaskComment(taskId, commentBody.trim())
    setCommentBody('')
    await load()
  }

  async function restoreRevision(rev: Revision) {
    if (!task) return
    const content = rev.content ?? {}
    await api.publishTask(task.id, {
      title: String(content.title ?? task.title),
      description: String(content.description ?? ''),
      status: String(content.status ?? task.status),
      estimatedDeliveryDate:
        typeof content.estimatedDeliveryDate === 'string'
          ? content.estimatedDeliveryDate
          : task.estimatedDeliveryDate || undefined,
      changeMessage: `Restored from v${rev.version}`,
    })
    setTab('discussion')
    await load()
  }

  if (!task && !error) return <p className="page-loading">Loading ticket…</p>
  if (error && !task) return <p className="error pad">{error}</p>
  if (!task) return null

  return (
    <div className="ticket-page">
      <div className="ticket-main">
        <nav className="crumbs">
          <Link to={project ? `/projects/${project.id}` : '/'}>{project?.name ?? 'Project'}</Link>
          <span>/</span>
          <span>{key}</span>
        </nav>

        <header className="ticket-head">
          <div>
            <h1>{task.title}</h1>
            <p className="muted">{key}</p>
          </div>
          <div className="ticket-head-actions">
            <select
              className="status-select"
              value={task.status}
              onChange={(e) => void setStatus(e.target.value)}
            >
              {STATUS_OPTS.map((s) => (
                <option key={s} value={s}>
                  {s.replace('_', ' ')}
                </option>
              ))}
            </select>
            <button className="btn secondary" type="button" onClick={() => setEditing((v) => !v)}>
              {editing ? 'Cancel edit' : 'Edit'}
            </button>
            <button className="btn ghost" type="button" onClick={() => navigate(-1)}>
              ···
            </button>
          </div>
        </header>

        {error && <p className="error">{error}</p>}

        <section className="panel-inset current-rev">
          <div className="rev-banner">
            <strong>
              Current revision · v{sortedHistory.at(-1)?.version ?? 1}
            </strong>
            <span className="pill-green">Current</span>
          </div>
          <p className="muted">
            Published by {sortedHistory.at(-1)?.actorName ?? user?.displayName} ·{' '}
            {sortedHistory.at(-1) ? relativeTime(sortedHistory.at(-1)!.createdAt) : 'just now'}
            {sortedHistory.at(-1)?.message ? ` · “${sortedHistory.at(-1)!.message}”` : ''}
          </p>
        </section>

        {editing ? (
          <form className="panel-inset stack" onSubmit={saveEdit}>
            <label>
              Title
              <input value={title} onChange={(e) => setTitle(e.target.value)} required />
            </label>
            <label>
              Description
              <textarea value={description} onChange={(e) => setDescription(e.target.value)} rows={5} />
            </label>
            <label>
              Estimated delivery
              <input type="date" value={due} onChange={(e) => setDue(e.target.value)} />
            </label>
            <label>
              Change message
              <input
                value={changeMessage}
                onChange={(e) => setChangeMessage(e.target.value)}
                placeholder="What changed?"
                required
              />
            </label>
            <button className="btn primary" type="submit" disabled={saving}>
              {saving ? 'Publishing…' : 'Publish revision'}
            </button>
          </form>
        ) : (
          <section className="panel-inset stack">
            <h2>Overview</h2>
            <p>{task.description || 'No description yet.'}</p>
            {(task.tags?.length ?? 0) > 0 && (
              <div className="ticket-tags">
                {task.tags!.map((t) => (
                  <span key={t.id} className="tag violet">
                    {t.name}
                  </span>
                ))}
              </div>
            )}
          </section>
        )}

        <div className="tabs">
          {(
            [
              ['discussion', 'Discussion'],
              ['history', `History (${history.length})`],
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

        {tab === 'discussion' && (
          <section className="panel-inset stack">
            <div className="card-list">
              {comments.map((c) => (
                <div key={c.id} className="comment-row">
                  <span className={`avatar sm${c.source === 'AGENT' || c.source === 'API' ? ' agent' : ''}`}>
                    {c.source === 'AGENT' || c.source === 'API' ? '✦' : initials(c.actorName)}
                  </span>
                  <div>
                    <strong>{c.actorName}</strong>{' '}
                    <span className="muted">{relativeTime(c.createdAt)}</span>
                    <p>{c.body}</p>
                  </div>
                </div>
              ))}
              {comments.length === 0 && <p className="muted">No discussion yet.</p>}
            </div>
            <form className="composer" onSubmit={addComment}>
              <input
                value={commentBody}
                onChange={(e) => setCommentBody(e.target.value)}
                placeholder="Write a comment…"
              />
              <button className="btn primary" type="submit">
                Send
              </button>
            </form>
          </section>
        )}

        {tab === 'history' && (
          <RevisionCompare
            projectName={project?.name ?? 'Project'}
            ticketKey={key}
            title={task.title}
            history={sortedHistory}
            leftVer={leftVer}
            rightVer={rightVer}
            left={left}
            right={right}
            onLeft={setLeftVer}
            onRight={setRightVer}
            onRestore={(r) => void restoreRevision(r)}
          />
        )}

        {tab === 'activity' && (
          <section className="panel-inset">
            <ul className="activity-list">
              {sortedHistory
                .slice()
                .reverse()
                .map((r) => (
                  <li key={r.id}>
                    <span className={`avatar sm${r.source === 'AGENT' || r.source === 'API' ? ' agent' : ''}`}>
                      {r.source === 'AGENT' || r.source === 'API' ? '✦' : initials(r.actorName)}
                    </span>
                    <div>
                      <p>
                        <strong>{r.actorName}</strong> published v{r.version}
                        {r.message ? ` — ${r.message}` : ''}
                      </p>
                      <time>{relativeTime(r.createdAt)}</time>
                    </div>
                  </li>
                ))}
            </ul>
          </section>
        )}
      </div>

      <aside className="ticket-rail">
        <section className="rail-card">
          <h3>Details</h3>
          <dl className="detail-list">
            <div>
              <dt>Assignee</dt>
              <dd>
                <span className="avatar xs">{initials(user?.displayName ?? 'U')}</span> {user?.displayName}
              </dd>
            </div>
            <div>
              <dt>Status</dt>
              <dd>{task.status.replace('_', ' ')}</dd>
            </div>
            <div>
              <dt>Created</dt>
              <dd>{formatDay(task.createdAt) ?? '—'}</dd>
            </div>
            <div>
              <dt>Due</dt>
              <dd>{formatDay(task.estimatedDeliveryDate) ?? '—'}</dd>
            </div>
            <div>
              <dt>Tags</dt>
              <dd className="ticket-tags">
                {(task.tags ?? []).map((t) => (
                  <span key={t.id} className="tag violet">
                    {t.name}
                  </span>
                ))}
                {(task.tags?.length ?? 0) === 0 && <span className="muted">None</span>}
              </dd>
            </div>
          </dl>
        </section>
        {orgTags.length > 0 && (
          <section className="rail-card">
            <h3>Workspace tags</h3>
            <p className="muted">Available: {orgTags.map((t) => t.name).join(', ')}</p>
          </section>
        )}
      </aside>
    </div>
  )
}

function RevisionCompare({
  projectName,
  ticketKey: key,
  title,
  history,
  leftVer,
  rightVer,
  left,
  right,
  onLeft,
  onRight,
  onRestore,
}: {
  projectName: string
  ticketKey: string
  title: string
  history: Revision[]
  leftVer: number | null
  rightVer: number | null
  left: Revision | null
  right: Revision | null
  onLeft: (v: number) => void
  onRight: (v: number) => void
  onRestore: (r: Revision) => void
}) {
  if (!history.length) return <p className="muted pad">No revisions yet.</p>

  const leftDesc = String(left?.content?.description ?? '')
  const rightDesc = String(right?.content?.description ?? '')
  const leftTitle = String(left?.content?.title ?? title)
  const rightTitle = String(right?.content?.title ?? title)

  return (
    <div className="history-layout">
      <div className="history-main stack">
        <nav className="crumbs">
          <span>{projectName}</span>
          <span>/</span>
          <span>{key}</span>
          <span>/</span>
          <span>History</span>
        </nav>
        <div>
          <h2>{title}</h2>
          <p className="muted">Revision history</p>
        </div>

        <div className="rev-timeline">
          {history.map((r, i) => (
            <button
              key={r.id}
              type="button"
              className={`rev-node${r.version === rightVer ? ' current' : ''}${
                r.source === 'AGENT' || r.source === 'API' ? ' agent' : ''
              }`}
              onClick={() => {
                onRight(r.version)
                if (i > 0) onLeft(history[i - 1].version)
              }}
            >
              <span className="rev-dot">{r.source === 'AGENT' || r.source === 'API' ? '✦' : ''}</span>
              <strong>v{r.version}</strong>
              <span>{r.actorName}</span>
              <time>{formatDay(r.createdAt)}</time>
            </button>
          ))}
        </div>

        <div className="compare-controls">
          <label>
            Compare
            <select
              value={leftVer ?? ''}
              onChange={(e) => onLeft(Number(e.target.value))}
            >
              {history.map((r) => (
                <option key={r.id} value={r.version}>
                  v{r.version}
                </option>
              ))}
            </select>
          </label>
          <span className="muted">↔</span>
          <label>
            with
            <select
              value={rightVer ?? ''}
              onChange={(e) => onRight(Number(e.target.value))}
            >
              {history.map((r) => (
                <option key={r.id} value={r.version}>
                  v{r.version}
                </option>
              ))}
            </select>
          </label>
        </div>

        <div className="diff-grid">
          <article className="diff-pane prev">
            <header>Previous · v{left?.version}</header>
            <h3>{leftTitle}</h3>
            <p className="diff-body">{leftDesc || '—'}</p>
            <p className="muted">Status: {String(left?.content?.status ?? '—')}</p>
          </article>
          <article className="diff-pane curr">
            <header>Current · v{right?.version}</header>
            <h3>{rightTitle}</h3>
            <DiffText before={leftDesc} after={rightDesc} />
            <p className="muted">Status: {String(right?.content?.status ?? '—')}</p>
          </article>
        </div>

        <p className="info-banner">No content was overwritten. Earlier revisions remain available.</p>
      </div>

      <aside className="rail-card change-summary">
        <h3>Change summary</h3>
        <p>{right?.message || 'No change message.'}</p>
        <div className="change-author">
          <span className={`avatar${right?.source === 'AGENT' || right?.source === 'API' ? ' agent' : ''}`}>
            {right?.source === 'AGENT' || right?.source === 'API' ? '✦' : initials(right?.actorName ?? '?')}
          </span>
          <div>
            <strong>{right?.actorName}</strong>
            <p className="muted">{right ? relativeTime(right.createdAt) : ''}</p>
          </div>
        </div>
        <dl className="detail-list">
          <div>
            <dt>Source</dt>
            <dd>{right?.source ?? '—'}</dd>
          </div>
          <div>
            <dt>Based on</dt>
            <dd>
              <span className="tag key">v{left?.version ?? '—'}</span>
            </dd>
          </div>
        </dl>
        {right && (
          <button className="btn secondary" type="button" onClick={() => onRestore(right)}>
            Restore as new revision
          </button>
        )}
      </aside>
    </div>
  )
}

function DiffText({ before, after }: { before: string; after: string }) {
  if (before === after) return <p className="diff-body">{after || '—'}</p>
  return (
    <div className="diff-stack">
      {before && before !== after && <p className="diff-del">{before}</p>}
      <p className="diff-add">{after || '—'}</p>
    </div>
  )
}
