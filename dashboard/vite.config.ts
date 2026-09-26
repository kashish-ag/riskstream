import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

// In dev, the browser talks only to Vite; API calls are proxied to the two Spring Boot services.
// (In Docker, nginx does the same job — see nginx.conf.)
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      "/api/producer": "http://localhost:8081",
      "/api": "http://localhost:8082",
    },
  },
});
