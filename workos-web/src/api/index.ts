import http from './client'
export interface DesktopStorageInfo { directory: string; example: boolean; canImport: boolean; counts: Record<string, number>; backups: string[] }
export const fetchDesktopStorage = () => http.get<DesktopStorageInfo>('/desktop/storage')
export const backupDesktop = () => http.post<string>('/desktop/backup', {}, { timeout: 120000 })
export const importDesktop = (database: string) => http.post<Record<string, number>>('/desktop/import', { database }, { timeout: 120000 })
export const restoreDesktop = (name: string) => http.post<Record<string, number>>('/desktop/restore', { name }, { timeout: 120000 })
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

// 后端的 Long 序列化为字符串；ID 保留字符串，数量在请求边界转为数字。
function numericPage<T>(page: BasePageResult<T>): BasePageResult<T> {
  return { ...page, total: Number(page.total), pageNo: Number(page.pageNo), pageSize: Number(page.pageSize) }
}
function numericItem(item: Item): Item {
  return { ...item, waitingDays: item.waitingDays == null ? null : Number(item.waitingDays) }
}

/* ---------------- Today ---------------- */

export const fetchToday = async (): Promise<TodayResp> => {
  const result = await http.get<TodayResp>('/today')
  for (const key of Object.keys(result.sections) as Array<keyof TodayResp['sections']>) {
    result.sections[key] = result.sections[key].map(numericItem)
  }
  return result
}

export const reorderPlan = (date: string, itemIds: string[]) =>
  http.post('/today/plan/reorder', { date, itemIds })

/* ---------------- Inbox ---------------- */

export const fetchInbox = async (): Promise<InboxResp> => {
  const result = await http.get<InboxResp>('/inbox')
  return { ...result, total: Number(result.total), oldestDays: Number(result.oldestDays), records: result.records.map(numericItem) }
}

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

export const fetchItems = async (params: ItemQuery): Promise<BasePageResult<Item>> => {
  const result = numericPage(await http.get<BasePageResult<Item>>('/items', { params }))
  return { ...result, dataList: result.dataList.map(numericItem) }
}

export const fetchItem = async (id: string): Promise<Item> => numericItem(await http.get<Item>(`/items/${id}`))

export const quickCapture = (content: string, destination: 'INBOX' | 'TODO' | 'TODAY' = 'INBOX', projectId?: string): Promise<string> =>
  http.post('/items/quick-capture', { content, destination, projectId })

export const createItem = (payload: Partial<Item> & { title: string }) =>
  http.post('/items', payload)

export const updateItem = (
  id: string,
  payload: {
    version?: number
    title?: string
    description?: string
    projectId?: string | null
    relatedProjectIds?: string[]
    itemType?: ItemType
    priority?: ItemPriority
    plannedDate?: string
    dueAt?: string
    nextFollowUpAt?: string
    relatedPerson?: string
    clearNextFollowUp?: boolean
    clearProject?: boolean
    clearDueAt?: boolean
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

export const planToday = (id: string, version: number): Promise<Item> =>
  http.post(`/items/${id}/plan`, { version })

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
}): Promise<BasePageResult<Project>> => http.get<BasePageResult<Project>>('/projects', { params }).then(numericPage)

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
): Promise<{ date: string; generatedContent: string; sourceWorkLogCount: number; version: number }> =>
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
