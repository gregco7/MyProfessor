import { useEffect, useState } from 'react';
import { api, ApiError } from './api';
import { Markdown } from './Markdown';
import { Nodemap } from './Nodemap';
import { Quiz, emptyDraft, toAnswers, type Draft } from './Quiz';
import { Review } from './Review';
import type { AttemptResult, LessonView, ReviewView, SessionView } from './types';

type Mode = 'read' | 'test' | 'review';

const when = (iso: string) =>
  new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });

/**
 * One session is one screen: the learning body on the left, the nodemap pinned
 * on the right. Clicking a node swaps the left pane and leaves the map in
 * place, so the learner never loses the shape of the course.
 *
 * The left pane has three modes and the quiz is reached from the bottom of the
 * reading rather than from a route of its own.
 */
export function Workspace({ session, onBack }: { session: SessionView; onBack: () => void }) {
  const firstOpen = session.nodes.find((n) => n.prerequisiteIds.length === 0) ?? session.nodes[0];
  const [nodeId, setNodeId] = useState<string | null>(firstOpen?.nodeId ?? null);
  const [mode, setMode] = useState<Mode>('read');

  const [lesson, setLesson] = useState<LessonView | null>(null);
  const [loading, setLoading] = useState(false);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [draft, setDraft] = useState<Draft>(emptyDraft());
  const [submitting, setSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState<string | null>(null);
  const [result, setResult] = useState<AttemptResult | null>(null);
  const [review, setReview] = useState<ReviewView | null>(null);

  // Nodes carry their standing on the session payload, and an attempt changes
  // it. Rather than refetching the whole session, the one node that moved is
  // patched from the attempt result.
  const [nodes, setNodes] = useState(session.nodes);
  useEffect(() => setNodes(session.nodes), [session]);

  useEffect(() => {
    if (!nodeId) return;
    let live = true;
    setLoading(true);
    setLoadError(null);
    setLesson(null);
    setDraft(emptyDraft());
    setResult(null);
    setReview(null);
    api
      .lesson(nodeId)
      .then((l) => live && setLesson(l))
      .catch((e: ApiError) => live && setLoadError(e.message))
      .finally(() => live && setLoading(false));
    return () => {
      live = false;
    };
  }, [nodeId]);

  const selected = nodes.find((n) => n.nodeId === nodeId) ?? null;
  const passedCount = nodes.filter((n) => n.passed).length;

  const submit = async () => {
    if (!lesson || !nodeId) return;
    setSubmitting(true);
    setSubmitError(null);
    try {
      // The server marks the attempt; the client never decides what is correct.
      const attempt = await api.attempt(nodeId, toAnswers(lesson.questions, draft));
      // The key only ever arrives from the review call, and only after an
      // attempt exists — which is why this is fetched second, not prefetched.
      const reviewed = await api.review(nodeId);
      setResult(attempt);
      setReview(reviewed);
      setNodes((prev) =>
        prev.map((n) =>
          n.nodeId === nodeId
            ? { ...n, passed: attempt.passed, lastAttemptedTime: attempt.lastAttemptedTime }
            : n,
        ),
      );
      setMode('review');
    } catch (e) {
      setSubmitError(e instanceof ApiError ? e.message : 'Something went wrong submitting that.');
    } finally {
      setSubmitting(false);
    }
  };

  const questions = lesson?.questions ?? [];
  const mcCount = questions.filter((q) => q.type === 'MULTIPLE_CHOICE').length;

  return (
    <div className="wrap" style={{ padding: '22px 30px 90px' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, marginBottom: 20, flexWrap: 'wrap' }}>
        <button
          onClick={onBack}
          style={{
            fontFamily: 'var(--mono)',
            fontSize: 12,
            color: 'var(--dim)',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: 0,
          }}
        >
          ← sessions
        </button>
        <span style={{ fontFamily: 'var(--mono)', fontSize: 12, color: '#3d3d3d' }}>/</span>
        <span style={{ fontFamily: 'var(--mono)', fontSize: 12, color: 'var(--dim)' }}>{session.topic}</span>
        <span className="meta" style={{ marginLeft: 'auto', fontSize: 11 }}>
          {passedCount} of {nodes.length} nodes passed
        </span>
      </div>

      <div style={{ display: 'flex', flexWrap: 'wrap', gap: 22, alignItems: 'flex-start' }}>
        <div className="card" style={{ flex: '1 1 420px', minWidth: 0 }}>
          <div className="panel-head" style={{ padding: '16px 26px' }}>
            <span className="eyebrow">
              {mode === 'read' ? 'Learning body' : mode === 'test' ? 'Quiz' : 'Review'}
            </span>
            <span className="meta" style={{ marginLeft: 'auto' }}>
              {mode === 'read'
                ? 'markdown · latex · code'
                : mode === 'test'
                  ? `${mcCount} multiple choice · ${questions.length - mcCount} written`
                  : 'answer key'}
            </span>
          </div>

          {loading && (
            <div style={{ padding: '40px 34px', color: 'var(--dim)', fontFamily: 'var(--mono)', fontSize: 12 }}>
              loading the lesson…
            </div>
          )}

          {loadError && (
            <div style={{ padding: '34px', color: '#e7bdbd', fontSize: 14, lineHeight: 1.7 }}>{loadError}</div>
          )}

          {!loading && !loadError && lesson && mode === 'read' && (
            <div style={{ padding: '30px 34px 34px' }}>
              <h1
                style={{
                  fontSize: 27,
                  fontWeight: 600,
                  letterSpacing: '-0.02em',
                  lineHeight: 1.18,
                  margin: '0 0 6px',
                  maxWidth: '26ch',
                }}
              >
                {lesson.subtopic}
              </h1>
              <div style={{ fontFamily: 'var(--mono)', fontSize: 11, color: '#a1a1a1', marginBottom: 30 }}>
                {lesson.sections.length} sections ·{' '}
                {lesson.lastAttemptedTime ? `last sat ${when(lesson.lastAttemptedTime)}` : 'not sat'}
              </div>
              <div style={{ maxWidth: '70ch', display: 'flex', flexDirection: 'column', gap: 32 }}>
                {lesson.sections.map((s) => (
                  <Markdown key={s.learnId}>{s.body}</Markdown>
                ))}
              </div>
              <div
                style={{
                  marginTop: 42,
                  paddingTop: 26,
                  borderTop: '1px solid var(--line)',
                  display: 'flex',
                  alignItems: 'center',
                  gap: 16,
                  flexWrap: 'wrap',
                }}
              >
                <button className="btn" onClick={() => setMode('test')}>
                  Quiz
                </button>
                <span className="meta" style={{ fontSize: 11 }}>
                  {questions.length} questions · passes at {lesson.passRequirement}
                </span>
                {result && (
                  <button className="btn-quiet" onClick={() => setMode('review')}>
                    back to the review
                  </button>
                )}
              </div>
            </div>
          )}

          {!loading && !loadError && lesson && mode === 'test' && (
            <Quiz
              title={lesson.subtopic}
              questions={lesson.questions}
              allowHints
              draft={draft}
              setDraft={setDraft}
              onBack={() => setMode('read')}
              onSubmit={submit}
              submitting={submitting}
              submitLabel="submit attempt"
              writtenNote="stored, shown back in the review, not marked"
              error={submitError}
            />
          )}

          {mode === 'review' && review && result && (
            <Review review={review} result={result} onBack={() => setMode('read')} />
          )}
        </div>

        <div
          style={{
            flex: '1 1 360px',
            maxWidth: 460,
            minWidth: 0,
            position: 'sticky',
            top: 22,
            display: 'flex',
            flexDirection: 'column',
            gap: 16,
          }}
        >
          <Nodemap
            nodes={nodes}
            selectedId={nodeId}
            onOpen={(id) => {
              setNodeId(id);
              setMode('read');
            }}
          />
          {selected && mode !== 'test' && (
            <div className="panel" style={{ padding: '16px 20px' }}>
              <div style={{ fontSize: 13, lineHeight: 1.6, color: 'var(--dim)' }}>
                <span style={{ color: 'var(--fg)', fontWeight: 500 }}>{selected.subtopic}</span>
                <br />
                {selected.numSections} sections · {selected.numQuestions} questions · passes at{' '}
                {selected.passRequirement}
                <br />
                {selected.lastAttemptedTime
                  ? `last sat ${when(selected.lastAttemptedTime)}`
                  : 'not sat yet'}
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
