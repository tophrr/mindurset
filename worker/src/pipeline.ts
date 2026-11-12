// The generation pipeline behind contract v1.
//
// Flow: local risk floor -> keyword retrieval over the bundled corpus ->
// prompt v1 -> Workers AI -> normalise into the frozen shape -> output
// guardrails. Any failure on that path returns the offline fallback with HTTP
// 200 and model_version "offline-fallback"; a 500 is never a fallback.

import fixture from "../../contract/fixtures/generation-ok.json";
import { maxRisk, isRiskLevel, type GenerationResponse, type RiskLevel } from "./contract";
import {
  assessInputRisk,
  crisisResponse,
  detectLanguage,
  inspectOutput,
  type RiskAssessment,
} from "./guardrails";
import {
  localModelClient,
  workersAiClient,
  type AiBinding,
  type ModelClient,
} from "./model";
import { PROMPT_VERSION, readMode, type Mode } from "./prompts";
import { retrieve, type Excerpt } from "./retrieval";

export const MODEL_ID = "@cf/meta/llama-3.3-70b-instruct-fp8-fast";
export const OFFLINE_MODEL_VERSION = "offline-fallback";
export const CRISIS_MODEL_VERSION = "crisis-path-v1";

export type { AiBinding } from "./model";
export { extractModelText } from "./model";

export interface Env {
  /** Platform binding declared as [ai] in wrangler.toml. No key lives in the repo. */
  AI?: AiBinding;
  /** Optional non-secret override for the model id. */
  MODEL_ID?: string;
  /** "local" enables the deterministic dev adapter; never set this in production. */
  DEV_MODEL?: string;
  /** Platform rate limiting binding declared as [[ratelimits]] in wrangler.toml. */
  RATE_LIMITER?: RateLimiter;
}

/** The shape of the Workers Rate Limiting binding, minus the Cloudflare types. */
export interface RateLimiter {
  limit(options: { key: string }): Promise<{ success: boolean }>;
}

export type GenerationKind = "reframe" | "chat" | "summarise";

const FIXTURE = fixture as unknown as GenerationResponse;

const CANNED_CHAT_REPLY =
  "I hear you. The companion cannot reach the model right now, so this is the offline reply. Nothing you wrote has left this device.";

const CANNED_SUMMARY =
  "This is the offline summary. The model was not reached, so the session was not summarised; everything you recorded is still on your device.";

const MAX_INPUT = 2000;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function cloneFixture(): GenerationResponse {
  return JSON.parse(JSON.stringify(FIXTURE)) as GenerationResponse;
}

function readStringField(body: unknown, key: string): string {
  if (!isRecord(body)) return "";
  const value = body[key];
  if (typeof value !== "string") return "";
  return value.trim().slice(0, MAX_INPUT);
}

function readTranscript(body: unknown): string {
  if (!isRecord(body)) return "";
  const transcript = body.transcript;
  if (typeof transcript === "string") return transcript.trim().slice(0, MAX_INPUT);
  if (!Array.isArray(transcript)) return "";

  return transcript
    .map((item) => {
      if (typeof item === "string") return item;
      if (isRecord(item) && typeof item.content === "string") return item.content;
      return "";
    })
    .filter((line) => line.length > 0)
    .join("\n")
    .slice(0, MAX_INPUT);
}

function readUserText(kind: GenerationKind, body: unknown): string {
  if (kind === "reframe") {
    return readStringField(body, "thought") || readStringField(body, "message");
  }
  if (kind === "chat") {
    return readStringField(body, "message") || readStringField(body, "thought");
  }
  return readTranscript(body) || readStringField(body, "notes");
}

function fallback(kind: GenerationKind, risk: RiskAssessment): GenerationResponse {
  const response = cloneFixture();
  response.prompt_version = PROMPT_VERSION;
  response.model_version = OFFLINE_MODEL_VERSION;
  response.risk_level = { level: risk.level, confidence: risk.confidence };

  // The fixture's citations name documents the corpus no longer carries, and a
  // fallback has retrieved nothing, so it claims no sources at all. The schema
  // allows an empty array, and a client that treats "no sources" as ungrounded
  // (as the Android client does) then shows its own offline copy instead of a
  // citation it cannot resolve.
  response.source_ids = [];

  // Keep the existing offline copy. Request text is never interpolated here.
  if (kind === "chat") response.message = CANNED_CHAT_REPLY;
  if (kind === "summarise") response.message = CANNED_SUMMARY;
  return response;
}

/** Trim to the schema's maxLength without cutting mid-sentence when possible. */
function clampText(value: unknown, maxLength: number): string | null {
  if (typeof value !== "string") return null;
  const text = value.trim().replace(/\s+/g, " ");
  if (text.length === 0) return null;
  if (text.length <= maxLength) return text;

  const cut = text.slice(0, maxLength);
  const stop = Math.max(
    cut.lastIndexOf(". "),
    cut.lastIndexOf("! "),
    cut.lastIndexOf("? "),
  );
  if (stop > maxLength * 0.6) return cut.slice(0, stop + 1).trim();

  const wordBoundary = cut.lastIndexOf(" ");
  return (wordBoundary > 0 ? cut.slice(0, wordBoundary) : cut).trim().slice(0, maxLength);
}

type NormalisedDraft = Omit<GenerationResponse, "prompt_version" | "model_version">;

/**
 * Coerce the model's draft into the frozen shape. Anything that cannot be
 * represented without inventing a field returns null and the caller falls back.
 */
function normaliseDraft(
  draft: unknown,
  excerpts: Excerpt[],
  floor: RiskAssessment,
): NormalisedDraft | null {
  if (!isRecord(draft)) return null;

  const message = clampText(draft.message, 600);
  const distortion = clampText(draft.distortion, 80);
  if (!message || !distortion) return null;

  const rawPerspectives = Array.isArray(draft.perspectives) ? draft.perspectives : [];
  const perspectives = rawPerspectives
    .map((value) => clampText(value, 300))
    .filter((value): value is string => value !== null)
    .slice(0, 3);
  if (perspectives.length < 2) return null;

  const microAction = isRecord(draft.micro_action) ? draft.micro_action : null;
  if (!microAction) return null;
  const title = clampText(microAction.title, 120);
  const rawMinutes =
    typeof microAction.minutes === "number" && Number.isFinite(microAction.minutes)
      ? Math.round(microAction.minutes)
      : null;
  if (!title || rawMinutes === null) return null;
  const minutes = Math.min(15, Math.max(1, rawMinutes));

  const allowed = new Set(excerpts.map((excerpt) => excerpt.sourceId));
  const rawIds = Array.isArray(draft.source_ids) ? draft.source_ids : [];
  const sourceIds = [
    ...new Set(
      rawIds.filter(
        (value): value is string => typeof value === "string" && allowed.has(value),
      ),
    ),
  ].slice(0, 3);
  if (sourceIds.length === 0) return null;

  const risk = isRecord(draft.risk_level) ? draft.risk_level : null;
  const level: RiskLevel = isRiskLevel(risk?.level) ? risk.level : "L0";
  const confidence =
    typeof risk?.confidence === "number" && Number.isFinite(risk.confidence)
      ? Math.min(1, Math.max(0, risk.confidence))
      : 0.5;
  const finalLevel = maxRisk(floor.level, level);

  return {
    message,
    distortion,
    perspectives,
    micro_action: { title, minutes },
    source_ids: sourceIds,
    risk_level: {
      level: finalLevel,
      confidence: finalLevel === floor.level ? floor.confidence : confidence,
    },
  };
}

function renderedText(draft: NormalisedDraft): string {
  return [draft.message, draft.distortion, ...draft.perspectives, draft.micro_action.title].join(
    "\n",
  );
}

/** Liveness only: the corpus and the prompt module are loaded, so the pipeline is routable. */
export function health(): { ok: true } {
  return { ok: true };
}

function selectModelClient(env: Env): ModelClient | null {
  if (env.DEV_MODEL?.trim().toLowerCase() === "local") return localModelClient();
  const ai = env.AI;
  if (!ai) return null;
  return workersAiClient(ai, env.MODEL_ID?.trim() || MODEL_ID);
}

export async function runGeneration(
  kind: GenerationKind,
  body: unknown,
  env: Env = {},
): Promise<GenerationResponse> {
  const mode = readMode(body);
  let floor: RiskAssessment = { level: "L0", confidence: 0.5 };

  try {
    const userText = readUserText(kind, body);
    floor = assessInputRisk(userText);

    if (floor.level === "L3") return crisisResponse(detectLanguage(userText));
    if (userText.length === 0) return fallback(kind, floor);

    const excerpts = retrieve(userText, 3);
    if (excerpts.length === 0) return fallback(kind, floor);

    const client = selectModelClient(env);
    if (!client) return fallback(kind, floor);

    const draft = await client.generate({ kind, mode, userText, excerpts });
    if (!draft) return fallback(kind, floor);

    const normalised = normaliseDraft(draft, excerpts, floor);
    if (!normalised) return fallback(kind, floor);

    if (normalised.risk_level.level === "L3") {
      return crisisResponse(detectLanguage(userText));
    }

    const guardrail = inspectOutput(renderedText(normalised));
    if (!guardrail.ok) return fallback(kind, floor);

    return {
      ...normalised,
      prompt_version: PROMPT_VERSION,
      model_version: client.version,
    };
  } catch {
    return fallback(kind, floor);
  }
}
