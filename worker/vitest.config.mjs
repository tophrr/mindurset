import { readFileSync } from "node:fs";
import { defineConfig } from "vitest/config";

// Vitest does not read markdown as text by default. Wrangler does, through the
// [[rules]] Text loader in wrangler.toml; this plugin gives the test runner the
// same behaviour so the corpus has one source of truth.
export default defineConfig({
  plugins: [
    {
      name: "markdown-as-text",
      enforce: "pre",
      load(id) {
        const path = id.split("?")[0] ?? id;
        if (!path.endsWith(".md")) return null;
        const content = readFileSync(path, "utf8");
        return `export default ${JSON.stringify(content)};`;
      },
    },
  ],
});
