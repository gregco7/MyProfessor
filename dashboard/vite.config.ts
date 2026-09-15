import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dashboard is served by Spring from src/main/resources/static in
// production, so every request is same-origin and API paths stay relative.
// In development the proxy reproduces that, which keeps CORS out of the
// picture entirely rather than relying on the allowed-origins list.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: { '/api': { target: 'http://localhost:8080', changeOrigin: true } },
  },
  build: {
    outDir: '../src/main/resources/static',
    emptyOutDir: true,
    rollupOptions: {
      output: {
        // One vendor chunk, not several. KaTeX, highlight.js and the markdown
        // pipeline share the unified/vfile modules underneath, and splitting
        // them apart puts those shared modules in one chunk while a sibling
        // reads them at module-evaluation time — which fails at runtime with
        // "cannot access before initialization". Keeping every dependency
        // together still separates rarely-changing vendor code from the app.
        manualChunks: (id) => (id.includes('node_modules') ? 'vendor' : undefined),
      },
    },
  },
});
