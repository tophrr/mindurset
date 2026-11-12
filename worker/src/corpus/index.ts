// Corpus v1. The markdown files are the content; this module parses them into
// excerpts with stable source ids of the form `<topic>#<section>`.
//
// The ids are part of the contract: the client may show them as citations, and
// the schema only accepts `^[a-z0-9-]+#[a-z0-9-]+$`. Keep file names and
// headings lowercase and hyphenated so the ids stay stable when the prose is
// edited.

import action from "./action.md";
import allOrNothing from "./all-or-nothing.md";
import rumination from "./rumination.md";
import safetyPlan from "./safety-plan.md";
import selfCompassion from "./self-compassion.md";
import sleep from "./sleep.md";
import stress from "./stress.md";
import version from "./version.json";

export interface Excerpt {
  sourceId: string;
  topic: string;
  heading: string;
  text: string;
  keywords: string[];
}

interface CorpusFile {
  slug: string;
  markdown: string;
}

export const CORPUS_VERSION: string = version.version;

const FILES: CorpusFile[] = [
  { slug: "action", markdown: action },
  { slug: "all-or-nothing", markdown: allOrNothing },
  { slug: "rumination", markdown: rumination },
  { slug: "safety-plan", markdown: safetyPlan },
  { slug: "self-compassion", markdown: selfCompassion },
  { slug: "sleep", markdown: sleep },
  { slug: "stress", markdown: stress },
];

function slugify(value: string): string {
  return value
    .toLowerCase()
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

function parseKeywords(markdown: string): string[] {
  const match = markdown.match(/<!--\s*keywords:\s*(.*?)\s*-->/i);
  if (!match?.[1]) return [];
  return match[1]
    .split(",")
    .map((keyword) => keyword.trim().toLowerCase())
    .filter((keyword) => keyword.length > 0);
}

function parseFile(file: CorpusFile): Excerpt[] {
  const keywords = parseKeywords(file.markdown);
  const body = file.markdown.replace(/<!--[\s\S]*?-->/g, "");
  const lines = body.split(/\r?\n/);
  const excerpts: Excerpt[] = [];
  const usedIds = new Set<string>();
  let heading: string | null = null;
  let buffer: string[] = [];

  const flush = (): void => {
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
      sourceId: `${file.slug}#${section}`,
      topic: file.slug,
      heading: heading.trim(),
      text,
      keywords,
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

export const CORPUS: Excerpt[] = FILES.flatMap(parseFile);

export function allSourceIds(): string[] {
  return CORPUS.map((excerpt) => excerpt.sourceId);
}
