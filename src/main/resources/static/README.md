# Dashboard build output goes here

Copy the built React app into this directory (the contents of `dist/`, so that
`index.html` sits directly in `static/`). Spring serves it at `/`, and
`DashboardWebConfig` forwards client-side routes to `index.html`.

Anything in here is served as-is and is public. Do not put source, `.env` files,
or maps you would not hand to a stranger in this directory.

While developing, you do not need this at all — run the dev server on its own
port and let CORS handle it. See `ReactDashboard.md` in the repository root.
