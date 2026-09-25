import http from './client'
export { fail } from './client'
import type {
  BasePageResult,
  Daily,
  FollowUpOutcome,
  InboxResp,
  Item,
  ItemActivity,
  ItemPriority,
  ItemStatus,
  ItemType,
  Project,
  ProjectContext,
  Settings,
  TodayResp,
  WorkLog
} from '@/types'

/* ---------------- Today ---------------- */

export const fetchToday = (): Promise<TodayResp> => http.get('/today')

export const reorderPlan = (date: string, itemIds: string[]) =>
  http.post('/today/plan/reorder', { date, itemIds })

/* ---------------- Inbox ---------------- */

export const fetchInbox = (): Promise<InboxResp> => http.get('/inbox')

export const acceptAll = (itemIds?: string[]) =>
  http.post('/inbox/accept-all', itemIds?.length ? { itemIds } : {})

/* ---------------- Item ---------------- */

export interface ItemQuery {
  projectId?: string
  status?: ItemStatus
  type?: ItemType
  priority?: ItemPriority
  keyword?: string
  noDate?: boolean
  unclassified?: boolean
  followUpOnly?: boolean
  pageNo?: number
  pageSize?: number
}

export const fetchItems = (params: ItemQuery): Promise<BasePageResult<Item>> =>
  http.get('/items', { params })

export const fetchItem = (id: string): Promise<Item> => http.get(`/items/${id}`)

export const quickCapture = (content: string): Promise<string> =>
  http.post('/items/quick-capture', { content })

export const createItem = (payload: Partial<Item> & { title: string }) =>
  http.post('/items', payload)

export const updateItem = (
  id: string,
  payload: {
    version?: number
    title?: string
    description?: string
    projectId?: string | null
    itemType?: ItemType
    priority?: ItemPriority
    plannedDate?: string
    dueAt?: string
    nextFollowUpAt?: string
    relatedPerson?: string
    clearNextFollowUp?: boolean
  }
): Promise<boolean> => http.put(`/items/${id}`, payload)

export const deleteItem = (id: string): Promise<boolean> => http.delete(`/items/${id}`)

export interface TransitionPayload {
  version?: number
  toStatus: ItemStatus
  waitingFor?: string
  waitingExpectedAt?: string
  nextFollowUpAt?: string
  remark?: string
}

export const transition = (id: string, payload: TransitionPayload): Promise<Item> =>
  http.post(`/items/${id}/transitions`, payload)

export const followUpAction = (
  id: string,
  payload: {
    version?: number
    outcome: FollowUpOutcome
    note?: string
    nextFollowUpAt?: string
    toStatus?: ItemStatus
  }
): Promise<Item> => http.post(`/items/${id}/follow-up-actions`, payload)

export const planItem = (id: string, plannedDate: string, version?: number): Promise<Item> =>
  http.post(`/items/${id}/plan`, { plannedDate, version })

export const unplanItem = (id: string): Promise<boolean> => http.delete(`/items/${id}/plan`)

export const fetchActivities = (id: string): Promise<ItemActivity[]> =>
  http.get(`/items/${id}/activities`)

/* ---------------- WorkLog ---------------- */

export const createWorkLog = (payload: {
  itemId?: string
  projectId?: string
  workDate?: string
  content: string
}): Promise<string> => http.post('/work-logs', payload)

export const fetchWorkLogs = (params: {
  itemId?: string
  projectId?: string
  workDate?: string
}): Promise<WorkLog[]> => http.get('/work-logs', { params })

/* ---------------- Project ---------------- */

export const fetchProjects = (params: {
  status?: string
  keyword?: string
  pageNo?: number
  pageSize?: number
}): Promise<BasePageResult<Project>> => http.get('/projects', { params })

export const createProject = (payload: { name: string; description?: string }): Promise<string> =>
  http.post('/projects', payload)

export const fetchProject = (id: string): Promise<Project> => http.get(`/projects/${id}`)

export const archiveProject = (id: string) => http.post(`/projects/${id}/archive`)

export const restoreProject = (id: string) => http.post(`/projects/${id}/restore`)

export const updateProjectContext = (
  id: string,
  payload: Partial<ProjectContext>
): Promise<ProjectContext> => http.put(`/projects/${id}/context`, payload)

/* ---------------- Daily ---------------- */

export const fetchDaily = (date: string): Promise<Daily> => http.get(`/dailies/${date}`)

export const fetchDailies = (weekStart?: string): Promise<Daily[]> =>
  http.get('/dailies', { params: weekStart ? { weekStart } : {} })

export const updateDaily = (date: string, content: string, version?: number): Promise<Daily> =>
  http.put(`/dailies/${date}`, { content, version })

export const generateDaily = (
  date: string
): Promise<{ date: string; generatedContent: string; sourceWorkLogCount: number }> =>
  http.post(`/dailies/${date}/generate`)

export const applyGenerated = (date: string, content: string, version?: number): Promise<Daily> =>
  http.post(`/dailies/${date}/apply-generated`, { content, version })

/* ---------------- Reminder ---------------- */

export const fetchPendingReminders = (): Promise<
  Array<{ id: string; title: string; remindAt: string; itemId?: string }>
> => http.get('/reminders/pending')

export const dismissReminder = (id: string) =>
  http.post(`/reminders/${id}/dismiss`, {})

export const rescheduleReminder = (id: string, remindAt: string) =>
  http.put(`/reminders/${id}`, { remindAt })

/* ---------------- Settings ---------------- */

export const fetchSettings = (): Promise<Settings> => http.get('/settings')

export const updateSettings = (payload: Partial<Settings>): Promise<Settings> =>
  http.put('/settings', payload)
