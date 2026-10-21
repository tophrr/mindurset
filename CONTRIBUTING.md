# Contributing to Mind-Ur-Set

This repository is worked on by four people, with AI tools in the loop. The rules below exist so
that we can all commit on the same day without stepping on each other, and so that every change can
be traced back to a row in the project traceability matrix. Whoever commits a change owns it: a tool
is never the author of record.

Agents read `AGENTS.md`. This file is the human version of the same contract.

## 0. First day

1. Accept the GitHub invitation. Until you do, you cannot push and you cannot open a pull request.
2. Clone, create `local.properties` with your SDK path, and turn on commit signing (section 1).
3. Run the build and test command in section 1. It must end in `BUILD SUCCESSFUL` with 7 unit tests.
4. Install the debug APK on your own phone and open it once.
5. Read `AGENTS.md` down to the section called Pitfalls, then come back to section 3.

If any of those five steps fails, say it in the team chat before you write any code. A person who
cannot build the project cannot review it, and reviewing is half of this project.

## 1. Before you start

| Requirement | How |
| --- | --- |
| JDK 21 or newer | Android Studio's bundled JBR is used by Gradle through `~/.gradle/gradle.properties` |
| Android SDK with platform 37 | `~/Android/Sdk`, set in `local.properties` (never committed) |
| Git signing | `git config --global commit.gpgsign true` with your own key, so your commits show as verified |
| Build lock | Every Gradle call: `flock /tmp/mindurset-gradle.lock ./gradlew ...` |

```bash
git clone https://github.com/tophrr/mindurset.git
cd mindurset
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

## 2. Branch model

`main` is append-only and protected. Nobody pushes to it directly, including the project manager.

| Branch | Use |
| --- | --- |
| `main` | Only receives merges from pull requests |
| `feat/<scope>-<slug>` | A feature, for example `feat/triage-offer-card` |
| `fix/<scope>-<slug>` | A bug fix |
| `test/<scope>-<slug>` | Tests only |
| `docs/<scope>-<slug>` | Documents and templates only |
| `agent/<tool>/<slug>` | Parallel agent work, one worktree per agent |

Rebase your branch on `main` before requesting review. Never merge `main` into a feature branch.

## 3. Commits

Format: `type(scope): subject`

- Types: `feat`, `fix`, `test`, `docs`, `chore`, `ci`, `refactor`, `perf`.
- Scope: the package or area, for example `checkin`, `nav`, `db`, `domain`, `ci`.
- Subject in the imperative, no full stop, at most 72 characters.
- Body only when the reason is not obvious from the diff.
- Last line: `Refs: F-03, UC-03` when the change closes traceability rows.
- One logical change per commit. Formatting commits are separate from behaviour commits.

## 4. Pull requests

1. Open the pull request against `main` with the template filled in.
2. Title follows the commit format.
3. CI must be green: build, unit tests, formatting, lint, secret scan.
4. One reviewer, at least a second reader who is not the author. The author never merges their own
   pull request, not even the project manager.
5. Merge with **squash**. The squash subject becomes the commit on `main`, so it must follow the
   commit format.
6. Delete the branch after merge.

A pull request that does not state which traceability rows it closes is sent back, however good the
code is.

## 5. Code rules

- Kotlin style is enforced by ktlint through Spotless: run `./gradlew spotlessApply` before pushing.
- Business rules live in `domain/` and `safety/` and must not import Android; they are tested on the
  JVM with JUnit.
- No hardcoded user-facing strings; add them to `app/src/main/res/values/strings*.xml`.
- The app is light-only for now. Do not add a dark theme, a theme switch or a colour that is not in
  the palette without design sign-off.
- Copy is non-punitive: no streaks, no missed-day counts, no red badges, no shaming.

## 6. Secrets and local files

Never commit any of these: `local.properties`, `*.jks`, `*.keystore`, `keystore.properties`,
`google-services.json`, `.env`, or a token of any kind. CI runs a secret scan on every pull request
and a hit blocks the merge. If you think a key leaked, tell the project manager the same day; we
rotate first and clean history second.

## 7. Documents

- Decisions: `docs/decisions/NNNN-<slug>.md`, one file per decision, never edited after it is
  accepted; supersede it with a new file instead.
- Test plans and device evidence: `docs/testing/`.
- Project documents (proposal, use case model, activity diagrams, traceability matrix, mockups) live
  in the private repository `tophrr/mindurset-docs`. Clone it and pull before you start; never copy
  those files into this repository, and never link a local path.

## 8. When something is blocked

Write it in the weekly status in one line: what is blocked, why, who can unblock it. Do not start a
second best-effort version of the same work in parallel; that is how we end up with two databases,
two navigation graphs and a merge nobody can review.
