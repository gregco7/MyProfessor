import { useState } from 'react';
import { Markdown, MarkdownInline } from './Markdown';
import type { AttemptResult, ReviewQuestion, ReviewView } from './types';

/**
 * The answer key beside what the learner gave.
 *
 * The standing is reported as plain arithmetic — marked correct, not marked,
 * needed to pass — because with written answers unmarked the bar is often
 * unreachable, and a celebration or a progress ring would be papering over
 * that. Where a question is not marked it says so, and never shows a red cross.
 */
export function Review({
  review,
  result,
  onBack,
}: {
  review: ReviewView;
  result: AttemptResult;
  onBack: () => void;
}) {
  const graded = result.outcomes.filter((o) => o.graded).length;
  const notMarked = result.total - graded;
  const written = review.questions.filter((q) => q.type === 'WRITTEN').length;
  const blanks = result.outcomes.filter((o) => !o.graded && o.type !== 'WRITTEN').length;
  const markable = review.questions.length - written;
  const reachable = markable >= result.passRequirement;

  const stats = [
    { value: `${result.correct}/${result.total}`, label: 'marked correct', color: '#00d294' },
    { value: String(notMarked), label: 'not marked', color: '#fafafa' },
    { value: String(result.passRequirement), label: 'needed to pass', color: '#fafafa' },
    {
      value: result.passed ? 'pass' : 'no pass',
      label: 'standing',
      color: result.passed ? '#00d294' : '#a1a1a1',
    },
  ];

  return (
    <div style={{ padding: '26px 34px 34px' }}>
      <div style={{ display: 'flex', alignItems: 'baseline', gap: 14, marginBottom: 22, flexWrap: 'wrap' }}>
        <h1 style={{ fontSize: 23, fontWeight: 600, letterSpacing: '-0.02em', margin: 0 }}>Review</h1>
        <button
          onClick={onBack}
          style={{
            marginLeft: 'auto',
            fontFamily: 'var(--mono)',
            fontSize: 12,
            color: 'var(--dim)',
            background: 'none',
            border: 'none',
            cursor: 'pointer',
          }}
        >
          ← back to the reading
        </button>
      </div>

      <div
        style={{
          display: 'grid',
          gridTemplateColumns: 'repeat(auto-fit,minmax(120px,1fr))',
          gap: 12,
          marginBottom: 12,
        }}
      >
        {stats.map((st) => (
          <div
            key={st.label}
            style={{ padding: '16px 18px', borderRadius: 14, border: '1px solid var(--line)', background: '#111' }}
          >
            <div style={{ fontFamily: 'var(--mono)', fontSize: 22, lineHeight: 1, color: st.color }}>
              {st.value}
            </div>
            <div
              style={{
                fontFamily: 'var(--mono)',
                fontSize: 10,
                letterSpacing: '0.1em',
                textTransform: 'uppercase',
                color: '#a1a1a1',
                marginTop: 8,
              }}
            >
              {st.label}
            </div>
          </div>
        ))}
      </div>

      <p style={{ margin: '0 0 28px', fontSize: 13, color: 'var(--dim)', lineHeight: 1.65, maxWidth: '66ch' }}>
        {notMarked} of the {result.total} questions were not marked — {written} written and {blanks} left
        blank.{' '}
        {reachable
          ? 'Written answers are stored and returned here, but never counted as correct.'
          : `With ${markable} markable questions against a bar of ${result.passRequirement}, this lesson cannot currently be passed.`}
      </p>

      <div style={{ display: 'flex', flexDirection: 'column', gap: 14 }}>
        {review.questions.map((q) => (
          <ReviewItem key={q.questionId} q={q} />
        ))}
      </div>
    </div>
  );
}

function ReviewItem({ q }: { q: ReviewQuestion }) {
  const [open, setOpen] = useState(false);

  // `correct` is null for a written answer, which is unmarked. A blank multiple
  // choice comes back with no answer recorded, and is also not marked.
  const marked = q.type !== 'WRITTEN' && q.correct !== null && !!q.yourAnswer;
  const mark = marked ? (q.correct ? 'correct' : 'incorrect') : 'not marked';
  const markColor = !marked ? '#a1a1a1' : q.correct ? '#00d294' : '#a1a1a1';
  const markBorder = !marked ? '#262626' : q.correct ? '#2f6d56' : '#525252';
  const chosen = (q.yourAnswer ?? '').split(',').filter(Boolean);

  return (
    <div style={{ padding: '22px 24px', borderRadius: 14, border: '1px solid var(--line)', background: '#111' }}>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12, marginBottom: 14 }}>
        <span className="eyebrow" style={{ letterSpacing: '0.12em' }}>
          q{String(q.ordinal).padStart(2, '0')}
        </span>
        <span
          style={{
            marginLeft: 'auto',
            fontFamily: 'var(--mono)',
            fontSize: 10.5,
            letterSpacing: '0.06em',
            padding: '4px 9px',
            borderRadius: 6,
            border: `1px solid ${markBorder}`,
            color: markColor,
          }}
        >
          {mark}
        </span>
      </div>

      <Markdown>{q.body}</Markdown>

      {q.type === 'MULTIPLE_CHOICE' ? (
        <>
          <div style={{ display: 'flex', flexDirection: 'column', gap: 9, marginTop: 16 }}>
            {q.choices.map((c) => {
              const chose = chosen.includes(c.choiceId);
              return (
                <div
                  key={c.choiceId}
                  style={{
                    padding: '12px 14px',
                    borderRadius: 'var(--r)',
                    border: `1px solid ${chose ? '#2f6d56' : '#1e1e1e'}`,
                    background: chose ? 'rgba(0,187,127,.05)' : 'transparent',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 11, fontSize: 14.5, lineHeight: 1.55 }}>
                    <span
                      style={{
                        fontFamily: 'var(--mono)',
                        color: c.isCorrect ? '#00d294' : chose ? '#a1a1a1' : '#3d3d3d',
                        width: 14,
                        flex: 'none',
                        textAlign: 'center',
                      }}
                    >
                      {c.isCorrect ? '✓' : chose ? '✕' : '·'}
                    </span>
                    <span style={{ flex: 1 }}>
                      <MarkdownInline>{c.body}</MarkdownInline>
                    </span>
                    {chose && (
                      <span className="meta" style={{ fontSize: 10, whiteSpace: 'nowrap' }}>
                        your answer
                      </span>
                    )}
                  </div>
                  {/* The explanation for the option actually chosen is always
                      shown; the rest sit behind one control per question. */}
                  {(chose || open) && c.explanation && (
                    <div style={{ marginTop: 10, fontSize: 13.5, lineHeight: 1.65, color: 'var(--dim)' }}>
                      <MarkdownInline>{c.explanation}</MarkdownInline>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
          <div style={{ marginTop: 14 }}>
            <button className="btn-quiet" style={{ padding: '7px 13px' }} onClick={() => setOpen(!open)}>
              {open ? 'hide the other explanations' : 'why the other options are wrong'}
            </button>
          </div>
        </>
      ) : (
        <>
          <div
            style={{
              display: 'grid',
              gridTemplateColumns: 'repeat(auto-fit,minmax(200px,1fr))',
              gap: 12,
              marginTop: 16,
            }}
          >
            <Panel label="your answer" text={q.yourAnswer || 'Left blank.'} />
            <Panel label="what a full answer covers" text={q.rubric} />
            <Panel label="model answer" text={q.modelAnswer} />
          </div>
          <div className="meta" style={{ marginTop: 13, fontSize: 11 }}>
            not marked — judge it against the rubric yourself
          </div>
        </>
      )}
    </div>
  );
}

function Panel({ label, text }: { label: string; text: string | null }) {
  return (
    <div
      style={{
        padding: '14px 16px',
        borderRadius: 12,
        border: '1px solid var(--line)',
        background: '#0d0d0d',
        minWidth: 0,
      }}
    >
      <div
        style={{
          fontFamily: 'var(--mono)',
          fontSize: 10,
          letterSpacing: '0.1em',
          textTransform: 'uppercase',
          color: '#a1a1a1',
          marginBottom: 9,
        }}
      >
        {label}
      </div>
      <div style={{ fontSize: 13.5, lineHeight: 1.65, color: '#dcdcdc' }}>
        <MarkdownInline>{text}</MarkdownInline>
      </div>
    </div>
  );
}
