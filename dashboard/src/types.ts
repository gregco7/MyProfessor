/**
 * The wire contract, exactly as the API sends it.
 *
 * Nothing here is invented. If a screen wants a field that is not on one of
 * these types, the field does not exist — ask for it on the API rather than
 * deriving it or filling the space. The two things the dashboard works out for
 * itself, availability and tier depth, are computed in `graph.ts` from
 * `prerequisiteIds` and are deliberately not added to these types.
 */

export type QuestionType = 'MULTIPLE_CHOICE' | 'WRITTEN';

/** Markdown with `$ … $` and `$$ … $$` maths and fenced code. The only value sent. */
export type ContentFormat = 'MARKDOWN_LATEX';

/** GET /api/sessions */
export interface SessionCard {
  sessionId: string;
  topic: string;
  lvl: 1 | 2 | 3 | 4;
  proficiency: string;
  numNodes: number;
  /** false = planned from the topic alone, so it starts at the beginning of it */
  probed: boolean;
  createdAt: string;
}

/** GET /api/sessions/{sessionId} — the tree's shape, with no lesson content. */
export interface SessionView {
  sessionId: string;
  topic: string;
  lvl: 1 | 2 | 3 | 4;
  proficiency: string;
  /** Claude's reading of the learner; null when no diagnostic was sat. */
  assessment: string | null;
  probeId: string | null;
  createdAt: string;
  nodes: NodeCard[];
}

export interface NodeCard {
  nodeId: string;
  subtopic: string;
  passRequirement: number;
  passed: boolean;
  /** null until the lesson has been sat at least once */
  lastAttemptedTime: string | null;
  numSections: number;
  numQuestions: number;
  /** Other nodeIds in this same session. A DAG, not a chain. */
  prerequisiteIds: string[];
}

/** GET /api/nodes/{nodeId} — hints included, answer key withheld. */
export interface LessonView {
  nodeId: string;
  subtopic: string;
  passRequirement: number;
  passed: boolean;
  lastAttemptedTime: string | null;
  sections: LessonSection[];
  questions: LessonQuestion[];
}

export interface LessonSection {
  learnId: string;
  ordinal: number;
  body: string;
  format: ContentFormat;
}

export interface LessonQuestion {
  questionId: string;
  ordinal: number;
  type: QuestionType;
  body: string;
  bodyFormat: ContentFormat;
  /** null for WRITTEN; true means more than one choice is correct */
  multiSelect: boolean | null;
  hint1: string;
  hint2: string;
  choices: LessonChoice[];
}

/** No `isCorrect` — that only ever arrives from the review call. */
export interface LessonChoice {
  choiceId: string;
  body: string;
}

/** GET /api/probes/{probeId} — no hints, no key: a diagnostic must not nudge or reveal. */
export interface ProbeView {
  probeId: string;
  topic: string;
  lvl: 1 | 2 | 3 | 4;
  proficiency: string;
  questions: ProbeQuestion[];
}

export interface ProbeQuestion {
  questionId: string;
  ordinal: number;
  type: QuestionType;
  body: string;
  bodyFormat: ContentFormat;
  multiSelect: boolean | null;
  choices: LessonChoice[];
}

/** POST /api/sessions */
export interface CreateSessionRequest {
  topic: string;
  lvl: 1 | 2 | 3 | 4;
  numNodes?: number;
  probeId?: string;
  answers?: SubmittedAnswer[];
}

/** POST /api/nodes/{nodeId}/attempt */
export interface SubmittedAnswer {
  questionId: string;
  written?: string;
  choiceIds?: string[];
}

export interface AttemptResult {
  nodeId: string;
  correct: number;
  total: number;
  passRequirement: number;
  passed: boolean;
  lastAttemptedTime: string;
  outcomes: QuestionOutcome[];
}

export interface QuestionOutcome {
  questionId: string;
  ordinal: number;
  type: QuestionType;
  answered: boolean;
  /** false for a written answer or a blank one; `correct` is then meaningless */
  graded: boolean;
  correct: boolean;
}

/** GET /api/nodes/{nodeId}/review — 409 until the lesson has been attempted. */
export interface ReviewView {
  nodeId: string;
  subtopic: string;
  questions: ReviewQuestion[];
}

export interface ReviewQuestion {
  questionId: string;
  ordinal: number;
  type: QuestionType;
  body: string;
  /** prose, or comma-joined choiceIds */
  yourAnswer: string | null;
  /** null for written, which is unmarked */
  correct: boolean | null;
  rubric: string | null;
  modelAnswer: string | null;
  choices: ReviewChoice[];
}

export interface ReviewChoice {
  choiceId: string;
  body: string;
  isCorrect: boolean;
  /** Present on every choice; a wrong one names the misunderstanding behind it. */
  explanation: string | null;
}

/** RFC 9457, which every failure comes back as. */
export interface ProblemDetail {
  title: string;
  status: number;
  detail: string;
  instance?: string;
}
