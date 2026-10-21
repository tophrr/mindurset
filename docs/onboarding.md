# Onboarding: your first hour on this repository

Read this once, top to bottom. It takes ten minutes, and it replaces a week of guessing.

## 1. Accept the invitation

You have been invited as a collaborator with write access. Until you accept it (GitHub sends it by
email, and it also appears at https://github.com/tophrr/mindurset/invitations) you cannot push and
you cannot open a pull request.

## 2. Get the project running

```bash
git clone https://github.com/tophrr/mindurset.git
cd mindurset

# JDK 21 or newer; Android Studio's bundled JBR works and is already used by Gradle here.
# The Android SDK path goes into local.properties, which is never committed:
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

# Sign your commits so GitHub shows them as verified (use your own key).
git config --global commit.gpgsign true

# Build and test. Always take the lock: several agents share this machine.
flock /tmp/mindurset-gradle.lock ./gradlew :app:assembleDebug :app:testDebugUnitTest --console=plain
```

Expected: `BUILD SUCCESSFUL` and 7 unit tests. Then install on your device:

```bash
adb devices
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.mindurset.app/.MainActivity
```

## 3. Read the two contracts

- `AGENTS.md` is what any AI agent reads before touching the code. It carries the toolchain pins,
  the commands, the hard bans and the pitfalls that have already cost us time.
- `CONTRIBUTING.md` is the human version: branch model, commit format, pull request rules.

Ten minutes here saves you a rejected pull request later.

## 4. Your first pull request, in six steps

```bash
git switch -c feat/<scope>-<slug>          # for example feat/triage-offer-card
# ... work ...
flock /tmp/mindurset-gradle.lock ./gradlew spotlessCheck :app:lintDebug :app:testDebugUnitTest :app:assembleDebug --console=plain
git add -A && git commit -m "feat(triage): offer the reframe card above intensity five"
git push -u origin feat/<scope>-<slug>
```

Then open the pull request on GitHub, fill the template, and wait for three checks: formatting,
build with lint and tests, secret scan. Merge with **squash**. Delete the branch.

`main` is protected: no direct pushes, no force push, linear history only. The project manager cannot
bypass it either, except for an incident that then gets written into the pull request.

## 5. Use AI, it is expected

Every task in this project is meant to be done with an AI agent in the loop. The repository is
prepared for it:

- Point your agent at the repository and tell it to read `AGENTS.md` first. Claude Code, Codex and
  Cursor pick it up automatically; anything else needs the sentence.
- One agent per `git worktree`, branch `agent/<tool>/<slug>`. Two agents in one checkout will
  overwrite each other.
- Never let an agent push to `main`, rewrite history, touch `gradle/libs.versions.toml`, or commit
  `gradle/gradle-daemon-jvm.properties`. Those rules are already in `AGENTS.md`; repeat them anyway.
- Agent commits carry a trailer: `Assisted-by: <tool name>`. This is disclosure, not decoration, and
  the report has to be able to show it.
- Make the agent run the same gate you run, and paste the output into the pull request. "It should
  work" is not evidence.

Good prompts for this codebase: "read AGENTS.md, then implement X, add the JUnit test, run the four
Gradle tasks, show me the output". Bad prompts: "fix the app" and anything that starts by asking
for a rewrite.

## 6. Where things live

| Thing | Where |
| --- | --- |
| Traceability matrix (feature, use case, screen, status, owner) | `mindurset-docs/deliverables/Mind-Ur-Set_Traceability_Matrix.pdf` |
| Decisions | `docs/decisions/` in this repository |
| Test plans and device evidence | `docs/testing/` |
| Course documents | outside the repository, in `mindurset-docs` |
| Weekly status and assignments | the team chat, sent by the project manager |

## 7. Who to ask

Blocked on access, CI or a decision: the project manager (`@tophrr`). Blocked on a design detail:
Nabeel. Blocked on a test case: Evan. Blocked on the Worker: Tora. Blocked on a screen: Christian.

Do not start a second version of work someone else is already doing. Say it in the chat, in one
line, and pick something else for an hour.
