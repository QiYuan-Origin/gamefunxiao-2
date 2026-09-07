import { defineConfig } from 'vite';

const tunnelHosts = true;

export default defineConfig({
  build: {
    chunkSizeWarningLimit: 1200
  },
  server: {
    host: '0.0.0.0',
    port: 4174,
    allowedHosts: tunnelHosts
  },
  preview: {
    host: '0.0.0.0',
    port: 4175,
    allowedHosts: tunnelHosts
  }
});
