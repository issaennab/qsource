const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

export type Actor = { name: string; source: 'web' | 'api' | 'agent' }

const defaultActor: Actor = { name: 'sarah', source: 'web' }

let actor: Actor = { ...defaultActor }

export function getActor() {
  return actor
}

export function setActor(next: Partial<Actor>) {
  actor = { ...actor, ...next }
}

async function request<T>(path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  headers.set('X-Actor-Name', actor.name)
  headers.set('X-Actor-Source', actor.source)
  const res = await fetch(`${API_BASE}${path}`, { ...init, headers })
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || res.statusText)
  }
  return res.json() as Promise<T>
}

export type Project = {
  id: string
  organizationId: string
  name: string
  description?: string
  createdAt: string
}

export type RoadmapItem = {
  id: string
  projectId: string
  title: string
  summary?: string
  sequenceIndex: number
  linkedFeatureId?: string
}

export type FeatureSummary = {
  id: string
  projectId: string
  title: string
  currentRevisionId?: string
  updatedAt: string
}

export type TaskSummary = {
  id: string
  projectId: string
  featureId?: string
  title: string
  status: string
  currentRevisionId?: string
  updatedAt: string
}

export type Activity = {
  id: string
  projectId: string
  kind: string
  summary: string
  actorName: string
  source: string
  relatedId?: string
  createdAt: string
}

export type Board = {
  project: Project
  roadmap: RoadmapItem[]
  features: FeatureSummary[]
  tasks: TaskSummary[]
  activity: Activity[]
}

export type FeatureDetail = FeatureSummary & {
  currentContent: {
    title?: string
    outcome?: string
    requirements?: string
    successCriteria?: string
  }
}

export type Revision = {
  id: string
  targetType: string
  targetId: string
  version: number
  content: Record<string, unknown>
  message: string
  actorName: string
  source: string
  parentRevisionId?: string
  accepted: boolean
  createdAt: string
}

export type Comment = {
  id: string
  body: string
  actorName: string
  source: string
  createdAt: string
}

export const api = {
  listProjects: () => request<Project[]>('/api/projects'),
  createProject: (body: { name: string; description?: string }) =>
    request<Project>('/api/projects', { method: 'POST', body: JSON.stringify(body) }),
  board: (projectId: string) => request<Board>(`/api/projects/${projectId}/board`),
  getFeature: (id: string) => request<FeatureDetail>(`/api/features/${id}`),
  featureHistory: (id: string) => request<Revision[]>(`/api/features/${id}/revisions`),
  publishFeature: (
    id: string,
    body: {
      title: string
      outcome: string
      requirements: string
      successCriteria: string
      changeMessage: string
    },
  ) => request<FeatureDetail>(`/api/features/${id}/revisions`, { method: 'POST', body: JSON.stringify(body) }),
  featureComments: (id: string) => request<Comment[]>(`/api/features/${id}/comments`),
  addFeatureComment: (id: string, body: string) =>
    request<Comment>(`/api/features/${id}/comments`, { method: 'POST', body: JSON.stringify({ body }) }),
  createFeature: (
    projectId: string,
    body: {
      title: string
      outcome: string
      requirements: string
      successCriteria: string
      changeMessage?: string
    },
  ) =>
    request<FeatureDetail>(`/api/projects/${projectId}/features`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  createTask: (
    projectId: string,
    body: { featureId?: string; title: string; description?: string; status?: string; changeMessage?: string },
  ) =>
    request(`/api/projects/${projectId}/tasks`, { method: 'POST', body: JSON.stringify(body) }),
}
