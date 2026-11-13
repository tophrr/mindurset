# 0007 — The ASQ step asks the published items and keeps no answers

- **Status:** Accepted
- **Date:** 13 November 2026
- **Related:** `docs/testing/safety-suite.md` (Sec. 3.7), ADR 0005, ADR 0006

## Context

The ladder sends ambiguous hopelessness (L2) to clarifying questions before it decides. Until now the
client skipped that rung: an L2 note went straight to the crisis card, which is wider than the proposal
allows and hides the "no" branch from the person.

The instrument is the NIMH Ask Suicide-Screening Questions set: four items, plus an acuity probe when
any of the four is answered yes.

## Decision

1. **The four items are asked in full, then the acuity probe** when any answer was yes. The instrument is
   designed to be administered whole; jumping to the acuity question after the first yes would drop
   items 3 and 4 from the record and from the conversation.
2. **Any yes reaches L3.** The ASQ is a screen, not a severity scale, and the proposal sends every
   positive screen to the crisis card. The acuity answer decides the copy (imminent or not), not the
   level.
3. **All four no answers save the check-in that was held back.** The note is already typed and held in
   the ViewModel, so a negative screen stores it and leaves the flow where the person expected to be.
4. **No answer is stored.** The flow produces a level and nothing else: no ASQ rows, no export entries,
   no log lines. What is recorded is one `SafetyEvent` at L2 when the note is flagged, and one at L3 with
   trigger `asq_positive` if the screen comes back positive.
5. **The English wording is the published instrument.** The Indonesian wording is a project translation
   and **has not been validated clinically**; the PR that introduced it says so, and a review from a
   clinician or the course supervisor is still wanted. Nothing else in this decision depends on the
   translation being final.

## Consequences

- The check-in form is replaced by one question at a time while the step is open; leaving the screen
  saves nothing, which is the same rule as a flagged note.
- Test plan Sec. 3.7 stops being `NOT AVAILABLE` for the check-in path. The journal and Companion paths
  still go straight to the card, and remain open items in
  `docs/testing/runs/20261112-safety-suite.md`.
- `AsqFlow` is a pure object: no Android imports, so the whole matrix (positive, negative, imminent,
  re-answered question) runs on the JVM in CI.

## What this does not decide

- Whether the same step belongs on the Companion path, where the reply is conversational rather than a
  saved entry.
- The clinical review of the Indonesian translation, which is a person's task, not a code change.
