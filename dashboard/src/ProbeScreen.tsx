import { useEffect, useState } from 'react';
import { api, ApiError } from './api';
import { Quiz, emptyDraft, toAnswers, type Draft } from './Quiz';
import type { ProbeView } from './types';

/**
 * Sitting the diagnostic the CLI just wrote.
 *
 * <p>It is the same quiz component a lesson uses, because it is the same thing:
 * the same rows, asked before anything has been taught rather than after. The
 * one difference is that hints are off — the API sends none for a diagnostic,
 * and a nudge here would measure the nudge rather than the learner.
 *
 * <p>Submitting is where the session gets planned, and that takes minutes. This
 * screen owns that wait: the request has no client-side timeout, and leaving the
 * page mid-flight is guarded, because navigating away abandons work that has
 * already been paid for.
 */
export function ProbeScreen({ probeId, onDone }: { probeId: string; onDone: (sessionId: string) => void }) {
  const [probe, setProbe] = useState<ProbeView | null>(null);
  const [draft, setDraft] = useState<Draft>(emptyDraft());
  const [planning, setPlanning] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api
      .probe(probeId)
      .then(setProbe)
      .catch((e: ApiError) => setError(e.message));
  }, [probeId]);

  // There is no job id and no progress endpoint, so a refresh mid-plan loses
  // the work entirely. The browser's own warning is the only honest guard.
  useEffect(() => {
    if (!planning) return;
    const warn = (e: BeforeUnloadEvent) => e.preventDefault();
    window.addEventListener('beforeunload', warn);
    return () => window.removeEventListener('beforeunload', warn);
  }, [planning]);

  const submit = async () => {
    if (!probe) return;
    setPlanning(true);
    setError(null);
    try {
      const session = await api.createSession({
        topic: probe.topic,
        lvl: probe.lvl,
        probeId: probe.probeId,
        answers: toAnswers(probe.questions, draft),
      });
      onDone(session.sessionId);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Something went wrong planning that session.');
      setPlanning(false);
    }
  };

  if (error && !probe) {
    return (
      <div className="wrap" style={{ padding: '44px 30px' }}>
        <div style={{ color: '#e7bdbd', fontSize: 15, lineHeight: 1.7, maxWidth: '60ch' }}>{error}</div>
      </div>
    );
  }

  if (!probe) {
    return (
      <div className="wrap" style={{ padding: '44px 30px', color: 'var(--dim)', fontFamily: 'var(--mono)', fontSize: 12 }}>
        loading the diagnostic…
      </div>
    );
  }

  if (planning) return <Planning topic={probe.topic} />;

  const mc = probe.questions.filter((q) => q.type === 'MULTIPLE_CHOICE').length;

  return (
    <div className="wrap" style={{ padding: '22px 30px 90px', maxWidth: 920 }}>
      <div style={{ marginBottom: 20 }}>
        <div className="eyebrow" style={{ marginBottom: 12 }}>
          Diagnostic
        </div>
        <h1 style={{ fontSize: 30, fontWeight: 600, letterSpacing: '-0.02em', margin: '0 0 10px', lineHeight: 1.12 }}>
          {probe.topic}
        </h1>
        <p style={{ margin: 0, color: 'var(--dim)', fontSize: 15, lineHeight: 1.7, maxWidth: '62ch' }}>
          Before anything is taught, this works out where you already stand, so the session starts at
          the edge of what you know rather than at the beginning of the topic. Nothing here is marked
          in front of you and there are no hints — answer what you can and leave the rest blank.
          Blank is evidence too.
        </p>
      </div>

      <div className="card">
        <div className="panel-head" style={{ padding: '16px 26px' }}>
          <span className="eyebrow">{`aiming for ${probe.proficiency}`}</span>
          <span className="meta" style={{ marginLeft: 'auto' }}>
            {mc} multiple choice · {probe.questions.length - mc} written
          </span>
        </div>
        <Quiz
          questions={probe.questions}
          allowHints={false}
          draft={draft}
          setDraft={setDraft}
          onSubmit={submit}
          submitting={planning}
          submitLabel="submit diagnostic"
          writtenNote="read by the planner in your own words, not marked"
          error={error}
        />
      </div>
    </div>
  );
}

/**
 * The wait while the session is planned.
 *
 * <p>Deliberately static. There is no job id and no progress endpoint, so a bar
 * or a percentage would be a number this screen invented.
 */
function Planning({ topic }: { topic: string }) {
  return (
    <div className="wrap" style={{ padding: '90px 30px', maxWidth: 720 }}>
      <div className="eyebrow" style={{ marginBottom: 14 }}>
        Planning
      </div>
      <h1 style={{ fontSize: 30, fontWeight: 600, letterSpacing: '-0.02em', margin: '0 0 14px', lineHeight: 1.15 }}>
        Reading your answers.
      </h1>
      <p style={{ margin: '0 0 26px', color: 'var(--dim)', fontSize: 15, lineHeight: 1.75, maxWidth: '58ch' }}>
        Working out what you already hold on <strong style={{ color: 'var(--fg)', fontWeight: 500 }}>{topic}</strong>,
        then writing a lesson at a time from the first real gap. This takes a few minutes — one pass to
        choose the lessons and one for each lesson's material and questions.
      </p>
      <div style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
        <span className="pulse" />
        <span className="meta" style={{ fontSize: 12 }}>
          leave this tab open
        </span>
      </div>
    </div>
  );
}
