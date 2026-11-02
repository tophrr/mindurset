# Contract, version 1

One JSON shape for everything the app asks a model for: a reframe step, a companion reply, a session
summary. The Android client and the Cloudflare Worker both read these files, which is the only reason
the two sides cannot drift apart silently.

| File | Purpose |
| --- | --- |
| `generation-v1.schema.json` | JSON Schema, the authority. If a field is not here, it is not in the contract |
| `fixtures/generation-ok.json` | One valid response. Both test suites load this file rather than copying it |

Rules:

1. A response carries `source_ids` and a `risk_level`. A response without sources is not grounded,
   and the client must treat it as an offline fallback rather than as an answer.
2. `risk_level.level` uses the same ladder as the safety layer: `L0` to `L3`. The client never lowers
   the level it computed locally, only raises it.
3. `micro_action.minutes` is capped at 15, because the product promise is an action that fits in a
   quarter of an hour.
4. Changing any field, type or enum here means a new `v2` file, a new fixture, and a pull request
   with two readers. Do not edit `v1` in place: devices in the field still parse it.
