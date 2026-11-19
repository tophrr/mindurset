// Embedding and Vectorize access for semantic retrieval.
//
// This module only knows how to turn text into vectors and how to ask the index
// for neighbours. It does not fall back: retrieval.ts owns that decision, and
// every error here is allowed to escape to it.

export const EMBED_MODEL = "@cf/baai/bge-m3";

// Cosine-similarity floor for a citation, measured rather than guessed. On
// 19 November, bge-m3 against the twenty-one sections of corpus v1: a relevant
// query peaks between 0.47 and 0.54 ("kepalaku penuh, susah fokus, badan lemas"
// reached 0.538 on stress#exam-pressure), while an unrelated one stays at or
// below 0.37 ("cara membuat kue bolu kukus" reached 0.368 on
// action#make-the-step-concrete). 0.42 sits in that gap. The forty-case
// evaluation run has to re-check the number instead of inheriting it.
export const MIN_SCORE = 0.42;

/** The one call this module makes on the shared `AI` platform binding. */
export interface EmbeddingBinding {
  run(model: string, inputs: { text: string[] }): Promise<unknown>;
}

export interface VectorMatch {
  id: string;
  score: number;
}

/**
 * The part of the Vectorize binding retrieval uses. Kept narrow on purpose so a
 * test can fake the index without the real platform types or a network call.
 */
export interface VectorizeBinding {
  query(
    vector: number[],
    options: { topK: number },
  ): Promise<unknown>;
  upsert(vectors: Array<{ id: string; values: number[] }>): Promise<unknown>;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

/**
 * One vector per input text. Workers AI returns the batch under `data`, shaped
 * by the model as an array of arrays; anything else is malformed and throws.
 */
export async function embed(ai: EmbeddingBinding, texts: string[]): Promise<number[][]> {
  const output = await ai.run(EMBED_MODEL, { text: texts });
  if (!isRecord(output) || !Array.isArray(output.data)) {
    throw new Error("embedding: missing data");
  }

  const vectors = output.data;
  if (vectors.length !== texts.length) {
    throw new Error("embedding: wrong vector count");
  }

  for (const vector of vectors) {
    if (!Array.isArray(vector) || !vector.every((value) => typeof value === "number")) {
      throw new Error("embedding: malformed vector");
    }
  }

  return vectors as number[][];
}
