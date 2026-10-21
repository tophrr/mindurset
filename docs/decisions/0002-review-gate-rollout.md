# 0002: Review gate rollout, staged

Date: 2026-10-21
Status: accepted
Deciders: Christopher M. M. Gijoh (project manager)
Related: 0001

## Context

Decision 0001 requires one approving review on every pull request. At the moment this decision was
written, the repository had exactly one collaborator, so a required approval would have blocked every
change, including the project manager's own, because GitHub does not let an author approve their own
pull request.

Leaving the requirement at zero and forgetting it is worse than either extreme: the pull request
template would promise a review the platform does not require.

## Decision

The protection rule on `main` is applied in two stages.

Stage 1, now: pull request required, linear history, conversation resolution, no force push, no
branch deletion, and three required status checks (formatting, build with lint and unit tests, secret
scan). Approving reviews are set to zero, because there is nobody else to approve yet.

Stage 2, after the four team members are added as collaborators: raise the required approving review
count to one, with `require_last_push_approval` so an approval goes stale if the author pushes again.

## Consequences

- Until stage 2, the second-reader rule lives in `CONTRIBUTING.md` and in the pull request template,
  not in GitHub. A reviewer can be requested and the pull request can be merged without one, which is
  a known gap, recorded here rather than hidden.
- The project manager keeps admin bypass. It exists for incidents, and every use must be written in
  the pull request that needed it.
- Stage 2 is a single API call and takes effect immediately, so it must not be forgotten: the weekly
  status carries it as an open item until it is done.

## Alternatives considered

- **Require one approval now.** Rejected: it blocks all work until invitations are accepted, and the
  temptation to disable protection in frustration is exactly how the rule dies.
- **No protection until the whole team is on GitHub.** Rejected: force pushes were rewriting `main`
  three times in one week; every day without protection is a day history can change under someone.
