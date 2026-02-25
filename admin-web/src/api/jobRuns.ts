import { requestJson } from './client'
import type { Page } from './types'

export interface JobRun {
  id: number
  runId: string
  jobType: string
  status: string
  startedAt: string
  finishedAt?: string | null
  actorUserId?: number | null
  actorName?: string | null
  insertedCount?: number | null
  updatedCount?: number | null
  skippedCount?: number | null
  message?: string | null
  detailsJson?: string | null
}

export async function listJobRuns(jobType: string, page: number, size: number): Promise<Page<JobRun>> {
  const params = new URLSearchParams()
  params.set('jobType', jobType)
  params.set('page', String(page))
  params.set('size', String(size))
  return requestJson<Page<JobRun>>('GET', `/api/admin/job-runs?${params.toString()}`)
}

export async function runMaterialPriceNow(): Promise<JobRun> {
  return requestJson<JobRun>('POST', '/api/admin/job-runs/material-price/run-now')
}
