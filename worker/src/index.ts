// Mind-Ur-Set Worker, contract stub.
//
// Returns fixed, contract-shaped JSON for the four endpoints the Android client
// needs to test against. No AI, no model calls, no secrets, no bindings.
//
// The response shape is contract/generation-v1.schema.json, and the payload is
// loaded straight from contract/fixtures/generation-ok.json rather than copied,
// so this stub cannot drift away from the frozen contract. If a field is not in
// the fixture it is not in the contract, and this file must not invent one.

import fixture from "../../contract/fixtures/generation-ok.json";

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
    level: "L0" | "L1" | "L2" | "L3";
    confidence: number;
  };
  prompt_version: string;
  model_version: string;
}

const GENERATION = fixture as GenerationResponse;

const CANNED_CHAT_REPLY =
  "I hear you. This is the stub companion reply; nothing is generated yet.";

const CANNED_SUMMARY =
  "This is the stub session summary. The session covered one check-in and one reframe. Nothing is generated yet.";

const DEFAULT_MODE = "listen";

function json(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8" },
  });
}

// The contract has no room for a mode field (additionalProperties: false), so
// the requested support mode is echoed inside the message text. The cap keeps
// message inside the contract's maxLength of 600.
function readMode(body: unknown): string {
  if (typeof body === "object" && body !== null && "mode" in body) {
    const mode = (body as { mode: unknown }).mode;
    if (typeof mode === "string" && mode.length > 0) {
      return mode.slice(0, 40);
    }
  }
  return DEFAULT_MODE;
}

export default {
  async fetch(request: Request): Promise<Response> {
    const { pathname } = new URL(request.url);
    const method = request.method.toUpperCase();

    if (pathname === "/v1/health") {
      return method === "GET"
        ? json({ ok: true })
        : json({ error: "method_not_allowed" }, 405);
    }

    const isGenerationPath =
      pathname === "/v1/reframe" ||
      pathname === "/v1/chat" ||
      pathname === "/v1/summarise";

    if (isGenerationPath) {
      if (method !== "POST") {
        return json({ error: "method_not_allowed" }, 405);
      }
      let body: unknown;
      try {
        body = await request.json();
      } catch {
        return json({ error: "invalid_json" }, 400);
      }

      if (pathname === "/v1/reframe") {
        return json({ ...GENERATION });
      }
      if (pathname === "/v1/chat") {
        const mode = readMode(body);
        return json({
          ...GENERATION,
          message: `${CANNED_CHAT_REPLY} (mode: ${mode})`,
        });
      }
      return json({ ...GENERATION, message: CANNED_SUMMARY });
    }

    return json({ error: "not_found" }, 404);
  },
};
