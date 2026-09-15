import { useCallback, useEffect, useState } from 'react';
import { api, ApiError } from './api';
import { ProbeScreen } from './ProbeScreen';
import { Sessions } from './Sessions';
import { Workspace } from './Workspace';
import type { SessionCard, SessionView } from './types';

/**
 * Three screens, addressed by path so the CLI can open one directly.
 *
 *   /                 the sessions list
 *   /probe/{id}       sit the diagnostic the CLI just wrote
 *   /sessions/{id}    one session's workspace
 *
 * No router library: three routes, two of which take a single id, do not earn
 * one. `DashboardWebConfig` forwards these paths to the bundle, so a reload or
 * a pasted link lands in the right place.
 */
function usePath(): [string, (next: string) => void] {
  const [path, setPath] = useState(window.location.pathname);
  useEffect(() => {
    const onPop = () => setPath(window.location.pathname);
    window.addEventListener('popstate', onPop);
    return () => window.removeEventListener('popstate', onPop);
  }, []);
  const go = useCallback((next: string) => {
    window.history.pushState({}, '', next);
    setPath(next);
  }, []);
  return [path, go];
}

export default function App() {
  const [path, go] = usePath();

  const probeId = path.startsWith('/probe/') ? path.slice('/probe/'.length) : null;
  const sessionId = path.startsWith('/sessions/') ? path.slice('/sessions/'.length) : null;

  return (
    <div className="app">
      <div className="wrap" style={{ padding: '20px 30px', display: 'flex', alignItems: 'center', gap: 26 }}>
        <button
          onClick={() => go('/')}
          style={{
            marginRight: 'auto',
            display: 'flex',
            alignItems: 'center',
            gap: 11,
            background: 'none',
            border: 'none',
            cursor: 'pointer',
            padding: 0,
            color: 'inherit',
          }}
        >
          <span
            style={{
              width: 9,
              height: 9,
              borderRadius: '50%',
              background: 'var(--em)',
              boxShadow: '0 0 12px var(--em)',
            }}
          />
          <span style={{ fontFamily: 'var(--mono)', fontSize: 14, letterSpacing: '0.02em', fontWeight: 500 }}>
            myprofessor
          </span>
        </button>
      </div>

      {probeId ? (
        <ProbeScreen probeId={probeId} onDone={(id) => go(`/sessions/${id}`)} />
      ) : sessionId ? (
        <SessionRoute sessionId={sessionId} onBack={() => go('/')} />
      ) : (
        <SessionsRoute onOpen={(id) => go(`/sessions/${id}`)} />
      )}
    </div>
  );
}

function SessionsRoute({ onOpen }: { onOpen: (sessionId: string) => void }) {
  const [sessions, setSessions] = useState<SessionCard[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api.sessions().then(setSessions).catch((e: ApiError) => setError(e.message));
  }, []);

  if (error) return <Failure message={error} />;
  if (!sessions) return <Loading />;
  return <Sessions sessions={sessions} onOpen={onOpen} />;
}

function SessionRoute({ sessionId, onBack }: { sessionId: string; onBack: () => void }) {
  const [session, setSession] = useState<SessionView | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let live = true;
    setSession(null);
    setError(null);
    api
      .session(sessionId)
      .then((s) => live && setSession(s))
      .catch((e: ApiError) => live && setError(e.message));
    return () => {
      live = false;
    };
  }, [sessionId]);

  if (error) return <Failure message={error} />;
  if (!session) return <Loading />;
  return <Workspace session={session} onBack={onBack} />;
}

function Loading() {
  return (
    <div className="wrap" style={{ padding: '44px 30px', color: 'var(--dim)', fontFamily: 'var(--mono)', fontSize: 12 }}>
      loading…
    </div>
  );
}

/** Renders the server's own `detail`, which is written to be read by a person. */
function Failure({ message }: { message: string }) {
  return (
    <div className="wrap" style={{ padding: '0 30px 20px' }}>
      <div
        style={{
          padding: '13px 16px',
          borderRadius: 'var(--r)',
          border: '1px solid #5a2f2f',
          background: 'rgba(120,40,40,.12)',
          color: '#e7bdbd',
          fontSize: 14,
          lineHeight: 1.6,
        }}
      >
        {message}
      </div>
    </div>
  );
}
