# Worker

The Cloudflare Worker that will sit between the Android client and the model. Today it is a stub:
fixed, contract-shaped JSON, no model calls, no secrets, no bindings.

## Run it

```bash
cd worker
npm ci
npm test          # vitest, 9 cases
npx tsc --noEmit  # type check
npm run dev       # wrangler dev on http://localhost:8787
```

`GET /v1/health` returns `{"ok":true}`. `POST /v1/reframe`, `/v1/chat` and `/v1/summarise` return the
fixture from `contract/fixtures/generation-ok.json`, which is imported rather than copied.

## The contract

`contract/generation-v1.schema.json` is the authority for the response shape. The tests compile it
with Ajv and validate the fixture and every endpoint response against it, so a field that is not in
the schema cannot appear and a fixture that drifts fails CI.

Changing the shape means a new `v2` file, a new fixture and a pull request with two readers. Do not
edit `v1` in place: devices in the field still parse it.

## What the stub deliberately does not do

- No model call, no prompt, no retrieval. `message` is canned text.
- No risk classification. The client's `LocalRiskFloor` and the future classifier own that.
- No persistence. Nothing is stored between requests.
