const API_BASE = import.meta.env.VITE_API_BASE_URL ?? ''

const ACCESS_KEY = 'qsource_access_token'
const REFRESH_KEY = 'qsource_refresh_token'
const ORG_KEY = 'qsource_org_id'

export type User = { id: string; email: string; displayName: string; createdAt: string }
export type OrgMembership = { organizationId: string; name: string; slug: string; role: string }
export type Org = { id: string; name: string; slug: string; createdAt: string }
export type Tag = { id: string; organizationId: string; name: string; color?: string; createdAt: string }

export type Project = {
  id: string
  organizationId: string
  name: string
  description?: string
  visibility: 'ORG' | 'RESTRICTED'
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
  tags?: Tag[]
}

export type TaskSummary = {
  id: string
  projectId: string
  featureId?: string
  title: string
  description?: string
  status: string
  sortOrder: number
  estimatedDeliveryDate?: string
  currentRevisionId?: string
  createdAt: string
  updatedAt: string
  tags?: Tag[]
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
  orgTags?: Tag[]
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

export type Group = {
  id: string
  organizationId: string
  name: string
  createdAt: string
  memberIds: string[]
}

export type AuthTokens = {
  accessToken: string
  refreshToken: string
  tokenType: string
  user: User
}

export function getAccessToken() {
  return localStorage.getItem(ACCESS_KEY)
}

export function getRefreshToken() {
  return localStorage.getItem(REFRESH_KEY)
}

export function getSelectedOrgId() {
  return localStorage.getItem(ORG_KEY)
}

export function setSelectedOrgId(orgId: string) {
  localStorage.setItem(ORG_KEY, orgId)
}

export function clearSession() {
  localStorage.removeItem(ACCESS_KEY)
  localStorage.removeItem(REFRESH_KEY)
}

function storeTokens(tokens: AuthTokens) {
  localStorage.setItem(ACCESS_KEY, tokens.accessToken)
  localStorage.setItem(REFRESH_KEY, tokens.refreshToken)
}

async function request<T>(path: string, init: RequestInit = {}, retry = true): Promise<T> {
  const headers = new Headers(init.headers)
  headers.set('Content-Type', 'application/json')
  headers.set('X-Actor-Source', 'web')
  const token = getAccessToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }
  const res = await fetch(`${API_BASE}${path}`, { ...init, headers })
  if (res.status === 401 && retry && getRefreshToken() && !path.startsWith('/api/auth/')) {
    const refreshed = await api.refresh({ refreshToken: getRefreshToken()! }).catch(() => null)
    if (refreshed) {
      storeTokens(refreshed)
      return request<T>(path, init, false)
    }
    clearSession()
  }
  if (!res.ok) {
    const text = await res.text()
    throw new Error(text || res.statusText)
  }
  if (res.status === 204) {
    return undefined as T
  }
  return res.json() as Promise<T>
}

export const api = {
  signup: async (body: {
    email: string
    password: string
    displayName: string
    organizationName: string
  }) => {
    const tokens = await request<AuthTokens>('/api/auth/signup', {
      method: 'POST',
      body: JSON.stringify(body),
    }, false)
    storeTokens(tokens)
    return tokens
  },
  login: async (body: { email: string; password: string }) => {
    const tokens = await request<AuthTokens>('/api/auth/login', {
      method: 'POST',
      body: JSON.stringify(body),
    }, false)
    storeTokens(tokens)
    return tokens
  },
  refresh: (body: { refreshToken: string }) =>
    request<AuthTokens>('/api/auth/refresh', { method: 'POST', body: JSON.stringify(body) }, false),
  logout: async () => {
    const refreshToken = getRefreshToken()
    if (refreshToken) {
      try {
        await request<void>('/api/auth/logout', {
          method: 'POST',
          body: JSON.stringify({ refreshToken }),
        }, false)
      } catch {
        /* ignore */
      }
    }
    clearSession()
  },
  me: () => request<{ user: User; organizations: OrgMembership[] }>('/api/me'),
  listOrgs: () => request<Org[]>('/api/orgs'),
  createOrg: (body: { name: string }) =>
    request<Org>('/api/orgs', { method: 'POST', body: JSON.stringify(body) }),
  listOrgMembers: (orgId: string) =>
    request<{ userId: string; email: string; displayName: string; role: string }[]>(
      `/api/orgs/${orgId}/members`,
    ),
  addOrgMember: (orgId: string, body: { email: string; role: 'ADMIN' | 'MEMBER' }) =>
    request<{ userId: string; email: string; displayName: string; role: string }>(
      `/api/orgs/${orgId}/members`,
      { method: 'POST', body: JSON.stringify(body) },
    ),
  removeOrgMember: (orgId: string, userId: string) =>
    request<void>(`/api/orgs/${orgId}/members/${userId}`, { method: 'DELETE' }),
  listOrgProjects: (orgId: string) => request<Project[]>(`/api/orgs/${orgId}/projects`),
  createOrgProject: (
    orgId: string,
    body: { name: string; description?: string; visibility?: 'ORG' | 'RESTRICTED' },
  ) =>
    request<Project>(`/api/orgs/${orgId}/projects`, { method: 'POST', body: JSON.stringify(body) }),
  updateProject: (
    projectId: string,
    body: { name?: string; description?: string; visibility?: 'ORG' | 'RESTRICTED' },
  ) =>
    request<Project>(`/api/projects/${projectId}`, { method: 'PATCH', body: JSON.stringify(body) }),
  listGroups: (orgId: string) => request<Group[]>(`/api/orgs/${orgId}/groups`),
  createGroup: (orgId: string, name: string) =>
    request<Group>(`/api/orgs/${orgId}/groups`, { method: 'POST', body: JSON.stringify({ name }) }),
  replaceProjectAccess: (
    projectId: string,
    principals: { type: 'USER' | 'GROUP'; id: string; permission: 'READ' | 'WRITE' }[],
  ) =>
    request(`/api/projects/${projectId}/access`, {
      method: 'PUT',
      body: JSON.stringify({ principals }),
    }),
  listTags: (orgId: string) => request<Tag[]>(`/api/orgs/${orgId}/tags`),
  createTag: (orgId: string, body: { name: string; color?: string }) =>
    request<Tag>(`/api/orgs/${orgId}/tags`, { method: 'POST', body: JSON.stringify(body) }),
  setFeatureTags: (featureId: string, tagIds: string[]) =>
    request<Tag[]>(`/api/features/${featureId}/tags`, {
      method: 'PUT',
      body: JSON.stringify({ tagIds }),
    }),
  board: (projectId: string, tagId?: string) =>
    request<Board>(
      `/api/projects/${projectId}/board${tagId ? `?tagId=${encodeURIComponent(tagId)}` : ''}`,
    ),
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
  addRoadmapItem: (
    projectId: string,
    body: { title: string; summary?: string; sequenceIndex?: number; linkedFeatureId?: string },
  ) =>
    request<RoadmapItem>(`/api/projects/${projectId}/roadmap`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  createTask: (
    projectId: string,
    body: {
      featureId?: string
      title: string
      description?: string
      status?: string
      estimatedDeliveryDate?: string
      changeMessage?: string
    },
  ) =>
    request<TaskSummary>(`/api/projects/${projectId}/tasks`, { method: 'POST', body: JSON.stringify(body) }),
  getTask: (id: string) =>
    request<
      TaskSummary & {
        currentContent: {
          title?: string
          description?: string
          status?: string
          estimatedDeliveryDate?: string
        }
      }
    >(`/api/tasks/${id}`),
  publishTask: (
    id: string,
    body: {
      title: string
      description?: string
      status: string
      estimatedDeliveryDate?: string
      changeMessage: string
    },
  ) =>
    request<TaskSummary>(`/api/tasks/${id}/revisions`, { method: 'POST', body: JSON.stringify(body) }),
  placeTask: (id: string, body: { status: string; sortOrder: number }) =>
    request<TaskSummary>(`/api/tasks/${id}/placement`, {
      method: 'PUT',
      body: JSON.stringify(body),
    }),
  taskHistory: (id: string) => request<Revision[]>(`/api/tasks/${id}/revisions`),
  taskComments: (id: string) => request<Comment[]>(`/api/tasks/${id}/comments`),
  addTaskComment: (id: string, body: string) =>
    request<Comment>(`/api/tasks/${id}/comments`, { method: 'POST', body: JSON.stringify({ body }) }),
  setTaskTags: (taskId: string, tagIds: string[]) =>
    request<Tag[]>(`/api/tasks/${taskId}/tags`, {
      method: 'PUT',
      body: JSON.stringify({ tagIds }),
    }),
  listApiKeys: (orgId: string) =>
    request<ApiKeyInfo[]>(`/api/orgs/${orgId}/api-keys`),
  createApiKey: (orgId: string, body: { name: string; projectId?: string }) =>
    request<ApiKeyCreated>(`/api/orgs/${orgId}/api-keys`, {
      method: 'POST',
      body: JSON.stringify(body),
    }),
  revokeApiKey: (orgId: string, keyId: string) =>
    request<void>(`/api/orgs/${orgId}/api-keys/${keyId}`, { method: 'DELETE' }),
}

export type ApiKeyInfo = {
  id: string
  organizationId: string
  projectId?: string
  name: string
  keyPrefix: string
  createdAt: string
}

export type ApiKeyCreated = ApiKeyInfo & { apiKey: string }
