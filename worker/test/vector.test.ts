import { describe, expect, it } from "vitest";
import Ajv2020 from "ajv/dist/2020.js";
import schema from "../../contract/generation-v1.schema.json";
import worker from "../src/index";
import { CORPUS, allSourceIds } from "../src/corpus/index";
import type { GenerationResponse } from "../src/contract";
import { MODEL_ID, type Env } from "../src/pipeline";
import { retrieve, retrieveSemantic } from "../src/retrieval";
import {
  EMBED_MODEL,
  MIN_SCORE,
  type EmbeddingBinding,
  type VectorizeBinding,
} from "../src/vector";

const ajv = new Ajv2020({ allErrors: true, strict: false });
const validate = ajv.compile(schema);

const QUERY = "I am worried about my exam";

function keywordIds(): string[] {
  return retrieve(QUERY, 3).map((excerpt) => excerpt.sourceId);
}

function ids(excerpts: Array<{ sourceId: string }>): string[] {
  return excerpts.map((excerpt) => excerpt.sourceId);
}

function fakeEmbedding(vector: number[] = [0.1, 0.2, 0.3]) {
  const calls: Array<{ model: string; text: string[] }> = [];
  const ai: EmbeddingBinding = {
    async run(model, inputs) {
      calls.push({ model, text: inputs.text });
      return { data: [vector] };
    },
  };
  return { ai, calls };
}

function fakeIndex(matches: Array<{ id: string; score: number }>): VectorizeBinding {
  return {
    async query(_vector, options) {
      return { matches: matches.slice(0, options.topK) };
    },
    async upsert() {
      return { mutationId: "fake" };
    },
  };
}

function post(
  path: string,
  body: unknown,
  env: Record<string, unknown>,
): Promise<Response> {
  return worker.fetch(
    new Request(`http://worker.test${path}`, {
      method: "POST",
      headers: { "content-type": "application/json" },
      body: JSON.stringify(body),
    }),
    env as Env,
  );
}

describe("semantic retrieval", () => {
  it("returns the corpus excerpt with its stable id, in score order", async () => {
    const first = CORPUS[0]!;
    const second = CORPUS[1]!;
    const { ai, calls } = fakeEmbedding([0.2, 0.4]);
    const env = {
      AI: ai,
      VECTORIZE: fakeIndex([
        { id: second.sourceId, score: MIN_SCORE + 0.05 },
        { id: first.sourceId, score: MIN_SCORE + 0.25 },
      ]),
    };

    const excerpts = await retrieveSemantic(env, QUERY, 2);

    expect(ids(excerpts)).toEqual([first.sourceId, second.sourceId]);
    expect(excerpts[0]).toBe(first);
    expect(excerpts[0]?.sourceId).toMatch(/^[a-z0-9-]+#[a-z0-9-]+$/);
    expect(calls).toEqual([{ model: EMBED_MODEL, text: [QUERY] }]);
  });

  it("returns the keyword result instead of a hit below the floor", async () => {
    const lowId = allSourceIds().find((id) => !keywordIds().includes(id))!;
    const { ai } = fakeEmbedding();
    const env = {
      AI: ai,
      VECTORIZE: fakeIndex([{ id: lowId, score: MIN_SCORE - 0.01 }]),
    };

    const excerpts = await retrieveSemantic(env, QUERY, 3);

    expect(ids(excerpts)).toEqual(keywordIds());
    expect(ids(excerpts)).not.toContain(lowId);
  });

  it("returns the keyword result when the embedding call throws", async () => {
    const ai: EmbeddingBinding = {
      async run() {
        throw new Error("model unavailable");
      },
    };
    const env = {
      AI: ai,
      VECTORIZE: fakeIndex([{ id: CORPUS[0]!.sourceId, score: 0.9 }]),
    };

    expect(ids(await retrieveSemantic(env, QUERY, 3))).toEqual(keywordIds());
  });

  it("returns the keyword result when a binding is missing", async () => {
    const cases: Array<Record<string, unknown>> = [
      {},
      { AI: fakeEmbedding().ai },
      { VECTORIZE: fakeIndex([{ id: CORPUS[0]!.sourceId, score: 0.9 }]) },
    ];

    for (const env of cases) {
      expect(ids(await retrieveSemantic(env, QUERY, 3))).toEqual(keywordIds());
    }
  });

  it("returns the keyword result for a malformed or failing index query", async () => {
    const broken: VectorizeBinding = {
      async query() {
        return { matches: "not an array" };
      },
      async upsert() {
        return { mutationId: "fake" };
      },
    };
    const rejecting: VectorizeBinding = {
      async query() {
        throw new Error("index unavailable");
      },
      async upsert() {
        return { mutationId: "fake" };
      },
    };

    for (const vector of [broken, rejecting]) {
      const env = { AI: fakeEmbedding().ai, VECTORIZE: vector };
      expect(ids(await retrieveSemantic(env, QUERY, 3))).toEqual(keywordIds());
    }
  });

  it("never returns an id the corpus does not carry", async () => {
    const real = CORPUS[2]!;
    const env = {
      AI: fakeEmbedding().ai,
      VECTORIZE: fakeIndex([
        { id: "invented#nowhere", score: 0.99 },
        { id: real.sourceId, score: MIN_SCORE + 0.1 },
      ]),
    };

    const excerpts = await retrieveSemantic(env, QUERY, 3);
    const known = new Set(allSourceIds());

    expect(ids(excerpts)).toEqual([real.sourceId]);
    for (const excerpt of excerpts) {
      expect(known.has(excerpt.sourceId)).toBe(true);
    }
  });

  it("serves a contract-v1 response when the vector path supplies the citations", async () => {
    // Two ids the keyword path would not return, so the citations can only
    // have come from the index.
    const served = allSourceIds()
      .filter((id) => !keywordIds().includes(id))
      .slice(0, 2);
    expect(served).toHaveLength(2);

    const ai: EmbeddingBinding = {
      async run(model) {
        if (model === EMBED_MODEL) return { data: [[0.2, 0.4, 0.6]] };
        return {
          response: JSON.stringify({
            message:
              "A hard exam can make the whole week feel like a verdict. One paper is one paper, and the next small step is still yours to choose.",
            distortion: "all-or-nothing thinking",
            perspectives: [
              "You have handled difficult weeks before, so this week is evidence about this week.",
              "A result you dislike is information about one paper, not a final judgement.",
            ],
            micro_action: { title: "Open the exam outline for ten minutes", minutes: 10 },
            source_ids: served,
            risk_level: { level: "L1", confidence: 0.8 },
          }),
        };
      },
    };

    const res = await post("/v1/reframe", { thought: QUERY }, {
      AI: ai,
      VECTORIZE: fakeIndex([
        { id: served[0]!, score: 0.9 },
        { id: served[1]!, score: 0.8 },
      ]),
    });

    expect(res.status).toBe(200);
    const body = (await res.json()) as GenerationResponse;
    expect(validate(body)).toBe(true);
    expect(body.model_version).toBe(MODEL_ID);
    expect(body.source_ids).toEqual(served);
  });
});
