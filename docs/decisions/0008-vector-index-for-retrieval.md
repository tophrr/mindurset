# 0008 — A vector index for retrieval, and when the build catches up

- **Status:** Accepted
- **Date:** 19 November 2026
- **Supersedes:** 0003
- **Related:** ADR 0003, `worker/src/retrieval.ts`, `worker/src/corpus/`, proposal Sec. 9.1, Sec. 9.2 and
  deliverable 14.3, NFR-02

## Context

ADR 0003 kept retrieval keyword-based for the R4 build: a deterministic score with a small bilingual
synonym table over seven markdown topics, behind `retrieve(query, k)`. It recorded that as a deliberate
deviation from the proposal, which promises a Cloudflare Vectorize index, and it said the documents
must not claim an index that does not exist.

On 19 November the project manager decided the other way: the vector index is the retrieval path the
project will implement, so the proposal stands and the build is what moves. The pull request that would
have rewritten the proposal to match the keyword retriever (`tophrr/mindurset-docs#49`) was closed
unmerged, and issue `tophrr/mindurset-docs#14`, which asked for that rewrite, is closed as not planned.
The deviation is now **scheduled**, not resolved by rewording.

## Decision

1. Retrieval moves to Workers AI multilingual embeddings with the index held in Cloudflare Vectorize.
   The proposal names `bge-m3` as the leading candidate; `bge-small-en-v1.5`, `bge-base-en-v1.5` and
   `bge-large-en-v1.5` stay excluded by name, because they are English-only and degrade the Indonesian
   half of the corpus without raising an error.
2. The pipeline interface does not change. `retrieve(query, k)` keeps returning excerpts with stable
   `<topic>#<section>` ids, so the prompt, the guardrails and contract v1 stay as they are. This is a
   module change, not a contract change.
3. Corpus markdown stays the source of truth. An index sync step, keyed on a content hash per topic and
   on `corpus/version.json`, rebuilds the index; the Worker keeps shipping the bundled corpus so the
   offline path and the existing tests do not depend on the index being reachable.
4. Grounding stays enforced on identifiers, not on similarity. An answer citing an id that did not come
   back from retrieval is still dropped. A similarity floor is an addition, not a replacement: below it
   the app falls back to the offline exercise rather than improvising.
5. Until the index is live, the shipped Worker and client retrieve by keyword. Anyone reading proposal
   Sec. 9.1, Sec. 9.2 or deliverable 14.3 must be told that the build lags the document; the traceability
   matrix carries that statement, and the work is a visible board item rather than something a reviewer
   has to discover.

## Consequences

- The matrix note changes from "a claim the build does not keep" to "a scheduled deviation", with the
  window it is scheduled in stated in the matrix rather than implied.
- The work needed: index creation on the project account, the embedding call, the sync job, the
  similarity floor, a retrieval test over paraphrase pairs that share no token with the corpus, and one
  evaluation run on the forty-case set before any claim about NFR-02 latency.
- Failure modes to keep in view: an index stale relative to the corpus markdown; a similarity floor so
  high that Indonesian queries fall through; an embedding model that is measurably better in English,
  which is why the evaluation is reported per language.
- ADR 0003 stays in the repository as the record of what the R4 build does. Only its `Status` line
  changes, to point here.
- `pgvector` on the VPS stays a development convenience for inspecting vectors, never the shipped index.

## Alternatives considered

- **Keep keyword retrieval and reword the proposal.** Rejected on 19 November: the vector index is what
  the team intends to implement, so the document describes the target, not the interim build.
- **Host the index on the VPS with pgvector instead of Vectorize.** Rejected: the Worker would need a
  second network hop plus a database credential that neither repository manages, and the free tier of
  Vectorize covers the planned volume.
- **Ship both paths and choose per query.** Rejected for this cycle: two retrieval paths means two
  failure modes to test and two numbers to report, and the evaluation set is not yet sized for that.
