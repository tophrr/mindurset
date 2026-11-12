// Guardrails around the generation path.
//
// Two directions:
//  - input: a local risk floor that mirrors the Android LocalRiskFloor ladder,
//    so an explicit L3 intent never reaches the model;
//  - output: reject rendered text that names medication or a diagnosis, or that
//    gives instructions for self-harm. Rejection means the caller returns the
//    offline fallback, never a 500.
//
// This is a floor, not a classifier. A miss proves nothing, which is why the
// crisis path is also triggered by the model's own L3 report.

import type { GenerationResponse, RiskLevel } from "./contract";
import { PROMPT_VERSION } from "./prompts";

export interface RiskAssessment {
  level: RiskLevel;
  confidence: number;
}

interface RiskPattern {
  id: string;
  level: Exclude<RiskLevel, "L0">;
  regexes: RegExp[];
}

const RISK_PATTERNS: RiskPattern[] = [
  {
    id: "en-intent",
    level: "L3",
    regexes: [
      /\bkill(?:ing)? myself\b/i,
      /\bend(?:ing)? my life\b/i,
      /\bcommit suicide\b/i,
      /\btake my own life\b/i,
    ],
  },
  {
    id: "id-intent",
    level: "L3",
    regexes: [
      /\bbunuh diri\b/i,
      /\bmengakhiri hidup\b/i,
      /\blebih baik (?:aku )?mati\b/i,
      /\bmati saja\b/i,
    ],
  },
  {
    id: "en-hopeless",
    level: "L2",
    regexes: [
      /\bwants? to die\b/i,
      /\bsuicidal\b/i,
      /\bbetter off (?:if .{0,24}?(?:was|were|is) )?dead\b/i,
      /\bno reason to live\b/i,
      /\bnothing to live for\b/i,
      /\bhurt(?:ing)? myself\b/i,
      /\bpoint of waking up\b/i,
      /\bempty and meaningless\b/i,
      /\bdon'?t want to (?:be here|live)\b/i,
    ],
  },
  {
    id: "id-hopeless",
    level: "L2",
    regexes: [
      /\bingin mati\b/i,
      /\bmau mati\b/i,
      /\bmenyakiti diri\b/i,
      /\btidak (?:ada alasan|ingin) (?:untuk )?hidup\b/i,
      /\bjadi beban\b/i,
      /\bputus asa\b/i,
    ],
  },
];

/** The strongest local signal in the text. L0 when nothing matched. */
export function assessInputRisk(text: string): RiskAssessment {
  let level: RiskLevel = "L0";
  let confidence = 0.5;

  for (const pattern of RISK_PATTERNS) {
    if (!pattern.regexes.some((regex) => regex.test(text))) continue;
    if (pattern.level === "L3") return { level: "L3", confidence: 0.95 };
    level = "L2";
    confidence = 0.8;
  }

  return { level, confidence };
}

const INDONESIAN_MARKERS =
  /\b(?:aku|saya|kamu|anda|tidak|nggak|gak|mau|ingin|banget|sekali|sedih|cemas|takut|lelah|capek|tidur|gagal|ujian|besok|hari|yang|dan|diriku|sendiri)\b/i;

export function detectLanguage(text: string): "en" | "id" {
  return INDONESIAN_MARKERS.test(text) ? "id" : "en";
}

const MEDICATION =
  /\b(?:sertraline|fluoxetine|escitalopram|citalopram|paroxetine|venlafaxine|bupropion|mirtazapine|alprazolam|clonazepam|lorazepam|diazepam|xanax|prozac|zoloft|lexapro|celexa|paxil|effexor|wellbutrin|antidepressants?|ssris?|benzodiazepines?|antipsychotics?|mood stabilizers?|medication|medicine|obat|dosis|dosage|miligram|mg)\b/i;

const DIAGNOSIS =
  /\b(?:diagnos(?:e|ed|es|ing|is|tic)|disorders?|syndromes?|ptsd|ocd|adhd|bipolar|schizophreni(?:a|c)|major depressive|generalized anxiety|clinical depression|depression|panic disorder|eating disorder|substance use disorder|gangguan|depresi|skizofrenia)\b/i;

const SELF_HARM_INSTRUCTION: RegExp[] = [
  /\b(?:how to|ways? to|steps? to|instructions? (?:for|to))\b[^.]{0,60}\b(?:kill|hurt|harm|cut|hang|strangle|poison|overdose)\b/i,
  /\b(?:you (?:should|could|can|might)|try to|go ahead and)\b[^.]{0,40}\b(?:kill|hurt|harm|cut)\s+(?:yourself|your self)\b/i,
  /\b(?:cut|hurt|harm|kill)\s+yourself\s+(?:now|tonight|to (?:feel|cope|relieve))\b/i,
  /\b(?:take|swallow|ingest)\s+(?:\d+|a (?:handful|bottle|pack)|too many|a lot of)\s+(?:pills?|tablets?|medication)\b/i,
  /\boverdose\s+(?:on|with)\b/i,
  /\b(?:hang|strangle)\s+yourself\b/i,
  /\b(?:jump|throw yourself)\s+(?:off|from)\b/i,
  /\b(?:cara|gimana|bagaimana)\b[^.]{0,40}\b(?:bunuh diri|menyakiti diri|melukai diri)\b/i,
];

export type GuardrailReason = "medication" | "diagnosis" | "self-harm-instruction";

export type GuardrailResult =
  | { ok: true }
  | { ok: false; reason: GuardrailReason };

/** Scan every piece of rendered copy, not only the message field. */
export function inspectOutput(text: string): GuardrailResult {
  if (MEDICATION.test(text)) return { ok: false, reason: "medication" };
  if (DIAGNOSIS.test(text)) return { ok: false, reason: "diagnosis" };
  if (SELF_HARM_INSTRUCTION.some((regex) => regex.test(text))) {
    return { ok: false, reason: "self-harm-instruction" };
  }
  return { ok: true };
}

const CRISIS_COPY = {
  en: {
    message:
      "What you wrote sounds like more than a hard day. If you are thinking about hurting yourself, talking to someone now matters more than anything in this app. Call 119 ext. 8, or 112 if you are in immediate danger.",
    distortion: "safety first",
    perspectives: [
      "You do not have to sit with this alone.",
      "A person you trust is enough; you do not have to explain everything.",
    ],
    action: "Call 119 ext. 8 or a person you trust",
  },
  id: {
    message:
      "Yang kamu tulis terdengar lebih berat dari hari yang biasa. Kalau kamu sedang berpikir untuk menyakiti diri, berbicara dengan seseorang sekarang lebih penting daripada apa pun di aplikasi ini. Telepon 119 ext. 8, atau 112 kalau kamu dalam bahaya langsung.",
    distortion: "keselamatan dulu",
    perspectives: [
      "Kamu tidak harus menanggung ini sendirian.",
      "Satu orang yang kamu percaya sudah cukup; kamu tidak perlu menjelaskan semuanya.",
    ],
    action: "Telepon 119 ext. 8 atau orang yang kamu percaya",
  },
} as const;

/**
 * The only answer allowed when the risk is L3. It stays inside contract v1
 * because the client still parses one shape, and it is authored copy: it never
 * goes through the model or the output guardrail.
 */
export function crisisResponse(language: "en" | "id"): GenerationResponse {
  const copy = CRISIS_COPY[language];
  return {
    message: copy.message,
    distortion: copy.distortion,
    perspectives: [...copy.perspectives],
    micro_action: { title: copy.action, minutes: 5 },
    source_ids: ["safety-plan#immediate-support", "safety-plan#trusted-person"],
    risk_level: { level: "L3", confidence: 0.95 },
    prompt_version: PROMPT_VERSION,
    model_version: "crisis-path-v1",
  };
}
