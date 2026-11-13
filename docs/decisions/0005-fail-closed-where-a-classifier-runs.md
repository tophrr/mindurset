# 0005 — Fail closed only where a classifier is consulted

- **Status:** Accepted
- **Date:** 13 November 2026
- **Supersedes:** nothing
- **Related:** `docs/testing/safety-suite.md` Sec. 3, `docs/testing/runs/20261112-safety-suite.md`, ADR 0001

## Context

Proposal Sec. 9.4 says a failure to classify must escalate rather than let a turn pass: if the remote
classifier is unavailable, the local floor's level goes up one tier. The safety suite restates it as
two rows — a clean note during an outage reaches L1, a flagged note during an outage reaches L3.

The client has two kinds of text path, and only one of them can fail this way:

| Path | Talks to a classifier? | What an outage changes |
| --- | --- | --- |
| Check-in note, journal entry | No. `LocalRiskFloor` decides, on the device. | Nothing. The decision never leaves the device. |
| Companion message | Yes, through the Worker behind contract v1. | The reply never arrives, so no classifier answer exists. |

Reading the rule literally over both paths would mean every check-in note escalates to L1 whenever the
phone has no network, which would place a helpline banner under every ordinary entry. That is not
fail-closed behaviour; it is noise that trains people to ignore the banner.

## Decision

1. **Escalation applies where a classifier is actually consulted.** `RiskScreening.screenLocal` is used
   by the check-in and journal paths and never escalates; `RiskScreening.screenWith` is used by the
   Companion and escalates one tier when the call fails, times out, or returns no usable level.
2. **A classifier may only raise the floor.** `max(floor, classifier)`, never lower.
3. **An escalated L1 does not halt.** The Companion keeps the offline exercise and adds a card with the
   helpline. An escalated L2 becomes L3 and halts with the crisis card, because `escalate()` moves L2 to
   L3.
4. **A flagged note halts before the network is touched.** A Companion line that the floor puts at L2
   or L3 shows the card and is never sent, so the outage rule cannot apply to it — the person is
   already at the card. The suite's second row is satisfied by construction rather than by escalation.

## Consequences

- The safety suite's row 1 (clean note, outage) now passes on the Companion path and is not applicable
  to check-in, which consults nothing. Row 2 (flagged note, outage) is satisfied by the pre-network
  halt.
- `ScreeningSource` records which layers produced a level, so a later report can tell "raised because
  the classifier failed" from "raised because the classifier said so".
- The token cost of a failed call is unchanged: nothing is retried automatically.

## What this does not decide

- The ASQ screen (L2 branch) and the trusted contact sheet (S-91) are still missing; both are separate
  open items in `docs/testing/runs/20261112-safety-suite.md`.
- Whether a *deployed* classifier should also see check-in notes is a privacy question for the proposal,
  not a client decision. Until it is answered, the client keeps them on the device.
