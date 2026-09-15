import { useState } from 'react';
import { Markdown, MarkdownInline } from './Markdown';
import type { QuestionType, SubmittedAnswer } from './types';

/**
 * The shape both a lesson's questions and a diagnostic's questions satisfy.
 *
 * A probe question is a lesson question asked at a different moment — same
 * table, same row shape — so it is sat through the same component. The hints
 * are optional because a diagnostic does not carry any: a hint would measure
 * the hint rather than the learner, so the API does not send them.
 */
export interface QuizQuestion {
  questionId: string;
  ordinal: number;
  type: QuestionType;
  body: string;
  multiSelect: boolean | null;
  choices: { choiceId: string; body: string }[];
  hint1?: string;
  hint2?: string;
}

export interface Draft {
  choices: Record<string, string[]>;
  written: Record<string, string>;
}

export const emptyDraft = (): Draft => ({ choices: {}, written: {} });

/**
 * Turns the draft into the request body.
 *
 * A question the learner left alone is omitted entirely rather than sent with
 * empty fields — blank is allowed, and the API counts an absent question as
 * unanswered. Only one of the two keys is ever sent for a given question.
 */
export function toAnswers(questions: QuizQuestion[], draft: Draft): SubmittedAnswer[] {
  const out: SubmittedAnswer[] = [];
  for (const q of questions) {
    if (q.type === 'WRITTEN') {
      const written = (draft.written[q.questionId] ?? '').trim();
      if (written) out.push({ questionId: q.questionId, written });
    } else {
      const choiceIds = draft.choices[q.questionId] ?? [];
      if (choiceIds.length) out.push({ questionId: q.questionId, choiceIds });
    }
  }
  return out;
}

export function Quiz({
  title,
  questions,
  allowHints,
  draft,
  setDraft,
  onBack,
  onSubmit,
  submitting,
  submitLabel,
  writtenNote,
  error,
}: {
  /** omitted where the surrounding page already names the thing being sat */
  title?: string;
  questions: QuizQuestion[];
  /** false for a diagnostic, which must not nudge */
  allowHints: boolean;
  draft: Draft;
  setDraft: (next: Draft) => void;
  onBack?: () => void;
  onSubmit: () => void;
  submitting: boolean;
  submitLabel: string;
  /** what becomes of a written answer here — it differs between the two uses */
  writtenNote: string;
  error: string | null;
}) {
  const [index, setIndex] = useState(0);
  // Hints are revealed one at a time and only on request. The lesson ships them
  // with the questions, so this is purely a matter of not showing them.
  const [hints, setHints] = useState<Record<string, number>>({});

  const q = questions[Math.min(index, questions.length - 1)];
  if (!q) return null;

  const shown = hints[q.questionId] ?? 0;
  const picked = draft.choices[q.questionId] ?? [];
  const answered = questions.filter(
    (x) => (draft.choices[x.questionId]?.length ?? 0) > 0 || (draft.written[x.questionId] ?? '').trim(),
  ).length;

  const pick = (choiceId: string) => {
    const current = draft.choices[q.questionId] ?? [];
    const next = q.multiSelect
      ? current.includes(choiceId)
        ? current.filter((c) => c !== choiceId)
        : [...current, choiceId]
      : [choiceId];
    setDraft({ ...draft, choices: { ...draft.choices, [q.questionId]: next } });
  };

  return (
    <div style={{ padding: '26px 34px 30px', display: 'flex', flexDirection: 'column', minHeight: 460 }}>
      {(title || onBack) && (
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 18, marginBottom: 24 }}>
        {title && (
          <h1
            style={{
              flex: 1,
              minWidth: 0,
              fontSize: 19,
              fontWeight: 600,
              letterSpacing: '-0.02em',
              margin: 0,
              lineHeight: 1.3,
            }}
          >
            {title}
          </h1>
        )}
        {onBack && (
          <button
            onClick={onBack}
            style={{
              flex: 'none',
              fontFamily: 'var(--mono)',
              fontSize: 12,
              color: 'var(--dim)',
              background: 'none',
              border: 'none',
              cursor: 'pointer',
              whiteSpace: 'nowrap',
            }}
          >
            ← back
          </button>
        )}
      </div>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 16 }}>
        <span className="eyebrow" style={{ letterSpacing: '0.12em' }}>
          q{String(q.ordinal).padStart(2, '0')}
        </span>
        <span className="meta" style={{ marginLeft: 'auto' }}>
          {q.type === 'WRITTEN' ? 'written' : q.multiSelect ? 'select all' : 'multiple choice'}
        </span>
      </div>

      <Markdown>{q.body}</Markdown>

      {q.type === 'MULTIPLE_CHOICE' ? (
        <>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 9, marginTop: 18 }}>
            {q.choices.map((c) => {
              const on = picked.includes(c.choiceId);
              return (
                <button
                  key={c.choiceId}
                  className="choice"
                  onClick={() => pick(c.choiceId)}
                  style={{
                    border: `1px solid ${on ? '#2f6d56' : '#262626'}`,
                    background: on ? 'rgba(0,187,127,.06)' : 'transparent',
                  }}
                >
                  <span
                    style={{
                      width: 13,
                      height: 13,
                      flex: 'none',
                      marginTop: 4,
                      borderRadius: q.multiSelect ? 3 : '50%',
                      border: `1px solid ${on ? '#00bb7f' : '#525252'}`,
                      background: on ? '#00bb7f' : 'transparent',
                      boxShadow: on ? '0 0 10px rgba(0,187,127,.7)' : 'none',
                    }}
                  />
                  <span style={{ flex: 1 }}>
                    <MarkdownInline>{c.body}</MarkdownInline>
                  </span>
                </button>
              );
            })}
          </div>
          <div className="meta" style={{ marginTop: 12, fontSize: 11 }}>
            {q.multiSelect ? 'more than one answer is correct' : 'one answer only'}
          </div>
        </>
      ) : (
        <>
          <textarea
            placeholder="Write your answer"
            value={draft.written[q.questionId] ?? ''}
            onChange={(e) =>
              setDraft({ ...draft, written: { ...draft.written, [q.questionId]: e.target.value } })
            }
            style={{
              width: '100%',
              boxSizing: 'border-box',
              marginTop: 18,
              minHeight: 150,
              resize: 'vertical',
              padding: '13px 15px',
              borderRadius: 'var(--r)',
              border: '1px solid var(--line)',
              background: '#0d0d0d',
              color: 'var(--fg)',
              fontFamily: 'var(--sans)',
              fontSize: 14.5,
              lineHeight: 1.6,
              caretColor: 'var(--em)',
            }}
          />
          <div className="meta" style={{ marginTop: 9, fontSize: 11 }}>
            {writtenNote}
          </div>
        </>
      )}

      <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginTop: 18, flexWrap: 'wrap' }}>
        {allowHints && shown < 2 && (
          <button
            className="btn-quiet"
            onClick={() => setHints({ ...hints, [q.questionId]: shown + 1 })}
          >
            {shown === 0 ? 'show a hint' : 'show the second hint'}
          </button>
        )}
      </div>

      {allowHints && shown >= 1 && q.hint1 && <Hint label="hint 1" text={q.hint1} />}
      {allowHints && shown >= 2 && q.hint2 && <Hint label="hint 2" text={q.hint2} />}

      {error && (
        <div
          style={{
            marginTop: 16,
            padding: '11px 14px',
            borderRadius: 'var(--r)',
            border: '1px solid #5a2f2f',
            background: 'rgba(120,40,40,.12)',
            fontSize: 13.5,
            lineHeight: 1.6,
            color: '#e7bdbd',
          }}
        >
          {error}
        </div>
      )}

      <div style={{ marginTop: 'auto', paddingTop: 28, display: 'flex', alignItems: 'center', gap: 18 }}>
        <span className="meta" style={{ flex: 1, fontSize: 11 }}>
          {answered} of {questions.length} answered · blank is allowed
        </span>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14 }}>
          <button
            className="btn-quiet"
            style={{ width: 30, height: 30, padding: 0, borderRadius: 8 }}
            disabled={index === 0}
            onClick={() => setIndex((i) => Math.max(0, i - 1))}
          >
            ‹
          </button>
          <span
            style={{
              fontFamily: 'var(--mono)',
              fontSize: 12.5,
              color: 'var(--fg)',
              minWidth: 52,
              textAlign: 'center',
            }}
          >
            {index + 1} / {questions.length}
          </span>
          <button
            className="btn-quiet"
            style={{ width: 30, height: 30, padding: 0, borderRadius: 8 }}
            disabled={index >= questions.length - 1}
            onClick={() => setIndex((i) => Math.min(questions.length - 1, i + 1))}
          >
            ›
          </button>
        </div>
        <div style={{ flex: 1, display: 'flex', justifyContent: 'flex-end' }}>
          <button
            className="btn"
            style={{ fontSize: 12.5, padding: '10px 18px' }}
            onClick={onSubmit}
            disabled={submitting}
          >
            {submitting ? 'submitting…' : submitLabel}
          </button>
        </div>
      </div>
    </div>
  );
}

function Hint({ label, text }: { label: string; text: string }) {
  return (
    <div style={{ marginTop: 13, paddingLeft: 14, borderLeft: '2px solid var(--em)' }}>
      <div
        style={{
          fontFamily: 'var(--mono)',
          fontSize: 10,
          letterSpacing: '0.12em',
          textTransform: 'uppercase',
          color: 'var(--em)',
          marginBottom: 5,
        }}
      >
        {label}
      </div>
      <div style={{ fontSize: 13.5, lineHeight: 1.65, color: 'var(--dim)' }}>
        <MarkdownInline>{text}</MarkdownInline>
      </div>
    </div>
  );
}
