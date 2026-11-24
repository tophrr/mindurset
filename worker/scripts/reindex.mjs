#!/usr/bin/env node
// Rebuild the Vectorize index from the bundled corpus.
//
// This is an operational step, run by hand and never in CI: it needs a live
// account and the index does not exist there yet. Credentials come from the
// environment only; nothing here is committed or baked into the Worker.
//
//   CLOUDFLARE_ACCOUNT_ID=... CLOUDFLARE_API_TOKEN=... node scripts/reindex.mjs
//
// It writes .vectors.ndjson and prints the wrangler command that uploads it.
// Pass --upload to send the same file to the Vectorize API directly.

import { readdir, readFile, writeFile } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const WORKER_DIR = path.resolve(path.dirname(fileURLToPath(import.meta.url)), "..");
const CORPUS_DIR = path.join(WORKER_DIR, "src", "corpus");
const OUTPUT = path.join(WORKER_DIR, ".vectors.ndjson");
const INDEX = "mindurset-corpus";
const MODEL = "@cf/baai/bge-m3";
const BATCH = 32;

const accountId = process.env.CLOUDFLARE_ACCOUNT_ID?.trim();
const apiToken = process.env.CLOUDFLARE_API_TOKEN?.trim();

if (!accountId || !apiToken) {
  console.error(
    "CLOUDFLARE_ACCOUNT_ID and CLOUDFLARE_API_TOKEN are required; they are read from the environment only.",
  );
  process.exit(1);
}

// The corpus parser is duplicated from src/corpus/index.ts on purpose: this
// script is not bundled by Wrangler and cannot import TypeScript. Keep the two
// in step; a drifting chunk means a source id the Worker cannot resolve.
function slugify(value) {
  return value
    .toLowerCase()
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function parseFile(slug, markdown) {
  const body = markdown.replace(/<!--[\s\S]*?-->/g, "");
  const lines = body.split(/\r?\n/);
  const excerpts = [];
  const usedIds = new Set();
  let heading = null;
  let buffer = [];

  const flush = () => {
    if (heading === null) return;
    const text = buffer.join("\n").trim();
    if (text.length === 0) return;

    const base = slugify(heading) || "section";
    let section = base;
    let suffix = 2;
    while (usedIds.has(section)) {
      section = `${base}-${suffix}`;
      suffix += 1;
    }
    usedIds.add(section);

    excerpts.push({
      id: `${slug}#${section}`,
      // The heading is what the id is made from, so it belongs in the vector.
      text: `${heading}\n\n${text}`,
    });
  };

  for (const line of lines) {
    if (line.startsWith("## ")) {
      flush();
      heading = line.slice(3).trim();
      buffer = [];
    } else if (heading !== null) {
      buffer.push(line);
    }
  }
  flush();

  return excerpts;
}

async function embed(texts) {
  const response = await fetch(
    `https://api.cloudflare.com/client/v4/accounts/${accountId}/ai/run/${MODEL}`,
    {
      method: "POST",
      headers: {
        authorization: `Bearer ${apiToken}`,
        "content-type": "application/json",
      },
      body: JSON.stringify({ text: texts }),
    },
  );
  const payload = await response.json();
  const data = payload?.result?.data ?? payload?.data;
  if (!response.ok || !Array.isArray(data) || data.length !== texts.length) {
    throw new Error(
      `embedding failed (${response.status}): ${JSON.stringify(payload?.errors ?? payload)}`,
    );
  }
  return data;
}

const files = (await readdir(CORPUS_DIR))
  .filter((name) => name.endsWith(".md"))
  .sort();

const excerpts = [];
for (const name of files) {
  const markdown = await readFile(path.join(CORPUS_DIR, name), "utf8");
  excerpts.push(...parseFile(name.slice(0, -3), markdown));
}

const vectors = [];
for (let start = 0; start < excerpts.length; start += BATCH) {
  const batch = excerpts.slice(start, start + BATCH);
  const data = await embed(batch.map((excerpt) => excerpt.text));
  batch.forEach((excerpt, index) => {
    vectors.push({ id: excerpt.id, values: data[index] });
  });
}

const ndjson = vectors.map((vector) => JSON.stringify(vector)).join("\n") + "\n";
await writeFile(OUTPUT, ndjson, "utf8");

console.log(`Wrote ${vectors.length} vectors to ${path.relative(process.cwd(), OUTPUT)}`);

if (process.argv.includes("--upload")) {
  const response = await fetch(
    `https://api.cloudflare.com/client/v4/accounts/${accountId}/vectorize/v2/indexes/${INDEX}/upsert`,
    {
      method: "POST",
      headers: {
        authorization: `Bearer ${apiToken}`,
        "content-type": "application/x-ndjson",
      },
      body: ndjson,
    },
  );
  const payload = await response.json();
  if (!response.ok) {
    console.error(`Upload failed (${response.status}): ${JSON.stringify(payload?.errors ?? payload)}`);
    process.exitCode = 1;
  } else {
    console.log(`Upserted into ${INDEX}: ${JSON.stringify(payload?.result ?? {})}`);
  }
} else {
  console.log("Upload it from worker/ with:");
  console.log(`  npx wrangler vectorize insert ${INDEX} --file .vectors.ndjson`);
}
