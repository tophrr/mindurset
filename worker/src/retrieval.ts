// Keyword retrieval over corpus v1.
//
// The corpus is small and bundled, so a deterministic keyword score is enough:
// no embedding call, no key, no network, and the same answer every time. The
// query and the excerpts are expanded with a tiny bilingual synonym table so an
// Indonesian message can still reach English source material.

import { CORPUS, CORPUS_VERSION, type Excerpt } from "./corpus/index";
import {
  MIN_SCORE,
  embed,
  type EmbeddingBinding,
  type VectorizeBinding,
} from "./vector";

export { CORPUS_VERSION };
export type { Excerpt };

const STOPWORDS = new Set([
  "the", "and", "for", "are", "but", "not", "you", "your", "yours", "with", "this", "that",
  "these", "those", "from", "have", "has", "had", "was", "were", "will", "would", "can",
  "could", "should", "about", "into", "over", "under", "just", "very", "too", "all", "any",
  "how", "what", "when", "where", "why", "who", "its", "it's", "i'm", "im", "my", "me",
  "we", "our", "they", "them", "their", "he", "she", "his", "her", "been", "being", "do",
  "does", "did", "than", "then", "there", "here", "out", "off", "on", "in", "at", "to",
  "of", "is", "am", "be", "as", "by", "or", "if", "so", "no", "up", "down", "get", "got",
  "yang", "dan", "atau", "untuk", "dengan", "saya", "aku", "kamu", "dia", "ini", "itu",
  "tidak", "nggak", "gak", "ga", "di", "ke", "dari", "pada", "juga", "sudah", "masih",
  "akan", "bisa", "ada", "adalah", "karena", "kalau", "jadi", "lagi", "saja", "banget",
]);

const SYNONYM_GROUPS: string[][] = [
  ["stress", "stressed", "tekanan", "stres", "pressure", "overwhelmed", "kewalahan", "beban", "burden"],
  ["anxious", "anxiety", "cemas", "kecemasan", "worried", "worry", "khawatir", "takut", "panic", "panik"],
  ["sad", "sadness", "sedih", "down", "low", "mood", "murung"],
  ["fail", "failure", "failed", "gagal", "kegagalan", "not good enough", "tidak cukup"],
  ["exam", "exams", "ujian", "test", "tests", "paper", "papers", "deadline", "deadlines", "tugas", "assignment"],
  ["sleep", "sleeping", "tidur", "insomnia", "sleepless", "begadang", "night", "malam", "tired", "lelah", "capek"],
  ["think", "thinking", "overthinking", "rumination", "ruminating", "overpikir", "mikir", "loop", "replay"],
  ["action", "step", "steps", "tindakan", "langkah", "start", "starting", "mulai", "procrastination", "menunda", "stuck", "macet"],
  ["kind", "compassion", "self-compassion", "baik", "self-worth", "harga diri", "shame", "malu", "friend", "teman"],
  ["safe", "safety", "crisis", "krisis", "danger", "bahaya", "emergency", "darurat", "alone", "sendirian", "bantuan"],
  ["perfect", "perfectionism", "sempurna", "always", "never", "selalu", "tidak pernah", "all-or-nothing"],
];

const SYNONYM_LOOKUP = new Map<string, string[]>();
for (const group of SYNONYM_GROUPS) {
  for (const word of group) {
    SYNONYM_LOOKUP.set(word, group);
  }
}

function normalise(value: string): string {
  return value
    .toLowerCase()
    .normalize("NFKD")
    .replace(/[\u0300-\u036f]/g, "");
}

export function tokenize(value: string): string[] {
  return normalise(value)
    .split(/[^a-z0-9]+/)
    .filter((token) => token.length > 2 && !STOPWORDS.has(token));
}

function expand(token: string): string[] {
  return SYNONYM_LOOKUP.get(token) ?? [token];
}

function tokenSet(value: string): Set<string> {
  const tokens = new Set<string>();
  for (const token of tokenize(value)) {
    for (const variant of expand(token)) {
      tokens.add(variant);
    }
  }
  return tokens;
}

/**
 * Return the top-k excerpts for a query. An excerpt must share at least one
 * content token with the query. An empty result means the caller should use the
 * offline fallback rather than invent an ungrounded answer.
 */
export function retrieve(query: string, k = 3): Excerpt[] {
  const queryTokens = tokenSet(query);
  if (queryTokens.size === 0) return [];

  const phrase = normalise(query).trim();
  const scored = CORPUS.map((excerpt) => {
    const heading = tokenSet(excerpt.heading);
    const body = tokenSet(excerpt.text);
    const keywords = tokenSet(excerpt.keywords.join(" "));
    let score = 0;

    for (const token of queryTokens) {
      if (heading.has(token)) score += 3;
      if (keywords.has(token)) score += 2;
      if (body.has(token)) score += 1;
    }

    if (phrase.length >= 4) {
      const haystack = normalise(`${excerpt.heading}\n${excerpt.text}`);
      if (haystack.includes(phrase)) score += 2;
    }

    return { excerpt, score };
  });

  return scored
    .filter((entry) => entry.score > 0)
    .sort((a, b) => b.score - a.score || a.excerpt.sourceId.localeCompare(b.excerpt.sourceId))
    .slice(0, Math.max(1, k))
    .map((entry) => entry.excerpt);
}

/**
 * The bindings semantic retrieval reads. `AI` stays `unknown` here because the
 * pipeline owns the chat shape of the same binding; this module only claims the
 * embedding call it makes, and validates the answer before trusting it.
 */
export interface SemanticEnv {
  AI?: unknown;
  VECTORIZE?: VectorizeBinding;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

/** A Vectorize match, as far as this module needs to check it. */
function isMatch(value: unknown): value is { id: string; score: number } {
  return (
    isRecord(value) &&
    typeof value.id === "string" &&
    typeof value.score === "number" &&
    Number.isFinite(value.score)
  );
}

/**
 * Vector retrieval with the keyword retriever as the fallback.
 *
 * A query is embedded, the index is searched for the top k above MIN_SCORE, and
 * the ids are mapped back to CORPUS. A missing binding, a model or query error,
 * a malformed response, or no hit at or above the floor all return
 * `retrieve(query, k)`, so the shipped Worker behaves exactly as before when no
 * index is configured. An id the corpus does not carry is dropped, never cited.
 */
export async function retrieveSemantic(
  env: SemanticEnv,
  query: string,
  k = 3,
): Promise<Excerpt[]> {
  const top = Math.max(1, k);

  try {
    if (!env.AI || !env.VECTORIZE) return retrieve(query, k);

    const [values] = await embed(env.AI as EmbeddingBinding, [query]);
    if (!values) return retrieve(query, k);

    const response = await env.VECTORIZE.query(values, {
      topK: top,
      returnMetadata: false,
    });
    if (!isRecord(response) || !Array.isArray(response.matches)) {
      return retrieve(query, k);
    }

    const byId = new Map(CORPUS.map((excerpt) => [excerpt.sourceId, excerpt]));
    const hits = response.matches
      .filter(isMatch)
      .filter((match) => match.score >= MIN_SCORE)
      .sort((a, b) => b.score - a.score || a.id.localeCompare(b.id));

    const seen = new Set<string>();
    const excerpts: Excerpt[] = [];
    for (const hit of hits) {
      if (seen.has(hit.id)) continue;
      const excerpt = byId.get(hit.id);
      if (!excerpt) continue;
      seen.add(hit.id);
      excerpts.push(excerpt);
      if (excerpts.length === top) break;
    }

    return excerpts.length > 0 ? excerpts : retrieve(query, k);
  } catch {
    return retrieve(query, k);
  }
}

export function sourceIds(excerpts: Excerpt[]): string[] {
  return excerpts.map((excerpt) => excerpt.sourceId);
}
