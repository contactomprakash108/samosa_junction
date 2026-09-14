import type { ApiErrorBody, FieldErrorDetail } from '@/types/api'


export class ApiRequestError extends Error {
  readonly status: number
  readonly code: string
  readonly path: string
  readonly fieldErrors: FieldErrorDetail[]

  constructor(body: ApiErrorBody) {
    super(body.message)
    this.name = 'ApiRequestError'
    this.status = body.status
    this.code = body.code
    this.path = body.path
    this.fieldErrors = body.fieldErrors ?? []
  }
}

export function isApiRequestError(error: unknown): error is ApiRequestError {
  return error instanceof ApiRequestError
}

export function errorMessage(error: unknown, fallback = 'Something went wrong. Please try again.') {
  if (isApiRequestError(error)) {
    return error.message
  }
  if (error instanceof Error && error.message) {
    return error.message
  }
  return fallback
}
