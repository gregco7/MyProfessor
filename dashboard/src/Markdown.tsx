import ReactMarkdown from 'react-markdown';
import remarkGfm from 'remark-gfm';
import remarkMath from 'remark-math';
import rehypeKatex from 'rehype-katex';
import rehypeHighlight from 'rehype-highlight';

/**
 * Every `body` and every MARKDOWN_LATEX field goes through here. None of them
 * are ever rendered as plain text — the material is unreadable without the
 * maths and the code blocks.
 *
 * The plugin order matters. `rehype-highlight` runs before `rehype-katex` so
 * the maths pass never walks into a `<pre>`: a dollar sign in a code sample has
 * to stay a dollar sign. `remark-math` already refuses to parse inside code
 * spans and fences, and running highlight first means the maths plugin is
 * looking at nodes highlight has already claimed.
 */

const REMARK = [remarkGfm, remarkMath];

/**
 * `detect` lets an unlabelled fence still be highlighted; the generated material
 * usually labels them, but not always. The language set is highlight.js's own
 * common bundle — naming a subset does not shrink anything, because
 * rehype-highlight imports that bundle statically.
 */
const REHYPE = [
  [rehypeHighlight, { detect: true }],
  [rehypeKatex, { throwOnError: false }],
] as never;

export function Markdown({ children }: { children: string | null | undefined }) {
  if (!children) return null;
  return (
    <div className="md">
      <ReactMarkdown remarkPlugins={REMARK} rehypePlugins={REHYPE}>
        {children}
      </ReactMarkdown>
    </div>
  );
}

/** For a choice, a hint, or a rubric — prose that sits on one line. */
export function MarkdownInline({ children }: { children: string | null | undefined }) {
  if (!children) return null;
  return (
    <span className="md mdi">
      <ReactMarkdown remarkPlugins={REMARK} rehypePlugins={REHYPE}>
        {children}
      </ReactMarkdown>
    </span>
  );
}
