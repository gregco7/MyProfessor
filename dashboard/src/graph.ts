import type { NodeCard } from './types';

/**
 * Everything the dashboard works out for itself about the lesson graph.
 *
 * The API sends no "available" flag and none is added to the wire types. A node
 * is unlocked when every id in its `prerequisiteIds` belongs to a node in the
 * same session whose `passed` is true; an empty list means available now.
 *
 * The same pass yields a depth per node — 0 with no prerequisites, otherwise one
 * more than the deepest prerequisite — which becomes the tier band it is drawn
 * on.
 */
export interface Derived {
  byId: Record<string, NodeCard>;
  depth: Record<string, number>;
  unlocked: Record<string, boolean>;
}

export function derive(nodes: NodeCard[]): Derived {
  const byId: Record<string, NodeCard> = {};
  nodes.forEach((n) => {
    byId[n.nodeId] = n;
  });

  const depth: Record<string, number> = {};
  // `visiting` guards against a cycle. The API only ever emits edges pointing
  // backwards, so this should be unreachable — but a cycle here would be an
  // infinite recursion rather than a wrong picture, which is worth preventing.
  const visiting = new Set<string>();
  const depthOf = (id: string): number => {
    const known = depth[id];
    if (known != null) return known;
    if (visiting.has(id)) return 0;
    visiting.add(id);
    const node = byId[id];
    const prereqs = (node?.prerequisiteIds ?? []).filter((p) => byId[p]);
    depth[id] = prereqs.length === 0 ? 0 : 1 + Math.max(...prereqs.map(depthOf));
    visiting.delete(id);
    return depth[id];
  };
  nodes.forEach((n) => depthOf(n.nodeId));

  const unlocked: Record<string, boolean> = {};
  nodes.forEach((n) => {
    unlocked[n.nodeId] = n.prerequisiteIds.every((p) => byId[p]?.passed);
  });

  return { byId, depth, unlocked };
}

export interface MapNode {
  node: NodeCard;
  x: number;
  y: number;
  size: number;
  unlocked: boolean;
  selected: boolean;
}

export interface MapEdge {
  key: string;
  x1: number;
  y1: number;
  x2: number;
  y2: number;
  live: boolean;
}

export interface MapLayout {
  width: number;
  height: number;
  nodes: MapNode[];
  edges: MapEdge[];
}

const TOP = 54;
const ROW = 132;
/** A fixed per-slot nudge, so tiers read as a drawn graph rather than a grid. */
const JITTER = [-9, 11, -5, 8, 3, -12];

export function layout(nodes: NodeCard[], selectedId: string | null, width: number): MapLayout {
  const { byId, depth, unlocked } = derive(nodes);
  const w = Math.max(240, width || 366);

  const tiers: Record<number, NodeCard[]> = {};
  nodes.forEach((n) => {
    const d = depth[n.nodeId];
    (tiers[d] = tiers[d] ?? []).push(n);
  });
  const bands = Object.keys(tiers)
    .map(Number)
    .sort((a, b) => a - b);

  const pos: Record<string, { x: number; y: number }> = {};
  bands.forEach((band, bandIndex) => {
    const list = tiers[band];
    list.forEach((n, i) => {
      const span = w - 72;
      const x = 36 + ((i + 1) / (list.length + 1)) * span + JITTER[(bandIndex * 2 + i) % JITTER.length];
      pos[n.nodeId] = { x: Math.round(x), y: TOP + bandIndex * ROW };
    });
  });

  const radius: Record<string, number> = {};
  const mapNodes: MapNode[] = nodes.map((n) => {
    const selected = n.nodeId === selectedId;
    const size = selected ? 30 : n.passed ? 24 : 22;
    radius[n.nodeId] = size / 2 + 2;
    return {
      node: n,
      x: pos[n.nodeId].x,
      y: pos[n.nodeId].y,
      size,
      unlocked: unlocked[n.nodeId],
      selected,
    };
  });

  // Each edge is trimmed back to the rims of the two nodes it joins, so no line
  // is drawn across the inside of a circle.
  const edges: MapEdge[] = [];
  nodes.forEach((n) =>
    n.prerequisiteIds.forEach((pid) => {
      const from = pos[pid];
      const to = pos[n.nodeId];
      if (!from || !to) return;
      const dx = to.x - from.x;
      const dy = to.y - from.y;
      const len = Math.hypot(dx, dy) || 1;
      const ux = dx / len;
      const uy = dy / len;
      edges.push({
        key: `${pid}->${n.nodeId}`,
        x1: from.x + ux * radius[pid],
        y1: from.y + uy * radius[pid],
        x2: to.x - ux * radius[n.nodeId],
        y2: to.y - uy * radius[n.nodeId],
        live: !!byId[pid]?.passed,
      });
    }),
  );

  return {
    width: w,
    height: TOP + (bands.length - 1) * ROW + 46,
    nodes: mapNodes,
    edges,
  };
}
