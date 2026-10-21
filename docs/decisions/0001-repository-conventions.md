# 0001: Repository conventions, access model and quality gates

Date: 2026-10-21
Status: accepted
Deciders: Christopher M. M. Gijoh (project manager)

## Context

The repository was written by one person and several AI agents, with no collaboration surface: a
single GitHub collaborator, an unprotected `main`, no contribution rules, no pull request template
and no formatting or secret checks. The project now moves to four people plus agents, and the
proposal (Sec. 13.4) requires decisions to be recorded in the repository and every pull request to
have a second reader.

Three facts forced this decision:

1. With one collaborator, teammates cannot push or open pull requests at all.
2. `main` accepts force pushes, and history was rewritten three times in one week.
3. The pitfalls that broke this build before (built-in Kotlin in AGP 9, the daemon toolchain file,
   edge-to-edge keyboard handling, tab back stack) lived only in one person's notes, invisible to
   anyone else's tools.

## Decision

1. Teammates join as collaborators with write access; `main` accepts changes only through pull
   requests with one approval, linear history, conversation resolution, and no force push.
2. Agents may commit and push to non-main branches. They never push, amend, rebase or force-push
   `main`, and their commits carry an `Assisted-by:` trailer.
3. One `AGENTS.md` at the repository root is the agent contract, written in English. It carries the
   toolchain pins, the commands, the hard bans, the pitfalls that already cost time, and the
   definition of done.
4. `CONTRIBUTING.md` is the human contract: branch model, commit format, review rules, secrets.
5. Commits follow `type(scope): subject`, at most 72 characters, with `Refs: F-xx, UC-xx` when the
   change closes a traceability row. Merge is squash only.
6. Quality gates block merges: build, unit tests, ktlint through Spotless, Android lint, and a
   secret scan.
7. Documents return to the repository in a narrow form: `docs/decisions/` for these records and
   `docs/testing/` for test plans and device evidence. Course documents stay outside the repository.

## Consequences

- Every change now costs a branch, a pull request and a reviewer. That is the intended cost: a
  second reader is how a four-person team keeps one architecture instead of four.
- The project manager loses the ability to rewrite `main`. Emergency bypass stays possible through
  the GitHub UI and must be recorded in the pull request.
- Formatting is enforced by a tool, not by taste, so agent-generated diffs stay reviewable.

## Alternatives considered

- **Fork and pull request without collaborator access.** Rejected: branches cannot be shared, so
  pairing and review on the same branch become impossible.
- **Single writer, everyone else sends patches.** Rejected: it serialises the whole team behind one
  person and makes the gate in Sec. 13.4 decorative.
- **No formatter, rely on review.** Rejected: agents produce consistent style only when a tool
  defines it.
