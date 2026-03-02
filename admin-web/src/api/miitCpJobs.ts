import { requestJson } from './client'
import type { Page } from './types'
import type { JobRun } from './jobRuns'

export interface MiitCpJobCreateRequest {
  pcFrom: number
  pcTo: number
  qymc?: string | null
  clxh?: string | null
  clmc?: string | null
  cpsbList?: string[] | null
  pageSize?: number | null
  limit?: number | null
  headful?: boolean | null
}

export async function createMiitCpJob(payload: MiitCpJobCreateRequest): Promise<JobRun> {
  return requestJson<JobRun>('POST', '/api/admin/miit-cp-jobs', payload)
}

export async function listMiitCpJobs(page: number, size: number): Promise<Page<JobRun>> {
  const params = new URLSearchParams()
  params.set('page', String(page))
  params.set('size', String(size))
  return requestJson<Page<JobRun>>('GET', `/api/admin/miit-cp-jobs?${params.toString()}`)
}

export async function getMiitCpJob(runId: string): Promise<JobRun> {
  return requestJson<JobRun>('GET', `/api/admin/miit-cp-jobs/${encodeURIComponent(runId)}`)
}

