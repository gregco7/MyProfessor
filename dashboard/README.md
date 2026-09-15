# MyProfessor dashboard

React + Vite front end for the MyProfessor API, built from
`MyProfessor Dashboard v2.dc.html` in the Claude Design project.

## Running it

```bash
./mvnw spring-boot:run          # API on :8080, from the repository root
cd dashboard && npm install && npm run dev   # dashboard on :5173
```

`npm run dev` proxies `/api` to `localhost:8080`, so development is same-origin
and CORS never comes into it. The allowed-origins list in
`application.yaml` exists only for the case where a dev server is pointed
straight at the backend port instead.

## Shipping it

```bash
cd dashboard && npm run build
```

That writes into `src/main/resources/static/`, where Spring serves it at `/`.
The output is **git-ignored** — it is a build artifact, so rebuild it rather
than committing it, and run this before `./mvnw package` if you want the jar to
carry the dashboard.

Client-side routes are resolved by `DashboardWebConfig`: a request that matches
a real file gets that file, a path with no extension falls back to `index.html`,
and a missing asset 404s honestly rather than being answered with HTML.

## Shape

| File | What it holds |
|---|---|
| `types.ts` | The wire contract. Nothing here is invented — if a screen wants a field that is not on one of these types, it does not exist. |
| `api.ts` | The eight endpoints, relative paths, RFC 9457 errors surfaced as `ApiError`. |
| `graph.ts` | Availability and tier depth derived from `prerequisiteIds`, plus the nodemap layout. |
| `Markdown.tsx` | The only way any `body` is ever rendered: GFM, `$…$` maths, highlighted code. |
| `Sessions.tsx` | The list, newest first. |
| `Workspace.tsx` | One session: reading left, nodemap pinned right, three modes. |
| `Quiz.tsx` / `Review.tsx` | Sitting a lesson, and the answer key afterwards. |

## Two things the dashboard deliberately does not do

**It never triggers the long Claude calls.** Planning a probe (~50 s) and a
session (2–3 minutes) is driven from the CLI (`./ailearn`), where the learner
watches them. The dashboard reads sessions that already exist, so nothing here
needs minutes-long request handling.

**It never decides what is correct.** Marking is `POST /api/nodes/{id}/attempt`
on the server; `isCorrect` only ever arrives from the review call, and only
after an attempt exists. The prototype marked answers client-side to work
without a server — that shortcut is not carried over.
