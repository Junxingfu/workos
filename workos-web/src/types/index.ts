export type ItemType = 'TASK' | 'NOTE'
export type ItemStatus = 'INBOX' | 'TODO' | 'DOING' | 'WAITING' | 'DONE' | 'CANCELLED'
export type ItemPriority = 'P0' | 'P1' | 'P2' | 'P3'
export type FollowUpOutcome = 'STILL_WAITING' | 'RESOLVED' | 'ABANDONED'

export interface ApiResponse<T> {
  code: number
  message: string
  data: T
  traceId?: string
  messageArray?: string[]
}

export interface BasePageResult<T> {
  total: number
  pageNo: number
  pageSize: number
  dataList: T[]
}

export interface Item {
  id: string
  title: string
  type: ItemType
  status: ItemStatus
  priority: ItemPriority
  projectId?: string | null
  projectName?: string | null
  description?: string | null
  plannedDate?: string | null
  dueAt?: string | null
  nextFollowUpAt?: string | null
  waitingFor?: string | null
  waitingSince?: string | null
  waitingExpectedAt?: string | null
  waitingDays?: number | null
  relatedPerson?: string | null
  completedAt?: string | null
  remainingSteps?: number | null
  lastLogContent?: string | null
  lastLogAt?: string | null
  lastActivityType?: string | null
  overdue?: boolean | null
  followUpDue?: boolean | null
  allowedTransitions?: ItemStatus[]
  activities?: ItemActivity[]
  createdAt: string
  updatedAt: string
  version: number
}

export interface ItemActivity {
  id: string
  itemId: string
  activityType: string
  fromStatus?: string | null
  toStatus?: string | null
  remark?: string | null
  actor: string
  occurredAt: string
}

export interface TodaySummary {
  plan: number
  overdue: number
  today: number
  followUp: number
  waiting: number
  suggested: number
  inbox: number
  unclassified: number
  carriedOverPlan: number
}

export interface TodaySections {
  plan: Item[]
  overdue: Item[]
  today: Item[]
  followUp: Item[]
  waiting: Item[]
  suggested: Item[]
}

export interface TodayResp {
  date: string
  summary: TodaySummary
  sections: TodaySections
}

export interface InboxResp {
  records: Item[]
  total: number
  oldestDays: number
}

export interface ProjectContext {
  goal?: string | null
  currentFocus?: string | null
  progressNote?: string | null
  blocker?: string | null
  nextAction?: string | null
  keyPeople?: string | null
  keyLinks?: string | null
  notesMarkdown?: string | null
  updatedAt?: string | null
  staleDays?: number | null
}

export interface ProjectStatistics {
  todo: number
  doing: number
  waiting: number
  done: number
}

export interface Project {
  id: string
  name: string
  description?: string | null
  status: 'ACTIVE' | 'ARCHIVED'
  doingCount?: number | null
  waitingCount?: number | null
  lastActivityAt?: string | null
  statistics?: ProjectStatistics
  context?: ProjectContext
  createdAt: string
  updatedAt: string
  version: number
}

export interface Daily {
  id: string
  date: string
  content: string
  generated: boolean
  generatedContent?: string | null
  createdAt: string
  updatedAt: string
  version: number
}

export interface WorkLog {
  id: string
  itemId?: string | null
  projectId?: string | null
  content: string
  workDate: string
  occurredAt: string
  createdAt: string
}

export interface Settings {
  timezone: string
  dateFormat: string
  weekStart: string
  dayBoundaryHour: number
  morningBriefTime: string
  eodReviewTime: string
  desktopNotification: boolean
  quickCaptureShortcut: string
}
