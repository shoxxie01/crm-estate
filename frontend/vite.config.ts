import { fileURLToPath, URL } from "node:url";
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

export default defineConfig({
  plugins: [react(), tailwindcss()],
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
  optimizeDeps: {
    // MapLibre uruchamia dekodowanie kafelków w web workerze, którego adres
    // składa przez `new Worker(new URL(...))`. Wstępne pakowanie zależności
    // (esbuild) przepisuje ten adres tak, że worker nie wstaje. Mapa ładuje
    // styl i sprite'y, po czym rysuje puste płótno, bez błędu widocznego
    // w interfejsie. Wyłączenie z pre-bundlingu dotyczy tylko trybu dev;
    // produkcyjny build przez Rollup radzi sobie z tym sam.
    exclude: ["maplibre-gl"],
  },
  server: {
    port: 5173,
    proxy: {
      // Spring Boot backend
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
    },
  },
});
