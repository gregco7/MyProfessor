import type { SessionCard } from './types';

const when = (iso: string) =>
  new Date(iso).toLocaleDateString('en-GB', { day: 'numeric', month: 'short', year: 'numeric' });

/**
 * The list, newest first.
 *
 * The API sends no per-session count of lessons passed, so no progress bar is
 * drawn here — a bar would have to invent its own number. The node count and
 * the date are what there is.
 */
export function Sessions({
  sessions,
  onOpen,
}: {
  sessions: SessionCard[];
  onOpen: (sessionId: string) => void;
}) {
  return (
    <div className="wrap" style={{ padding: '44px 30px 90px' }}>
      <div
        style={{
          fontFamily: 'var(--mono)',
          fontSize: 11,
          letterSpacing: '0.14em',
          textTransform: 'uppercase',
          color: 'var(--em)',
          marginBottom: 14,
        }}
      >
        Sessions
      </div>
      <h1
        style={{
          fontSize: 40,
          fontWeight: 600,
          letterSpacing: '-0.02em',
          margin: '0 0 10px',
          lineHeight: 1.08,
        }}
      >
        Pick up where you are.
      </h1>
      <p style={{ margin: '0 0 40px', color: 'var(--dim)', fontSize: 15, maxWidth: '52ch' }}>
        Each session is a graph of concepts. Newest first.
      </p>

      {sessions.length === 0 ? (
        <div
          style={{
            padding: '34px 30px',
            borderRadius: 16,
            border: '1px dashed var(--line2)',
            maxWidth: 560,
            color: 'var(--dim)',
            lineHeight: 1.7,
          }}
        >
          Nothing here yet. Sessions are planned from the terminal — run{' '}
          <code
            style={{
              fontFamily: 'var(--mono)',
              background: 'var(--bg3)',
              color: 'var(--em-pale)',
              padding: '1.5px 5px',
              borderRadius: 4,
            }}
          >
            ailearn
          </code>{' '}
          and then{' '}
          <code
            style={{
              fontFamily: 'var(--mono)',
              background: 'var(--bg3)',
              color: 'var(--em-pale)',
              padding: '1.5px 5px',
              borderRadius: 4,
            }}
          >
            /init_learn 'a concept' competent
          </code>
          .
        </div>
      ) : (
        <div
          style={{
            display: 'grid',
            gridTemplateColumns: 'repeat(auto-fill,minmax(300px,1fr))',
            gap: 16,
            maxWidth: 1000,
          }}
        >
          {sessions.map((s) => (
            <button key={s.sessionId} className="session-card" onClick={() => onOpen(s.sessionId)}>
              <div
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: 10,
                  fontFamily: 'var(--mono)',
                  fontSize: 10.5,
                  letterSpacing: '0.1em',
                  textTransform: 'uppercase',
                  color: 'var(--dim)',
                }}
              >
                <span style={{ color: 'var(--em)' }}>{s.proficiency}</span>
                <span>·</span>
                <span>{s.numNodes} nodes</span>
                {!s.probed && (
                  <>
                    <span>·</span>
                    {/* A quiet marker, not a warning: it means the session was
                        planned from the topic alone rather than from a sat
                        diagnostic, so it starts at the beginning of the topic. */}
                    <span style={{ color: '#6f6f6f' }}>no diagnostic</span>
                  </>
                )}
              </div>
              <div
                style={{
                  fontSize: 19,
                  fontWeight: 500,
                  lineHeight: 1.28,
                  letterSpacing: '-0.01em',
                }}
              >
                {s.topic}
              </div>
              <div style={{ fontFamily: 'var(--mono)', fontSize: 10.5, color: '#a1a1a1', marginTop: 'auto' }}>
                {when(s.createdAt)}
              </div>
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
