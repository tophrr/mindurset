// The TypeScript mirror of contract/generation-v1.schema.json. The schema is
// the authority; this interface only exists so the Worker can be type-checked.
// Do not add a field here that the schema does not allow: additionalProperties
// is false on both sides.

export type RiskLevel = "L0" | "L1" | "L2" | "L3";

export interface GenerationResponse {
  message: string;
  distortion: string;
  perspectives: string[];
  micro_action: {
    title: string;
    minutes: number;
  };
  source_ids: string[];
  risk_level: {
    level: RiskLevel;
    confidence: number;
  };
  prompt_version: string;
  model_version: string;
}

const RISK_ORDER: Record<RiskLevel, number> = {
  L0: 0,
  L1: 1,
  L2: 2,
  L3: 3,
};

export function isRiskLevel(value: unknown): value is RiskLevel {
  return value === "L0" || value === "L1" || value === "L2" || value === "L3";
}

/** The client never lowers the level it computed locally; the Worker applies the same rule. */
export function maxRisk(a: RiskLevel, b: RiskLevel): RiskLevel {
  return RISK_ORDER[a] >= RISK_ORDER[b] ? a : b;
}
