import { useEffect, useRef, useState } from 'react';
import { layout } from './graph';
import type { NodeCard } from './types';

interface Tip {
  x: number;
  y: number;
  title: string;
  meta: string;
}

/**
 * The lesson graph, pinned beside the reading for the whole session.
 *
 * Clicking a node swaps the left pane and leaves this in place, so the shape of
 * the course never goes off screen. Locked nodes are drawn but not clickable —
 * seeing what is ahead is the point of a map.
 */
export function Nodemap({
  nodes,
  selectedId,
  onOpen,
}: {
  nodes: NodeCard[];
  selectedId: string | null;
  onOpen: (nodeId: string) => void;
}) {
  const boxRef = useRef<HTMLDivElement>(null);
  const [width, setWidth] = useState(366);
  const [tip, setTip] = useState<Tip | null>(null);

  // The panel is re-measured on resize rather than assuming a width, because it
  // shares a flexible row with the reading pane.
  useEffect(() => {
    const el = boxRef.current;
    if (!el) return;
    const measure = () => {
      const w = Math.round(el.clientWidth - 32);
      if (w > 0) setWidth((prev) => (Math.abs(w - prev) > 2 ? w : prev));
    };
    measure();
    const ro = new ResizeObserver(measure);
    ro.observe(el);
    return () => ro.disconnect();
  }, []);

  const map = layout(nodes, selectedId, width);
  const passed = nodes.filter((n) => n.passed).length;

  return (
    <div className="panel">
      <div className="panel-head">
        <span className="eyebrow">Nodemap</span>
        <span className="meta" style={{ marginLeft: 'auto' }}>
          {passed} of {nodes.length} passed
        </span>
      </div>

      <div ref={boxRef} style={{ padding: '18px 16px 12px' }}>
        <div style={{ position: 'relative', width: '100%', height: map.height }}>
          <svg
            width={map.width}
            height={map.height}
            style={{ position: 'absolute', left: 0, top: 0, overflow: 'visible', pointerEvents: 'none' }}
          >
            {map.edges.map((e) => (
              <line
                key={e.key}
                x1={e.x1}
                y1={e.y1}
                x2={e.x2}
                y2={e.y2}
                stroke={e.live ? '#00bb7f' : '#2a2a2a'}
                strokeWidth={e.live ? 2 : 1.5}
                strokeLinecap="round"
              />
            ))}
          </svg>

          {map.nodes.map((m) => {
            const { node, unlocked, selected } = m;
            const state = node.passed ? 'passed' : unlocked ? 'open' : 'locked';
            return (
              <button
                key={node.nodeId}
                className="mapnode"
                disabled={!unlocked}
                aria-label={`${node.subtopic} — ${state}`}
                onClick={() => unlocked && onOpen(node.nodeId)}
                onMouseMove={(e) => {
                  const box = e.currentTarget.parentElement!.getBoundingClientRect();
                  setTip({
                    x: Math.round(e.clientX - box.left),
                    y: Math.round(e.clientY - box.top) - 14,
                    title: node.subtopic,
                    meta: `${node.numSections} sections · ${node.numQuestions} questions · ${state}`,
                  });
                }}
                onMouseLeave={() => setTip(null)}
                style={{
                  left: m.x,
                  top: m.y,
                  width: m.size,
                  height: m.size,
                  marginLeft: -m.size / 2,
                  marginTop: -m.size / 2,
                  border: `2px solid ${node.passed ? '#00d294' : unlocked ? '#00bb7f' : '#3a3a3a'}`,
                  background: node.passed ? '#00bb7f' : unlocked ? 'rgba(0,187,127,.14)' : '#141414',
                  boxShadow: selected
                    ? '0 0 0 6px rgba(0,187,127,.10), 0 0 30px 2px rgba(0,187,127,.55)'
                    : node.passed
                      ? '0 0 16px rgba(0,187,127,.45)'
                      : 'none',
                  cursor: unlocked ? 'pointer' : 'not-allowed',
                }}
              />
            );
          })}

          {tip && (
            <div
              style={{
                position: 'absolute',
                left: tip.x,
                top: tip.y,
                transform: 'translate(-50%,-100%)',
                maxWidth: 210,
                padding: '9px 11px',
                borderRadius: 8,
                border: '1px solid #2f6d56',
                background: 'rgba(10,10,10,.96)',
                boxShadow: '0 10px 30px -10px rgba(0,0,0,.9)',
                pointerEvents: 'none',
                zIndex: 5,
              }}
            >
              <div style={{ fontSize: 12.5, fontWeight: 500, lineHeight: 1.35, color: 'var(--fg)' }}>
                {tip.title}
              </div>
              <div
                style={{
                  fontFamily: 'var(--mono)',
                  fontSize: 10,
                  color: '#8f8f8f',
                  marginTop: 5,
                  whiteSpace: 'nowrap',
                }}
              >
                {tip.meta}
              </div>
            </div>
          )}
        </div>
      </div>

      <div style={{ padding: '14px 20px 18px', borderTop: '1px solid var(--line)' }}>
        <div style={{ display: 'flex', alignItems: 'center', gap: 14, flexWrap: 'wrap' }}>
          {[
            { label: 'passed', ring: '#00d294', fill: '#00bb7f' },
            { label: 'open', ring: '#00bb7f', fill: 'rgba(0,187,127,.14)' },
            { label: 'locked', ring: '#3a3a3a', fill: '#141414' },
          ].map((lg) => (
            <div key={lg.label} style={{ display: 'flex', alignItems: 'center', gap: 7 }}>
              <span
                style={{
                  width: 9,
                  height: 9,
                  borderRadius: '50%',
                  border: `1.5px solid ${lg.ring}`,
                  background: lg.fill,
                }}
              />
              <span
                style={{
                  fontFamily: 'var(--mono)',
                  fontSize: 9.5,
                  letterSpacing: '0.06em',
                  textTransform: 'uppercase',
                  color: '#a1a1a1',
                }}
              >
                {lg.label}
              </span>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}
