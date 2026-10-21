# AGENTS.md

Instructions for AI coding agents working in this repository. A human version of the same rules
lives in `CONTRIBUTING.md`. Where the two disagree, this file wins.

This file is scoped to this repository. A workspace-level `AGENTS.md` on someone's machine governs
where files may be created there, not how this code is built or committed. For anything inside this
repository, this file wins.

## What this repository is

The native Android client for Mind-Ur-Set: Kotlin, Jetpack Compose, Room, DataStore, manual
dependency injection. One Gradle module, `:app`, application id `com.mindurset.app`. No backend,
no AI call, no network permission in the app; everything is on the device.

## Toolchain pins, do not drift

| Item | Value | Rule |
| --- | --- | --- |
| AGP | 9.4.1 with **built-in Kotlin** | Never add `org.jetbrains.kotlin.android`; it breaks the build |
| Kotlin / Compose plugin | 2.4.20 | Version changes need a PR |
| KSP / Room / DataStore | 2.3.12 / 2.8.5 / 1.2.1 | Same |
| SDK | compileSdk 37, targetSdk 36, minSdk 26 | Same |
| DI | manual `di/AppContainer.kt` | No Hilt, no Koin |
| Theme | light only (`darkTheme = false`) | No theme toggle without design sign-off |
| JDK | from `~/.gradle/gradle.properties` (`org.gradle.java.home`) | Do not edit that file |

## Commands

```bash
# Run from the repository root.

# build + unit tests (always take the build lock)
flock /tmp/mindurset-gradle.lock ./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain

# formatting and lint, both blocking in CI
flock /tmp/mindurset-gradle.lock ./gradlew spotlessCheck :app:lintDebug --console=plain
flock /tmp/mindurset-gradle.lock ./gradlew spotlessApply --console=plain   # fixes formatting

# device
adb devices                      # wireless 192.168.67.35:47111, previously USB 34d1e6a5
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.mindurset.app/.MainActivity
```

Several agents share this machine. Every Gradle invocation goes through
`flock /tmp/mindurset-gradle.lock`; a build without the lock will collide with another agent.

## Hard bans

1. Never push, amend, rebase or force-push `main`. Work on a branch and open a pull request.
2. Never commit `gradle/gradle-daemon-jvm.properties`. It pins a JDK 25 daemon that Gradle then
   downloads from foojay, which returns HTTP 400 here and fails every build at configuration time.
3. Never commit secrets: `local.properties`, `*.jks`, `*.keystore`, `keystore.properties`,
   `google-services.json`, `.env`, anything under `~/.ssh` or a Bitwarden export.
4. Never rewrite published history, even on your own branch, once a pull request is open.
5. Never add a dependency that talks to the network without a contract fixture and a fallback path.
6. Never change a version in `gradle/libs.versions.toml` outside a pull request that states why.
7. Never commit build output: `build/`, `app/build/`, `*.apk`, `*.aab`, `.gradle/`, `.kotlin/`.

## Code rules

- All user-facing text comes from `app/src/main/res/values/*.xml`. No hardcoded strings in Compose.
- `domain/` and `safety/` must not import Android. Business rules get a JUnit test in
  `app/src/test/java/com/mindurset/app/` that runs on the JVM.
- A screen is a pair: `ui/<area>/<Name>Screen.kt` and `ui/<area>/<Name>ViewModel.kt`, with routes
  declared in `ui/navigation/Routes.kt`.
- Copy stays non-punitive: no streak, no "you missed", no red badges, no loss framing.
- Follow the surrounding file rather than inventing a new structure; this codebase is small on
  purpose.

## Pitfalls that have already cost a day

- **Build dies with "Unable to download toolchain ... foojay ... 400"**: delete
  `gradle/gradle-daemon-jvm.properties` and rebuild. Do not try to fix the download.
- **Keyboard covers a text field**: the app is edge-to-edge (`enableEdgeToEdge`), so add
  `Modifier.imePadding()` to the scrolling column and request focus; do not animate to `maxValue`,
  that hides the rest of the form.
- **Top bar drifts or turns white**: put the bar in the Scaffold `topBar` slot with
  `colorScheme.background`, do not add `statusBarsPadding()` inside a screen, and use
  `contentWindowInsets = WindowInsets(0)` on screen Scaffolds.
- **A bottom tab does not return Home**: use `popBackStack(route)`, not
  `navigate(route, saveState/restoreState)`.
- **Mood hint ellipsis**: those labels are 8.sp and must stay one short line; lengthen the string at
  your own risk, not the layout.

## Definition of done for a pull request

- `assembleDebug`, `testDebugUnitTest`, `spotlessCheck` and `lintDebug` pass locally and in CI.
- A new business rule comes with a new unit test; a bug fix comes with the test that would have
  caught it.
- The pull request body ends with the traceability rows it closes, for example
  `Refs: F-03, UC-03`. A pull request that closes no row is not reviewed.
- Any UI change includes a screenshot in the pull request.
- No new warning in the build log.

## Commits

Conventional commit with a scope, subject in the imperative, at most 72 characters:

```
feat(checkin): focus the note above the keyboard
fix(nav): return to home from a pushed tab
test(domain): cover the triage threshold
```

Optional body explains why, not what. Last line carries `Refs: F-03, UC-03` when it closes a
traceability row. Commits are signed (`commit.gpgsign=true`). The member who commits a change owns
it, whatever tool helped write it. One commit per logical change; no work-in-progress commits on
`main`.

## Where documents live

- Architecture and scope decisions: `docs/decisions/NNNN-<slug>.md`, one file per decision.
- Test plans, usability notes and device evidence: `docs/testing/`.
- Project documents (proposal, use case model, activity diagrams, traceability matrix, mockups) live
  in the private repository `tophrr/mindurset-docs`, which you can clone and pull. They are never
  copied into this repository and never linked by a local path.

## Working alongside other agents

- One agent per `git worktree`. Branch naming: `agent/<tool>/<slug>` for agent work,
  `feat|fix|test|docs/<scope>-<slug>` for human work.
- Never merge a pull request you authored. Ask `@tophrr` for review.
- If two agents need the same file, the one whose branch was opened first keeps it; the other
  rebases after the merge.
