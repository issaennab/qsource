import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import {
  api,
  clearSession,
  getAccessToken,
  getSelectedOrgId,
  setSelectedOrgId,
  type OrgMembership,
  type User,
} from '../api/client'

type RefreshOptions = { selectOrgId?: string }

type AuthState = {
  user: User | null
  orgs: OrgMembership[]
  orgId: string | null
  loading: boolean
  refresh: (opts?: RefreshOptions) => Promise<void>
  setOrgId: (id: string) => void
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [orgs, setOrgs] = useState<OrgMembership[]>([])
  const [orgId, setOrgIdState] = useState<string | null>(getSelectedOrgId())
  const [loading, setLoading] = useState(true)

  const refresh = useCallback(async (opts?: RefreshOptions) => {
    if (!getAccessToken()) {
      setUser(null)
      setOrgs([])
      setLoading(false)
      return
    }
    try {
      const me = await api.me()
      setUser(me.user)
      setOrgs(me.organizations)
      const preferred = opts?.selectOrgId
      const existing = getSelectedOrgId()
      const pick =
        (preferred && me.organizations.some((o) => o.organizationId === preferred) && preferred) ||
        (existing && me.organizations.some((o) => o.organizationId === existing) && existing) ||
        me.organizations[0]?.organizationId ||
        null
      if (pick) {
        setSelectedOrgId(pick)
        setOrgIdState(pick)
      } else {
        setOrgIdState(null)
      }
    } catch {
      clearSession()
      setUser(null)
      setOrgs([])
    } finally {
      setLoading(false)
    }
  }, [])

  useEffect(() => {
    void refresh()
  }, [refresh])

  const setOrgId = useCallback((id: string) => {
    setSelectedOrgId(id)
    setOrgIdState(id)
  }, [])

  const logout = useCallback(async () => {
    await api.logout()
    setUser(null)
    setOrgs([])
    setOrgIdState(null)
  }, [])

  const value = useMemo(
    () => ({ user, orgs, orgId, loading, refresh, setOrgId, logout }),
    [user, orgs, orgId, loading, refresh, setOrgId, logout],
  )

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth requires AuthProvider')
  return ctx
}
