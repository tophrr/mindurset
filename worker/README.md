# Worker

The Cloudflare Worker between the Android client and the model. It serves the frozen
`generation-v1` response shape. The normal path is a real generation run: keyword retrieval over the
bundled corpus, prompt v1, Workers AI, then guardrails. The stub's fixed JSON is now the offline
fallback, not the normal answer.

## Run it

```bash
cd worker
npm ci
npm test          # vitest: contract + pipeline cases
npx tsc --noEmit  # type check
npm run dev       # wrangler dev on http://localhost:8787
```

`GET /v1/health` returns `{"ok":true}`. `POST /v1/reframe`, `/v1/chat` and `/v1/summarise` keep the
same request paths and the same response shape as before, so the client does not need a code change
to start receiving generated answers.

## The model binding

The Worker uses the Workers AI **platform binding named `AI`**, declared in `wrangler.toml`:

```toml
[ai]
binding = "AI"
```

There is no API key, token or `.dev.vars` in this repository, and there must never be one. Live
inference in local development needs an authenticated Wrangler session (`npx wrangler login`); the
binding reads the account from that session. To use a different model without a code change, set a
non-secret `MODEL_ID` variable in the Worker environment; the default is
`@cf/meta/llama-3.3-70b-instruct-fp8-fast`.

Without a binding or a successful model call, every generation endpoint returns HTTP 200 with
`model_version: "offline-fallback"` and the existing offline copy. A 500 is not a fallback.

## Local verification without Workers AI

`wrangler dev --local` disables remote bindings, so `env.AI` is not available in that mode. To
exercise retrieval, prompt input, normalisation and guardrails without an account, start the
deterministic local adapter explicitly:

```bash
npx wrangler dev --local --var DEV_MODEL:local
```

Every response then carries `model_version: "local-stub-v1"`. That adapter is a dev stub: it does
not call a model and must never be enabled in production. Without `DEV_MODEL`, a missing binding
still returns `offline-fallback`.

## Pipeline

| File | Role |
| --- | --- |
| `src/corpus/` | Corpus v1: one markdown file per topic plus `version.json` |
| `src/retrieval.ts` | Deterministic keyword retrieval, top-k excerpts, real source ids |
| `src/prompts.ts` | Prompt text at `v1.0.0`, exported with its version string |
| `src/guardrails.ts` | Local risk floor, medication/diagnosis/self-harm output checks, crisis copy |
| `src/model.ts` | `ModelClient` boundary: Workers AI client plus the deterministic local adapter |
| `src/pipeline.ts` | Orchestration, normalisation into the frozen shape, fallback |
| `src/index.ts` | The four HTTP endpoints |

Source ids have the form `<topic>#<section>`, for example `stress#exam-pressure`. The corpus version
is in `src/corpus/version.json`; the prompt version is `PROMPT_VERSION` in `src/prompts.ts`.

A request that matches the local L3 floor (explicit intent to end one's life, in English or
Indonesian) never reaches the model. It returns the crisis path with `model_version:
"crisis-path-v1"` and the same contract shape. A model that reports L3 is replaced the same way.

## The contract

`contract/generation-v1.schema.json` is the authority for the response shape. The tests compile it
with Ajv and validate the fixture and every endpoint response against it, so a field that is not in
the schema cannot appear and a fixture that drifts fails CI.

Changing the shape means a new `v2` file, a new fixture and a pull request with two readers. Do not
edit `v1` in place: devices in the field still parse it. The model draft is normalised into the
existing fields; if it cannot fit, the request falls back rather than inventing a field.

## What it deliberately does not do

- No persistence. Nothing is stored between requests.
- No user accounts, rate limiting or analytics.
- No risk classification beyond the local floor and the model's own report. The Android client's
  `LocalRiskFloor` remains the first line and never lowers its level.
- No streaming. The response is one JSON object.
