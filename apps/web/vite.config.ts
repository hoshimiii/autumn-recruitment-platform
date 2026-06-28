import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

export default defineConfig({
  base: process.env.MICRO_APP_PUBLIC_PATH ?? "/",
  plugins: [react()],
  preview: {
    headers: {
      "Access-Control-Allow-Origin": "*"
    }
  },
  server: {
    headers: {
      "Access-Control-Allow-Origin": "*"
    }
  }
});
