// Mind-Ur-Set Worker.
//
// The HTTP surface is unchanged from the stub: the same four paths, the same
// contract/generation-v1.schema.json response shape, so the Android client
// needs no code change to start receiving real answers. The stub's fixed JSON
// is now the offline fallback, not the normal path.
//
// Real generation runs through Workers AI behind the `AI` platform binding. A
// missing binding, a model error, malformed model output, or a guardrail hit
// all return a contract-valid 200 with model_version "offline-fallback".

import { health, runGeneration, type Env, type GenerationKind } from "./pipeline";

export type { GenerationResponse } from "./contract";

function json(body: unknown, status = 200, extraHeaders: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...extraHeaders },
  });
}

/**
 * The key a generation request is counted against.
 *
 * Once the client sends an account uid (the Firebase work in the proposal) that
 * is the key. Until then the edge client address is the only stable identifier
 * a request carries; Cloudflare discourages IP keys because a mobile network
 * shares one address across many people, so the limit below is deliberately
 * loose and exists to stop a runaway loop, not to police users.
 */
function rateLimitKey(request: Request): string {
  const uid = request.headers.get("x-user-id")?.trim();
  if (uid) return `uid:${uid}`;
  const ip = request.headers.get("cf-connecting-ip")?.trim();
  return `ip:${ip && ip.length > 0 ? ip : "anonymous"}`;
}

function generationKind(pathname: string): GenerationKind | null {
  if (pathname === "/v1/reframe") return "reframe";
  if (pathname === "/v1/chat") return "chat";
  if (pathname === "/v1/summarise") return "summarise";
  return null;
}

export default {
  async fetch(request: Request, env: Env = {}): Promise<Response> {
    const { pathname } = new URL(request.url);
    const method = request.method.toUpperCase();

    if (pathname === "/v1/health") {
      return method === "GET"
        ? json(health())
        : json({ error: "method_not_allowed" }, 405);
    }

    const kind = generationKind(pathname);
    if (kind !== null) {
      if (method !== "POST") {
        return json({ error: "method_not_allowed" }, 405);
      }

      const limiter = env.RATE_LIMITER;
      if (limiter) {
        const { success } = await limiter.limit({ key: rateLimitKey(request) });
        if (!success) {
          return json({ error: "rate_limited" }, 429, { "retry-after": "60" });
        }
      }

      let body: unknown;
      try {
        body = await request.json();
      } catch {
        return json({ error: "invalid_json" }, 400);
      }

      return json(await runGeneration(kind, body, env));
    }

    return json({ error: "not_found" }, 404);
  },
};
