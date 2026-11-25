# Test Plan — Mind-Ur-Set Android Client

This document defines the test levels, the device matrix, the scripted test cases, and the
evidence format for releases of the Mind-Ur-Set native Android client (`com.mindurset.app`).

Every test case must trace back to a specific use case (`UC-xx`) or non-functional requirement
(`NFR-xx`). A test case without a reference is invalid and will be removed.

---

## 1. Test Levels and Allocation

| Level | Scope and Contents | Execution Environment | Gate / Tooling | Target NFRs / Traceability |
| --- | --- | --- | --- | --- |
| **JVM Unit** | Domain models, intensity calculation, triage threshold evaluation, reframe state machine transitions, the twelve red-team sentences against `LocalRiskFloor`, non-punitive copy assertions, and the action screen's grouping and validation. The Worker contract is validated in `worker/test` with Vitest, outside this module. Strictly zero Android framework imports. | Local JVM / CI runner (`flock /tmp/mindurset-gradle.lock ./gradlew testDebugUnitTest`) | Blocking on PR and CI | NFR-11, NFR-13, UC-02, UC-03, UC-04, UC-13 |
| **Instrumented** | Room DAO CRUD operations and migrations, DataStore preference persistence, EncryptedSharedPreferences operations, WorkManager task scheduling, Repository integration against mocked Worker responses, Compose UI screen state rendering (empty, loading, shimmer, offline fallback, error, safety halted). | Android Emulator or physical test device (`./gradlew connectedDebugAndroidTest`) | Release candidate verification | NFR-02, NFR-04, NFR-06, NFR-14, UC-01, UC-04, UC-22 |
| **Manual Device** | Physical hardware interactions, edge-to-edge IME insets under keyboard popup, dynamic font scaling (up to 200%), TalkBack screen reader accessibility traversals, touch target measurement (minimum 48 dp), airplane mode / offline transitions, system process kill simulation, external intent resolution (dialer 119, SMS/chat outreach for trusted contact). | Single physical handset in R4 (Xiaomi Redmi Note 10 Pro, Android 16; see Sec. 2) | Gate R4 beta / Release sign-off | NFR-01, NFR-03, NFR-05, NFR-06, NFR-10, UC-14, UC-15, UC-21 |

---

## 2. Device Matrix (R4 Single-Handset Record)

Per the 18 November 2026 project decision, the full multi-device matrix was not pursued for R4. All
device evidence in this round comes from **one physical handset only**: a Xiaomi Redmi Note 10 Pro
running Android 16 (SDK 36, `arm64-v8a`). The Android emulator crashes with `SIGSEGV` at graphics
initialisation on the development host across all tested graphics backends, and no low-end handset,
API 26 device, or tablet was provisioned for R4.

| Device Name | Device Type | OS Version | API Level / ABI | Display Spec & Density | Role / Priority | R4 Status & Blocker |
| --- | --- | --- | --- | --- | --- | --- |
| **Pixel 8 emulator (AVD)** | Emulator, Android Studio | Android 15 (API 35) | SDK 35 (x86_64), `system-images;android-35;google_apis_playstore;x86_64` | 6.2" 1080×2400, 420 dpi (2.625×), 4 vCPU, 3 GB RAM | **Primary Target (planned):** layout, edge-to-edge and IME insets, Compose states, process-kill recovery, Room persistence. | **`NOT AVAILABLE`** — emulator crashes at graphics init (`SIGSEGV`) on the development host; all graphics combinations tried. |
| **Pixel 8 emulator, API 36** | Emulator, Android Studio | Android 16 (API 36) | SDK 36 (x86_64), `system-images;android-36;google_apis_playstore;x86_64` | 6.2" 1080×2400, 420 dpi (2.625×), 4 vCPU, 3 GB RAM | **Target-SDK reference (planned):** exercise `targetSdk 36` behaviour on an x86_64 AVD. | **`NOT AVAILABLE`** — same host emulator crash (`SIGSEGV` at graphics init); missing working AVD runtime. |
| **Xiaomi Redmi Note 10 Pro (physical)** | Physical hardware | Android 16 | SDK 36 (`arm64-v8a`) | 6.67" 1080×2400, 410 dpi physical (`2.5625×`), ~397 ppi | **Sole R4 device executed:** produced all R4 physical-handset evidence (real IME, dialer intent, process kill, offline fallback, instrumented tests). | **`FAIL`** — executed on build `51b2c21` (25 Nov 2026) with zero unhandled crashes; passed IME, rotation, offline check-in/reframe/Companion, dialer intent, `KILL-02`, and `KILL-05`, but failed draft restoration (`KILL-01`, `KILL-03`), 200% font truncation on Home/Companion, and secondary-user SMS launch on `S-91`; TalkBack is `NOT AVAILABLE` on this ROM (details in Sec. 2.1). |
| **Generic Phone Emulator / Testbed** | Android Virtual Device (AVD) | Android 8.0 (Oreo) | SDK 26 (x86_64 / arm64) | 5.0" LCD, 1080×1920 (~440 ppi), 60 Hz | **Baseline Target (planned):** validates `minSdk 26` compatibility, legacy crypto fallback, and Room backwards compatibility. | **`NOT AVAILABLE`** — emulator crashes at graphics init (`SIGSEGV`) on the development host and no physical API 26 hardware is provisioned. |
| **Low-RAM reference profile (Android Go)** | AVD / physical testbed | Android 8.1 / 13 (Go Edition) | SDK 27 / 33 (x86_64 / arm64) | 5.5" LCD, 720×1440 (hdpi, ~293 ppi), 60 Hz | **Low-RAM Target (planned):** validates ≤ 150 MB heap ceiling and memory-pressure behaviour (`isLowRamDevice() = true`). | **`NOT AVAILABLE`** — no low-end (2–3 GB RAM) physical handset or working Android Go AVD provisioned for R4. |

Proposal §8.3 also specifies a **tablet** target; no tablet hardware or working tablet AVD was
provisioned for R4 (`NOT AVAILABLE` — no hardware).

The physical handset is a personal device accessed via wireless adb (`192.168.67.35:47111`, secondary
user `10` / `dev`). Cases that require a second device, cellular 4G, or unprovisioned hardware are
recorded as `NOT AVAILABLE` with the blocking cause, following the policy below. They are never
marked as passed.

### 2.1 R4 Single-Handset Manual Execution Summary (25 November 2026)

- **Build under test:** `com.mindurset.app` debug assembled from `origin/main` `51b2c21`
- **Device executed:** Xiaomi Redmi Note 10 Pro (`M2101K6G`), Android 16 (SDK 36, `arm64-v8a`),
  secondary user `10` (`dev`), clean guest session (`[synthetic]` inputs only)
- **Prior 11–24 November evidence cited rather than re-run:** Indonesian localisation
  (`20261111-i18n-consent-id.png` through `20261111-i18n-s90-crisis-id.png`), ASQ check-in /
  journal / Companion runs (`20261113-asq-*.png`, `20261119-asq-journal-*.png`,
  `20261124-asq-companion-*.png`), contextual memory `S-52`/`S-53` (`20261124-s52-memory-*.png`,
  `20261124-s53-settings-memory.png`), Companion modes (`20261124-companion-modes-*.png`), and
  offline Today's Action (`20261104-action-today-done.png`).

| Area / Case | Status | Observed Behaviour on Redmi Note 10 Pro (`51b2c21`) | Evidence |
| --- | --- | --- | --- |
| **Keyboard & IME insets (`S-11`)** | **PASS** | Soft keyboard opened (`mInputShown=true`); selected mood, intensity `7 / 10`, `[synthetic]` note field, and `Continue` button stayed cleanly above the IME without overlap. | `docs/testing/evidence/20261125-s11-ime-keyboard.png` |
| **Font scaling to 200%** (`font_scale=2.0`) | **FAIL** | Crisis card `S-90` wrapped cleanly with no clipping. However, Home (`S-11` card) truncated mood labels (`Hap`, `Anxi`, `Stres`, mid-word `Overw/helmed`) and clipped the Companion shortcut subtitle (`Here to listen,`); Companion clipped the `Distract` chip at the right edge and truncated the active bottom-nav label (`Compa`). | `docs/testing/evidence/20261125-s90-font-200.png`, `docs/testing/evidence/20261125-s11-font-200.png`, `docs/testing/evidence/20261125-companion-font-200.png` |
| **TalkBack traversal on primary screens** | **`NOT AVAILABLE`** | TalkBack (`com.google.android.marvin.talkback`) is not installed on this custom Android 16 ROM (`query-services` lists only `AccessibilityMenuService` and `FreeKioskAccessibilityService`). A live `uiautomator` accessibility-tree and touch-target audit across Home, Companion, Journal (`S-12`), and Crisis Card (`S-90`) confirmed `content-desc` labels and `>= 48 dp` targets on all primary controls except Companion's middle starter chip (`I want one step I can take`, `36.3 dp` height). | `docs/testing/evidence/20261125-a11y-tree-audit.log` |
| **Offline & airplane-mode transitions (`OFF-01`, `OFF-02`, `OFF-03`, Companion offline)** | **PASS** | Clean guest onboarding (`A-01`/`OFF-01`), check-in save (`OFF-02`), triage offer, and three-step reframe degradation (`OFF-03`) completed locally with `airplane_mode_on=1` and `restricted_networking_mode=1`. Companion degraded to the bundled WHO Stress offline exercise plus fail-closed L1 banner with `restricted_networking_mode=1` blocking all third-party app network egress (Wi-Fi remained associated for wireless adb after toggling airplane mode dropped the adb link). | `docs/testing/evidence/20261125-s11-offline-triage.png`, `docs/testing/evidence/20261125-s21-offline-reframe.png`, `docs/testing/evidence/20261125-companion-offline.png` |
| **Rotation (`ROT-02`, `ROT-05`)** | **PASS** | `ROT-02` (`S-11`): typed text survived landscape rotation and `Continue` remained above the IME (scrolling or dismissing IME is required in landscape to view upper fields). `ROT-05` (`S-90`): crisis card stayed visible and non-dismissible in landscape with `Call 119 ext. 8` and `Message someone I trust` accessible. | `docs/testing/evidence/20261125-s11-rotated.png`, `docs/testing/evidence/20261125-s90-rotated.png` |
| **Process kill — uncommitted check-in (`KILL-01`)** | **FAIL** | `am kill com.mindurset.app` terminated PID `13459`; relaunch (`14018`) returned to `S-11` with `Anxious` (navigation arg), but intensity reset to `5 / 10` and the uncommitted `[synthetic]` draft note was lost. | `docs/testing/evidence/20261125-s11-kill01-resume.png` |
| **Process kill — active reframe (`KILL-02`)** | **PASS** | `am kill com.mindurset.app` terminated PID `14018` on `Step 2 of 3`; relaunch (`14442`) resumed directly at `Step 2 of 3` from Room (consistent with `20261111-s21-reframe-resumed.png`). | `docs/testing/evidence/20261125-s21-kill02-resume.png` |
| **Process kill — journal draft (`KILL-03`)** | **FAIL** | `am kill com.mindurset.app` terminated PID `21922`; relaunch (`23239`) returned to `New reflection` (`S-12`), but the uncommitted `[synthetic]` draft text was cleared. | `docs/testing/evidence/20261125-s12-kill03-resume.png` |
| **Process kill — L3 crisis card (`KILL-05`)** | **PASS** | `am kill com.mindurset.app` terminated PID `14442` on `S-90`; relaunch (`21922`) immediately re-rendered `S-90` without bypassing the safety halt. | `docs/testing/evidence/20261125-s90-kill05-resume.png` |
| **Dialer intent (`OFF-08`, `S-90`)** | **PASS** | Tapping `Call 119 ext. 8` launched `com.android.dialer` via `ACTION_DIAL` prefilled with `119` with no network required (`CrisisResources.PHONE` passes `119`; extension `8` is displayed in UI copy rather than appended as `,8`). | `docs/testing/evidence/20261125-s90-dialer-intent.png` |
| **Share sheet & trusted-contact outreach (`S-91`)** | **`NOT AVAILABLE` (share sheet) / FAIL (secondary-user SMS)** | No generic `Intent.ACTION_SEND` share sheet exists in build `51b2c21` (counselor handoff `#5` and JSON export `#53` are not merged yet). On `S-91`, saving a `[synthetic]` contact and tapping `Send a message` fired `Intent.ACTION_SENDTO` (`smsto:`) without crashing and resolved to `com.android.messaging` (sole SMS handler on user `10`, so no chooser sheet appeared), but `com.android.messaging` rejected the launch with toast `Could not start conversation` because secondary user `10` (`dev`) has SMS disabled (prior saving evidence: `20261113-contact-sheet-saved.png`, `20261113-crisis-card-contact.png`). | `docs/testing/evidence/20261125-s91-sms-intent.png` |

### 2.2 Verdict for NFR-10

**`NFR-10` remains `Unverified`.** All R4 execution took place on one physical device (Xiaomi Redmi
Note 10 Pro, Android 16, SDK 36, `arm64-v8a`), leaving four target rows in the matrix `NOT AVAILABLE`
(Pixel 8 API 35 AVD, Pixel 8 API 36 AVD, API 26 baseline target, and low-RAM Android Go target) plus
the unprovisioned tablet target from Proposal §8.3; in addition, the project has no Crashlytics
integration or automated zero-crash gate in CI. Although no unhandled crash occurred during the R4
manual run on `51b2c21`, single-handset manual passes cannot satisfy Proposal §8.3 or §10 `NFR-10`,
and this report records **one physical device**, not a device matrix.

### Meaning of "NOT AVAILABLE" in Test Reports

If an execution environment or target device cannot be provisioned during a release cycle:
1. The status column in the test execution report must explicitly record **`NOT AVAILABLE`** alongside the blocking root cause (e.g., hardware battery defect, emulator system image missing from build agent).
2. The case **must never be marked as passed, waived implicitly, or omitted from the report**.
3. A `NOT AVAILABLE` entry on a primary or minimum SDK device is a release blocker. Releasing requires either:
   - Retesting on an approved substitute device matching the identical SDK level and architecture, or
   - An explicit, signed waiver recorded by the technical lead (`@tophrr`) in the release notes explaining the mitigated risk.

---

## 3. Case Tables

### 3.1 Offline Cases (Airplane Mode)

All offline cases are executed with system Airplane Mode enabled and Wi-Fi disabled.

| Case ID | Title / Scenario | Preconditions & Input Action | Expected Result | Reference |
| --- | --- | --- | --- | --- |
| **OFF-01** | First launch and onboarding offline | Clean install or cleared app storage. Launch app in airplane mode. Progress through scope notice, PDP consent, nickname, and baseline screener. | Onboarding completes normally. User arrives at Home check-in screen. No network error dialogs or blocking spinners appear. | UC-01, NFR-04, NFR-15 |
| **OFF-02** | Daily check-in logging offline | App launched offline. Select mood face (e.g., Sad), drag slider to 7, enter note "Heavy workload today", tap Continue. | Check-in saves instantly to local Room database. Home screen updates with today's logged state. Response time <= 15 s. | UC-02, NFR-01, NFR-04, NFR-05 |
| **OFF-03** | Emotional triage and offline reframe exercise | Check-in logged with negative mood and intensity >= 6 while offline. Triage offer appears; tap "Start Reframe". | App degrades gracefully: displays clearly labelled offline cognitive exercise instead of spinning or crashing. Three steps progress locally. | UC-03, UC-04, NFR-04 |
| **OFF-04** | Complete today's micro-action offline | An active micro-action exists on Home screen. Tap checkbox while offline. | Action is marked completed immediately in local Room database. UI updates with checkmark without lag or error toast. | UC-10, NFR-04, NFR-13 |
| **OFF-05** | Compose and save journal entry offline | Open Journal tab offline. Type a multi-paragraph entry and tap Save. | Entry persists to local Room database. App confirms save locally. Entry appears in journal list. | UC-11, NFR-04, NFR-05 |
| **OFF-06** | View effort insights offline | Check-ins, completed reframes and completed actions exist locally. Open Home offline. | The 28-day effort heatmap renders from local Room data, with each day's value capped at three. No spinner or network failure view. | UC-12, NFR-04 |
| **OFF-07** | Local risk screening offline | While offline, enter text containing explicit self-harm keywords ("want to die") in check-in note or journal. | LocalRiskFloor detects match offline. Escalates immediately to L2/L3 protocol without attempting network calls. | UC-13, UC-15, NFR-04 |
| **OFF-08** | Emergency crisis dialer access offline | From any screen, navigate to crisis support or trigger safety protocol offline. Tap "Call 119 ext. 8". | Crisis numbers are bundled locally. Android phone dialer opens prefilled with `119` (extension `8`). No network connection required. | UC-15, NFR-04 |
| **OFF-09** | Account sign-in / upgrade attempt offline | Navigate to Profile -> Account & Backup while offline. Attempt email/password sign-in. | App displays clear message explaining that an active connection is required for account operations. Local session and data remain untouched. | UC-21, NFR-04, NFR-15 |
| **OFF-10** | Backup synchronization queued offline | User is signed in with backup enabled. New check-in logged while offline. | Backup sync request is deferred to WorkManager with `NetworkType.CONNECTED` constraint. Sync executes when connectivity resumes. | UC-22, NFR-04 |

---

### 3.2 Rotation Cases

All rotation cases test orientation changes between portrait (natural) and landscape modes.

| Case ID | Title / Scenario | Preconditions & Input Action | Expected Result | Reference |
| --- | --- | --- | --- | --- |
| **ROT-01** | Rotation during onboarding consent & inputs | Onboarding screen S-03 (consent toggles) or the nickname step, which has no screen ID in Sec. 7.2. Enter text and toggle switches, then rotate 90 degrees. | Entered text, selected checkboxes, and scroll position are fully preserved. Form remains interactive with no reset. | UC-01, NFR-05, NFR-06 |
| **ROT-02** | Rotation during daily check-in | Home screen S-11. Select mood face (e.g., Anxious), set slider to 8, type unsubmitted note. Rotate device. | Selected face, slider value (8), and entered note text remain intact. Keyboard adjusts cleanly without obscuring buttons. | UC-02, NFR-05 |
| **ROT-03** | Rotation during active reframe session | Therapy screen S-21. Reframe step 1 active with partial thought deconstruction text typed. Rotate device. | Thought deconstruction text, current step indicator (Step 1 of 3), and session state remain identical. No state reload. | UC-04, UC-05, NFR-05 |
| **ROT-04** | Rotation during journal drafting | Journal screen S-12. Compose long free-text entry. Rotate device back and forth between portrait and landscape. | Cursor position is maintained, all draft text is preserved, and scrolling column respects IME padding in landscape. | UC-11, NFR-05 |
| **ROT-05** | Rotation on crisis overlay (S-90) | Risk level L3 triggered; un-dismissible crisis modal S-90 displayed. Rotate device. | Crisis card remains visible and non-dismissible. Emergency call button (`119 ext. 8`) and trusted contact button remain fully accessible. | UC-15, NFR-05, NFR-06 |
| **ROT-06** | Rotation on account entry form | Screen S-06 / S-54. Type email address and partially entered password. Rotate device. | Email and password field values persist. Password visibility toggle state is retained. Form validation state remains consistent. | UC-21, NFR-05 |
| **ROT-07** | Rotation during data export / delete dialog | Screen S-92. Open "Delete All Data" confirmation dialog. Rotate device. | Confirmation dialog remains open and attached to parent screen. Selected options and warning copy persist without dismissing. | UC-16, NFR-05 |

---

### 3.3 Process Kill / State Restoration Cases

Simulate background process kill via adb: `adb shell am kill com.mindurset.app` or `adb shell cmd activity kill com.mindurset.app` while app is in the background.

| Case ID | Title / Scenario | Preconditions & Input Action | Expected Result | Reference |
| --- | --- | --- | --- | --- |
| **KILL-01** | Process kill during uncommitted check-in note | Enter mood face, slider position, and draft note on S-11. Press Home button. Terminate app process. Reopen app from launcher. | App launches to Home screen. Draft note text and mood selection are restored via SavedStateHandle or Room persistence. | UC-02, NFR-05 |
| **KILL-02** | Process kill during active reframe session | Reach Step 2 of reframe session (UC-06). Move app to background. Kill process. Re-launch app. | Reframe session state (status "active", step 2, prior messages) resumes from Room database without starting over from step 0. | UC-04, UC-06, NFR-05 |
| **KILL-03** | Process kill during journal entry composition | Draft a long journal entry in S-12. Background app and terminate process via adb. Relaunch app. | Journal draft is recovered from local draft cache. Zero data loss for typed content. | UC-11, NFR-05 |
| **KILL-04** | Process kill while viewing Insights | Open Insights tab (S-41). Background app and kill process. Relaunch app. | Insights screen rebuilds its state from local Room queries cleanly. No crashes or unhandled state transitions. | UC-12, NFR-05, NFR-10 |
| **KILL-05** | Process kill on L3 crisis card | Safety overlay S-90 displayed. Background app and terminate process. Reopen app. | Crisis card S-90 immediately re-renders upon launch. App does not bypass crisis screen or route to regular Home. | UC-13, UC-15, NFR-05 |
| **KILL-06** | Process kill during account creation | User enters credentials on S-06, app backgrounds and process is killed before submit. Relaunch app. | App returns safely to account screen or onboarding without corrupted state. No orphaned credential tokens in storage. | UC-21, NFR-05, NFR-14 |
| **KILL-07** | Process kill during data deletion execution | User confirms "Delete All Data". Process terminated during wipe execution. Relaunch app. | Upon relaunch, app detects incomplete wipe or fresh state, enforces complete purge, and routes user to onboarding (S-01). | UC-16, NFR-05, NFR-08 |

---

### 3.4 Account and Session Cases

Covers account lifecycle, session management, and optional identity rules from proposal §12.3.

| Case ID | Title / Scenario | Preconditions & Input Action | Expected Result | Reference |
| --- | --- | --- | --- | --- |
| **A-01** | Onboarding without account creation | Launch clean install. Reach account step S-06. Tap "Continue without an account". | User reaches first check-in immediately. All core features (check-in, reframe, insights, journal) function fully. Verified in airplane mode too. | UC-21, NFR-15 |
| **A-02** | Sign up with existing email | Navigate to Account screen. Enter an email address that already has an account. Submit form. | Clear, non-technical error message displayed. App offers options to sign in or reset password. No duplicate account created. | UC-21, NFR-14 |
| **A-03** | Sign in with invalid password | On Sign In screen, enter existing email and incorrect password. Submit form. | Generic failure message shown ("Invalid credentials"). Does not leak whether email exists. One retry offered. Local data untouched. | UC-21, NFR-14 |
| **A-04** | Sync attempt with unverified email | Register new account with email. Do not click email verification link. Attempt manual backup sync in S-54. | App explains in plain terms what verification is for. Local app loop continues working normally without blocking. | UC-21, UC-22, NFR-15 |
| **A-05** | Upgrade anonymous account to email | App used anonymously with prior check-ins logged. Open Profile -> Upgrade Account. Register email/password. | The same `user_id` is retained. All local rows persist intact. Pre-existing check-ins remain visible. Cloud backup is initiated. | UC-21, UC-22, NFR-14 |
| **A-06** | Sign-up / sign-in with network off | Enable airplane mode. Attempt to sign up or sign in from S-06 / S-54. | Operation fails gracefully with a plain-language network explanation. Nothing in local data is modified or deleted. | UC-21, NFR-04, NFR-15 |
| **A-07** | Expired ID token handling | Run active session until Firebase ID token expires (simulate via test hook). Initiate Worker request. | Android SDK refreshes token silently in the background. If refresh fails, degrades to offline path rather than throwing crash/error. | UC-21, NFR-09, NFR-14 |
| **A-08** | Sign out with local data present | User is signed in with local check-in records. Tap "Sign Out" in S-54. | Dialog offers clear choice: "Keep data on this phone" or "Clear local data". Neither choice is pre-selected destructively. Selected choice is honoured. | UC-21, NFR-14 |
| **A-09** | Account deletion from S-54 / S-92 | User taps "Delete Account". Enter confirmation. | Remote account, backup blobs, and derived summaries are purged. All local database rows are erased. Confirmation shown matches outcome. | UC-16, UC-21, NFR-08 |
| **A-10** | Release build credential scan | Inspect release APK assets, classes.dex strings, and runtime logcat output. | Zero API keys, zero service account tokens, and zero passwords present in APK strings, SharedPreferences, or logcat. | NFR-09, NFR-14 |

---

### 3.5 Data Control and Privacy Cases

Covers consent lifecycle, memory window limits, data export, and local data deletion (UU 27/2022 PDP alignment).

| Case ID | Title / Scenario | Preconditions & Input Action | Expected Result | Reference |
| --- | --- | --- | --- | --- |
| **DATA-01** | Granular PDP consent granting | First run onboarding screen S-03. Review granular consent options (local storage, AI processing, memory summaries). | Granular options can be toggled independently. Selections are stored as `ConsentRecord` entities in Room and DataStore. | UC-01, NFR-08 |
| **DATA-02** | Revocation of AI processing consent | User previously granted AI consent. In S-92, toggle "AI Text Processing" off. | AI processing consent is revoked. Reframe flows immediately switch to deterministic local offline exercises without remote transmission. | UC-01, UC-16, NFR-08 |
| **DATA-03** | Memory manager summary inspection | User has completed several reframe sessions. Open Memory Manager (S-52). | App displays exact derived memory summaries currently stored. No hidden background profiles exist. Clear list presented to user. | UC-16, NFR-08 |
| **DATA-04** | Selective memory summary deletion | In S-52, select an individual memory summary and tap Delete. | Target `SESSION_SUMMARY` row is permanently deleted from local Room database. Subsequent AI sessions operate without that context. | UC-16, NFR-08 |
| **DATA-05** | Three-session sliding memory window | Complete four consecutive reframe sessions across four days. | Memory manager holds only the last three sessions. The oldest summary is automatically pruned per retention policy (`retention_until`). | UC-04, UC-16, NFR-08 |
| **DATA-06** | Full data export as JSON | Navigate to Data Settings (S-92) -> "Export My Data". Tap Export. | App generates valid, structured JSON file containing user profile, check-in history, journal entries, and micro-actions via Storage Access Framework. The trusted contact (S-91) is deliberately excluded: the number belongs to someone else (ADR 0006). | UC-16, NFR-08 |
| **DATA-07** | Complete local data deletion | In S-92, select "Delete All Local Data" and confirm dialog. | Room database is wiped, DataStore is reset, credentials are removed from Keystore. App restarts to first-run onboarding (S-01). | UC-16, NFR-08 |
| **DATA-08** | Server and local log sanitisation | Perform check-in and reframe session with sensitive personal text. Inspect logcat and diagnostic logs. | Zero user journal or check-in text is printed to logcat. Only structured event identifiers and stage timings appear (`NFR-12`). | NFR-08, NFR-12 |
| **DATA-09** | Tracker and third-party SDK audit | Inspect build dependencies and outgoing network traffic via MITM proxy. | Zero third-party ad networks, behavioural trackers, or analytics SDKs present in APK. No telemetry transmissions occur. | NFR-08 |

### 3.6 Low-RAM Cases

Validates the heap ceiling the proposal promises (≤ 150 MB) and the behaviour under memory pressure.
The reference profile does not exist yet, so both cases are `NOT AVAILABLE` until it does.

| Case ID | Title / Scenario | Preconditions & Input Action | Expected Result | Reference |
| --- | --- | --- | --- | --- |
| **LOWRAM-01** | Cold start and memory budget under the low-RAM ceiling | Run on a profile with `ActivityManager.isLowRamDevice() = true`, or cap heap growth (`adb shell setprop dalvik.vm.heapgrowthlimit 128m`). Launch, then walk Home, Check-in, Journal and Settings. | Heap stays at or below 150 MB, no slow-frame warnings, no `OutOfMemoryError`, UI stays responsive. | NFR-06, NFR-10 |
| **LOWRAM-02** | Trim-memory response while in the foreground | App in the foreground. Inject trims (`adb shell am send-trim-memory com.mindurset.app MODERATE`, then `CRITICAL`), then complete a check-in. | Caches and stale Compose state release cleanly; the draft survives and nothing crashes. | NFR-05, NFR-06, NFR-10 |

### 3.7 ASQ Cases

The four ASQ items plus the acuity probe, which the proposal raises at L2 when hopelessness is
ambiguous. The check-in, journal and Companion paths ask them (`safety/AsqFlow`, ADR 0007).

| Case ID | Question / Scenario | Answer branch | Expected behaviour | Reference | Status |
| --- | --- | --- | --- | --- | --- |
| **ASQ-01** | Q1, "In the past few weeks, have you wished you were dead?" | No | Recorded, advances to Q2 without a crisis overlay. | UC-13, UC-15, F-09, NFR-11 | Runnable on check-in, journal and Companion |
| | | Yes | Escalates to L3: the crisis card (S-90) with 119 ext. 8 and the trusted contact (S-91); reflection locked out. | UC-13, UC-15, F-09, NFR-11 | Runnable on check-in, journal and Companion |
| **ASQ-02** | Q2, "… felt that you or your family would be better off if you were dead?" | No | Recorded, advances to Q3. | UC-13, UC-15, F-09, NFR-11 | Runnable on check-in, journal and Companion |
| | | Yes | Escalates to L3; navigation disabled until acknowledged. | UC-13, UC-15, F-09, NFR-11 | Runnable on check-in, journal and Companion |
| **ASQ-03** | Q3, "In the past week, have you been having thoughts about killing yourself?" | No | Recorded, advances to Q4. | UC-13, UC-15, F-09, NFR-11 | Runnable on check-in, journal and Companion |
| | | Yes | Escalates to L3 with the helpline and outreach shown. | UC-13, UC-15, F-09, NFR-11 | Runnable on check-in, journal and Companion |
| **ASQ-04** | Q4, "Have you ever tried to kill yourself?" | No to all four | Negative screen: no lockout, supportive tone, the helpline offered as an optional banner, grounding exercises still available. | UC-13, UC-15, F-09, NFR-11 | Runnable on check-in, journal and Companion |
| | | Yes | Escalates to L3, past attempt detected. | UC-13, UC-15, F-09, NFR-11 | Runnable on check-in, journal and Companion |
| **ASQ-05** | Acuity probe, "Are you having thoughts of killing yourself right now?", asked when any earlier answer is Yes | No | Non-imminent L3: resources shown, safety-plan assistance offered. | UC-13, UC-15, F-09, NFR-11 | Runnable on check-in, journal and Companion |
| | | Yes | Acute L3: navigation locks out, the dialer is pre-filled with 119 ext. 8. | UC-13, UC-15, F-09, NFR-11 | Runnable on check-in, journal and Companion |

### 3.8 Coverage today

Several cases below depend on features that are not in the repository yet. Per rule 3 in
`docs/testing/README.md` they stay in the plan and are reported as `NOT AVAILABLE` with the reason,
never deleted and never marked as passed.

| Area | Cases | Blocked by |
| --- | --- | --- |
| Account, sign-in and token refresh | A-02 to A-07, A-09, KILL-06 | No Firebase project and no `google-services.json`. The app requests the INTERNET permission for Companion and reframe generation, but no account backend or auth token endpoint exists to test |
| Backup and sync | OFF-09, OFF-10, A-04, A-05 | No backup or D1 mirror endpoint wired into the client |
| Insights | OFF-06, KILL-04 | The Home 28-day heatmap and local aggregation tests are implemented (`20261112-home-effort-heatmap.png` and `20261125-s11-font-200.png` show single-device renders); 7/30-day breakdown (`#52`) and a seeded multi-source `KILL-04` run remain open |
| Export & consent revocation | DATA-02, DATA-06 | `S-52` Memory Manager (`DATA-03`–`DATA-05`) landed in `#51` (`20261124-s52-memory-populated.png`); in-app JSON export (`DATA-06`) and consent revocation UI (`#53`) are not merged yet |
| Instrumented suite | `connectedDebugAndroidTest` rows outside DAO/Consent/Crisis | 15 instrumented tests (`CheckInDaoTest`, `JournalDaoTest`, `ConsentScreenTest`, `CrisisScreenTest`) run via `am instrument` on the single Redmi Note 10 Pro handset (`TESTING.md`); remaining screens are still route-coupled (`#60`) |
| Multi-device & Low-RAM | LOWRAM-01, LOWRAM-02, API 26, AVD API 35/36, tablet | Single-handset R4 run only (18 November decision); emulator crashes at graphics init (`SIGSEGV`) and no low-end, API 26, or tablet hardware is provisioned |

---

## 4. Evidence Format and Traceability

All manual test runs and release candidate validations must produce concrete evidentiary artifacts stored under `docs/testing/evidence/`.

### 4.1 Screenshot File Naming

Screenshots captured from devices must follow the canonical format:

```
<YYYYMMDD>-<screen_id>-<state_or_condition>.png
```

- `<YYYYMMDD>`: ISO calendar date of the test execution (e.g., `20261022`).
- A screen that has no identifier in Sec. 7.2 yet uses a short slug instead, and the file says so in
  its pull request. Today's Action is the current case: its evidence is named
  `action-today`, and the files are renamed once the inventory gives it an `S` number.
- `<screen_id>`: Official screen identifier from proposal §7.2:
  - `s01` (Launch / check)
  - `s02` (Scope & limits)
  - `s03` (PDP consent)
  - `s04` (Baseline GAD-7 / PHQ-9)
  - `s06` (Account creation / sign-in)
  - `s11` (Daily check-in)
  - `s12` (Journal entry)
  - `s13` (Reframe entry)
  - `s21` (Reframe session active)
  - `s41` (Insights heatmap)
  - `s52` (Memory manager)
  - `s54` (Account & backup)
  - `s90` (Safety crisis overlay)
  - `s91` (Trusted-contact sheet)
  - `s92` (Data & consent settings)
- `<state_or_condition>`: Concise descriptor of test condition (e.g., `offline`, `rotated`, `shimmer`, `error`, `l3-halted`).

**Examples:**
- `docs/testing/evidence/20261022-s11-checkin-offline.png`
- `docs/testing/evidence/20261022-s21-reframe-rotated.png`
- `docs/testing/evidence/20261022-s90-crisis-halted.png`

### 4.2 Logcat Excerpt File Naming

Logcat recordings and diagnostic traces must follow the format:

```
<YYYYMMDD>-<subsystem_or_case>-<condition>.log
```

**Examples:**
- `docs/testing/evidence/20261022-safety-escalation-closed.log`
- `docs/testing/evidence/20261022-processkill-recovery.log`
- `docs/testing/evidence/20261022-token-refresh-silent.log`

### 4.3 Evidence Privacy Rules
- **No Personal Data:** Test screenshots and logcat excerpts must never contain actual clinical patient data, real student identifiers, or personal passwords.
- **Log Hygiene:** Ensure logcat excerpts conform to NFR-12: structured decision logs only, with user input strings scrubbed.
