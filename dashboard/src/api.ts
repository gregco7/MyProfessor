import type {
  AttemptResult,
  CreateSessionRequest,
  LessonView,
  ProblemDetail,
  ProbeView,
  ReviewView,
  SessionCard,
  SessionView,
  SubmittedAnswer,
} from './types';

/**
 * Paths stay relative. In production Spring serves this bundle from the same
 * origin as the API; in development Vite proxies /api to the backend, so the
 * two arrangements look identical to the code and CORS never enters into it.
 */
const BASE = '/api';

/** A failed request, carrying the server's own wording where there is any. */
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, detail: string) {
    super(detail);
    this.status = status;
    this.name = 'ApiError';
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(BASE + path, {
      ...init,
      headers: { 'Content-Type': 'application/json', ...(init?.headers ?? {}) },
    });
  } catch {
    // A dead backend is the common case in development and deserves better
    // than the browser's own "Failed to fetch".
    throw new ApiError(0, 'Could not reach the server. Is it running on port 8080?');
  }

  if (!response.ok) {
    // Every failure is RFC 9457. `detail` is written to be read by a person,
    // so it is used verbatim rather than being replaced with our own wording.
    const problem = (await response.json().catch(() => null)) as ProblemDetail | null;
    throw new ApiError(response.status, problem?.detail ?? response.statusText);
  }

  return (await response.json()) as T;
}

export const api = {
  sessions: () => request<SessionCard[]>('/sessions'),

  probe: (probeId: string) => request<ProbeView>(`/probes/${probeId}`),

  /**
   * Records the diagnostic's answers and plans the whole session from them.
   *
   * <p>Runs for minutes — one Claude call for the outline plus one per lesson.
   * No AbortController is attached on purpose: a client-side timeout here would
   * abandon work that has already been paid for.
   */
  createSession: (body: CreateSessionRequest) =>
    request<SessionView>('/sessions', { method: 'POST', body: JSON.stringify(body) }),

  session: (sessionId: string) => request<SessionView>(`/sessions/${sessionId}`),

  lesson: (nodeId: string) => request<LessonView>(`/nodes/${nodeId}`),

  attempt: (nodeId: string, answers: SubmittedAnswer[]) =>
    request<AttemptResult>(`/nodes/${nodeId}/attempt`, {
      method: 'POST',
      body: JSON.stringify({ answers }),
    }),

  /**
   * Throws ApiError with status 409 until the lesson has been attempted. That
   * is the documented resting state, not a fault — callers treat it as "the
   * review is not open yet" rather than surfacing it.
   */
  review: (nodeId: string) => request<ReviewView>(`/nodes/${nodeId}/review`),
};
