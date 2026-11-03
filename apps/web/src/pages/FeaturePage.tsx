import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { api, type Comment, type FeatureDetail, type Revision, type Tag } from '../api/client'
import { useAuth } from '../auth/AuthContext'

export function FeaturePage() {
  const { featureId = '' } = useParams()
  const { orgId } = useAuth()
  const [feature, setFeature] = useState<FeatureDetail | null>(null)
  const [history, setHistory] = useState<Revision[]>([])
  const [comments, setComments] = useState<Comment[]>([])
  const [orgTags, setOrgTags] = useState<Tag[]>([])
  const [selectedTagIds, setSelectedTagIds] = useState<string[]>([])
  const [title, setTitle] = useState('')
  const [outcome, setOutcome] = useState('')
  const [requirements, setRequirements] = useState('')
  const [successCriteria, setSuccessCriteria] = useState('')
  const [changeMessage, setChangeMessage] = useState('')
  const [commentBody, setCommentBody] = useState('')
  const [error, setError] = useState('')
  const [saving, setSaving] = useState(false)

  async function load() {
    try {
      const [f, h, c] = await Promise.all([
        api.getFeature(featureId),
        api.featureHistory(featureId),
        api.featureComments(featureId),
      ])
      setFeature(f)
      setHistory(h)
      setComments(c)
      setSelectedTagIds((f.tags ?? []).map((t) => t.id))
      setTitle(f.currentContent?.title ?? f.title)
      setOutcome(String(f.currentContent?.outcome ?? ''))
      setRequirements(String(f.currentContent?.requirements ?? ''))
      setSuccessCriteria(String(f.currentContent?.successCriteria ?? ''))
      if (orgId) {
        setOrgTags(await api.listTags(orgId))
      }
      setError('')
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Failed to load feature')
    }
  }

  useEffect(() => {
    void load()
  }, [featureId, orgId])

  async function saveTags(e: React.FormEvent) {
    e.preventDefault()
    await api.setFeatureTags(featureId, selectedTagIds)
    await load()
  }

  async function publish(e: React.FormEvent) {
    e.preventDefault()
    if (!changeMessage.trim()) {
      setError('Change message is required to publish a revision.')
      return
    }
    setSaving(true)
    try {
      await api.publishFeature(featureId, {
        title,
        outcome,
        requirements,
        successCriteria,
        changeMessage: changeMessage.trim(),
      })
      setChangeMessage('')
      await load()
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Publish failed')
    } finally {
      setSaving(false)
    }
  }

  async function addComment(e: React.FormEvent) {
    e.preventDefault()
    if (!commentBody.trim()) return
    await api.addFeatureComment(featureId, commentBody.trim())
    setCommentBody('')
    await load()
  }

  if (!feature && !error) return <p className="meta">Loading feature…</p>
  if (error && !feature) return <p className="error">{error}</p>
  if (!feature) return null

  return (
    <div className="stack">
      <div>
        <Link className="meta" to={`/projects/${feature.projectId}`}>← Project</Link>
        <h1 style={{ fontFamily: 'Fraunces, Georgia, serif', margin: '0.35rem 0' }}>{feature.title}</h1>
      </div>
      {error && <p className="error">{error}</p>}
      <div className="split">
        <div className="stack">
        <form className="panel stack" onSubmit={publish}>
          <h2>Current working view</h2>
          <label>Title<input value={title} onChange={(e) => setTitle(e.target.value)} /></label>
          <label>Outcome<textarea value={outcome} onChange={(e) => setOutcome(e.target.value)} /></label>
          <label>Requirements<textarea value={requirements} onChange={(e) => setRequirements(e.target.value)} /></label>
          <label>Success criteria<textarea value={successCriteria} onChange={(e) => setSuccessCriteria(e.target.value)} /></label>
          <label>Change message (required to publish)<input value={changeMessage} onChange={(e) => setChangeMessage(e.target.value)} placeholder="What changed and why?" /></label>
          <button className="btn" type="submit" disabled={saving}>{saving ? 'Publishing…' : 'Publish revision'}</button>
        </form>
        <form className="panel stack" onSubmit={saveTags}>
          <h2>Tags</h2>
          <div className="list">
            {orgTags.map((t) => {
              const checked = selectedTagIds.includes(t.id)
              return (
                <label key={t.id} className="item" style={{ flexDirection: 'row', alignItems: 'center', gap: '0.5rem' }}>
                  <input
                    type="checkbox"
                    checked={checked}
                    onChange={(e) => {
                      setSelectedTagIds((prev) =>
                        e.target.checked ? [...prev, t.id] : prev.filter((id) => id !== t.id),
                      )
                    }}
                  />
                  <span>{t.name}</span>
                </label>
              )
            })}
            {orgTags.length === 0 && <p className="meta">Create tags on the project board first.</p>}
          </div>
          <button className="btn secondary" type="submit">Save tags</button>
        </form>
        </div>
        <div className="stack">
          <section className="panel">
            <h2>History</h2>
            <div className="list history">
              {history.map((r) => (
                <div key={r.id} className="item">
                  <strong>v{r.version} — {r.message}</strong>
                  <span className="meta">
                    {r.actorName} · <span className={`badge ${r.source.toLowerCase()}`}>{r.source}</span> ·{' '}
                    {new Date(r.createdAt).toLocaleString()}
                  </span>
                </div>
              ))}
            </div>
          </section>
          <section className="panel stack">
            <h2>Comments (non-authoritative)</h2>
            <form onSubmit={addComment} className="stack">
              <textarea value={commentBody} onChange={(e) => setCommentBody(e.target.value)} placeholder="Discuss without changing the revision…" />
              <button className="btn secondary" type="submit">Add comment</button>
            </form>
            <div className="list">
              {comments.map((c) => (
                <div key={c.id} className="item">
                  <strong>{c.actorName}</strong>
                  <span className="meta">{c.body}</span>
                </div>
              ))}
            </div>
          </section>
        </div>
      </div>
    </div>
  )
}
