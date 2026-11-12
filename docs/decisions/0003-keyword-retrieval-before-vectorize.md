# 0003: Keyword retrieval before a vector index

Date: 2026-11-11
Status: accepted
Deciders: Christopher M. M. Gijoh (project manager)
Related: 0001, 0002; proposal Sec. 9.1 and deliverable 14.3

## Context

The proposal describes retrieval over a Cloudflare Vectorize index built from the wellbeing corpus,
with an embedding step before generation, and deliverable 14.3 lists the Vectorize index among the
things the submission ships.

The Worker that landed corpus v1 (pull request #27) retrieves with a deterministic keyword score and
a small bilingual synonym table. That choice was not recorded anywhere, so the next reader finds a
gap between the document and the code and cannot tell whether it is a decision or an omission.

Three facts pushed the decision this way:

1. Corpus v1 is seven markdown files. A keyword score over that corpus returns the same excerpts a
   small embedding model would, and a reviewer can read the scoring rules in one screen.
2. An embedding step needs a second model, a Vectorize index and a sync job between the corpus file
   and the index. None of that exists yet, and the account that will host it belongs to the project
   manager, not to the team member who owns the Worker.
3. Grounding is enforced regardless of the retrieval method: the response only accepts source ids
   that came back from retrieval, and a client that receives no sources treats the answer as
   ungrounded.

## Decision

Retrieval stays keyword-based for the R4 build. The interface it exposes, `retrieve(query, k)`
returning excerpts with stable `<topic>#<section>` ids, is the same interface a vector index would
sit behind, so replacing the implementation does not change the pipeline, the prompt or the
contract.

This is recorded as a deliberate deviation, not a shipped equivalence: the proposal's Vectorize row
and the matrix note it, and the submission says keyword retrieval when it describes what runs.

## Consequences

- The tests that push every red-team sentence through retrieval also cover the retrieval step, which
  a hosted index could not offer at this size.
- Paraphrase that shares no token with the corpus falls back instead of retrieving: the failure mode
  is "offline exercise", not "wrong citation". NFR-02 (first token under three seconds p95) remains
  the number to measure before any claim about the online path.
- Adding a vector index later means one new module behind `retrieve`, an index sync step and a
  fixture for the evaluation run; it does not mean a contract change or a client change.
- Deliverable 14.3 must either name what exists or the document must say the index is planned. It
  cannot keep claiming an index that is not there.
