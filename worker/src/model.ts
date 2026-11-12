// Model boundary.
//
// The pipeline talks to a ModelClient, not to Workers AI directly. That keeps
// the real binding and the deterministic local adapter interchangeable:
//  - workersAiClient: the production path, through the `AI` platform binding;
//  - localModelClient: a deterministic dev adapter behind DEV_MODEL=local,
//    so `wrangler dev --local` can exercise retrieval, prompts, normalisation
//    and guardrails without an account or a workers.dev subdomain.
//
// The local adapter is explicitly labelled "local-stub-v1". It is not a model
// answer and must never be enabled in production.

import { detectLanguage } from "./guardrails";
import { buildMessages, type ChatMessage, type PromptInput } from "./prompts";

export const LOCAL_MODEL_VERSION = "local-stub-v1";

export interface AiBinding {
  run(
    model: string,
    inputs: {
      messages: ChatMessage[];
      max_tokens?: number;
      temperature?: number;
    },
  ): Promise<unknown>;
}

export interface ModelClient {
  readonly version: string;
  /** A draft object the pipeline will normalise, or null when nothing usable came back. */
  generate(input: PromptInput): Promise<unknown | null>;
}

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

/** Pull the text out of the shapes Workers AI returns for text-generation models. */
export function extractModelText(output: unknown): string | null {
  if (typeof output === "string") return output;
  if (!isRecord(output)) return null;
  if (typeof output.response === "string") return output.response;

  if (isRecord(output.result) && typeof output.result.response === "string") {
    return output.result.response;
  }

  if (Array.isArray(output.choices)) {
    const first = output.choices[0];
    if (isRecord(first)) {
      if (isRecord(first.message) && typeof first.message.content === "string") {
        return first.message.content;
      }
      if (typeof first.text === "string") return first.text;
    }
  }

  return null;
}

function parseJsonObject(text: string): Record<string, unknown> | null {
  const fenced = text.match(/```(?:json)?\s*([\s\S]*?)```/i);
  const candidate = fenced?.[1] ?? text;
  const start = candidate.indexOf("{");
  const end = candidate.lastIndexOf("}");
  if (start === -1 || end <= start) return null;

  try {
    const parsed: unknown = JSON.parse(candidate.slice(start, end + 1));
    return isRecord(parsed) ? parsed : null;
  } catch {
    return null;
  }
}

export function workersAiClient(ai: AiBinding, model: string): ModelClient {
  return {
    version: model,
    async generate(input) {
      const output = await ai.run(model, {
        messages: buildMessages(input),
        max_tokens: 900,
        temperature: 0.3,
      });
      const text = extractModelText(output);
      return text ? parseJsonObject(text) : null;
    },
  };
}

function buildLocalDraft(input: PromptInput): Record<string, unknown> | null {
  const primary = input.excerpts[0]?.sourceId;
  if (!primary) return null;
  const secondary = input.excerpts[1]?.sourceId;
  const sourceIds = secondary ? [primary, secondary] : [primary];
  const english = detectLanguage(input.userText) === "en";

  const message = (() => {
    if (input.kind === "summarise") {
      return english
        ? "This session moved through a check-in and a reframe. What stands out is how much room the hard moment took, and that the next step stayed small enough to start."
        : "Sesi ini bergerak dari check-in ke reframe. Yang terlihat adalah seberapa besar ruang yang diambil momen berat itu, dan bahwa langkah berikutnya tetap cukup kecil untuk dimulai.";
    }
    if (input.mode === "plan") {
      return english
        ? "It sounds like this is taking up a lot of room right now. Pick one small step you can finish in the next few minutes; that is enough to start."
        : "Kedengarannya ini sedang memenuhi banyak ruang sekarang. Pilih satu langkah kecil yang bisa selesai dalam beberapa menit; itu sudah cukup untuk mulai.";
    }
    if (input.mode === "reflect") {
      return english
        ? "It sounds like this is taking up a lot of room right now. One hard moment is information about the moment, not a verdict about you."
        : "Kedengarannya ini sedang memenuhi banyak ruang sekarang. Satu momen berat adalah informasi tentang momen itu, bukan vonis tentang dirimu.";
    }
    return english
      ? "It sounds like this is taking up a lot of room right now. I am here with you; what feels heaviest as you say it out loud?"
      : "Kedengarannya ini sedang memenuhi banyak ruang sekarang. Aku di sini; bagian mana yang paling berat kalau kamu ucapkan pelan-pelan?";
  })();

  return {
    message,
    distortion: english ? "narrowed attention" : "perhatian menyempit",
    perspectives: english
      ? [
          "A hard moment is information about the moment, not a verdict about you.",
          "The next small step can be chosen without solving the whole week.",
        ]
      : [
          "Momen yang berat adalah informasi tentang momen itu, bukan vonis tentang dirimu.",
          "Langkah kecil berikutnya bisa dipilih tanpa harus menyelesaikan seluruh minggu.",
        ],
    micro_action: {
      title: english ? "Write down one small next step" : "Tulis satu langkah kecil berikutnya",
      minutes: 5,
    },
    source_ids: sourceIds,
    risk_level: { level: "L0", confidence: 0.6 },
  };
}

export function localModelClient(): ModelClient {
  return {
    version: LOCAL_MODEL_VERSION,
    async generate(input) {
      return buildLocalDraft(input);
    },
  };
}
