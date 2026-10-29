# Test Plan — Mind-Ur-Set Android Client

This document defines the test levels, the device matrix, the scripted test cases, and the
evidence format for releases of the Mind-Ur-Set native Android client (`com.mindurset.app`).

Every test case must trace back to a specific use case (`UC-xx`) or non-functional requirement
(`NFR-xx`). A test case without a reference is invalid and will be removed.

---

## 1. Test Levels and Allocation

| Level | Scope and Contents | Execution Environment | Gate / Tooling | Target NFRs / Traceability |
| --- | --- | --- | --- | --- |
| **JVM Unit** | Domain models, intensity calculation, triage threshold evaluation, reframe state machine transitions, LocalRiskFloor keyword matching, retry backoff calculation, JSON contract serialisation/deserialisation, non-punitive copy assertions. Strictly zero Android framework imports. | Local JVM / CI runner (`flock /tmp/mindurset-gradle.lock ./gradlew testDebugUnitTest`) | Blocking on PR and CI | NFR-11, NFR-13, UC-02, UC-03, UC-04, UC-13 |
| **Instrumented** | Room DAO CRUD operations and migrations, DataStore preference persistence, EncryptedSharedPreferences operations, WorkManager task scheduling, Repository integration against mocked Worker responses, Compose UI screen state rendering (empty, loading, shimmer, offline fallback, error, safety halted). | Android Emulator or physical test device (`./gradlew connectedDebugAndroidTest`) | Release candidate verification | NFR-02, NFR-04, NFR-06, NFR-14, UC-01, UC-04, UC-22 |
| **Manual Device** | Physical hardware interactions, edge-to-edge IME insets under keyboard popup, dynamic theme/font scaling (up to 200%), TalkBack screen reader accessibility traversals, touch target measurement (minimum 48 dp), airplane mode transitions, system process kill simulation, external intent resolution (dialer 119 ext. 8, SMS/chat chooser for trusted contact). | Physical test devices and configured emulators | Gate R4 beta / Release sign-off | NFR-01, NFR-03, NFR-05, NFR-06, NFR-10, UC-14, UC-15, UC-21 |

---

## 2. Device Matrix

| Device Name | Device Type | OS Version | API Level / ABI | Display Spec & Density | Role / Priority |
| --- | --- | --- | --- | --- | --- |
| **Pixel 8 emulator (AVD)** | Emulator, Android Studio | Android 15 (API 35) | SDK 35 (x86_64), `system-images;android-35;google_apis_playstore;x86_64` | 6.2" 1080×2400, 420 dpi (2.625×), 4 vCPU, 3 GB RAM | **Primary Target:** layout, edge-to-edge and IME insets, Compose states, process-kill recovery, Room persistence. Soft keyboard enabled (`hw.keyboard=no`), otherwise the IME cases cannot be executed. Not valid for launch latency, thermals, battery or real 4G. |
| **Pixel 8 emulator, API 36** | Emulator, Android Studio | Android 16 (API 36) | SDK 36 (x86_64), `system-images;android-36;google_apis_playstore;x86_64` | 6.2" 1080×2400, 420 dpi (2.625×), 4 vCPU, 3 GB RAM | **Target-SDK reference:** replace the API 35 image with this one before Gate R4, so the behaviour that `targetSdk 36` changes is exercised rather than assumed. |
| **Generic Phone Emulator / Testbed** | Android Virtual Device (AVD) | Android 8.0 (Oreo) | SDK 26 (x86_64 / arm64) | 5.0" LCD, 1080×1920 (~440 ppi), 60 Hz | **Baseline Target:** Validates `minSdk 26` compatibility, legacy crypto fallback, Room backwards compatibility, and absence of unsupported API calls. |

Physical hardware is not provisioned for this cycle. Cases that need it, such as real 4G latency,
thermal behaviour, battery and wireless adb, are recorded as `NOT AVAILABLE` with the reason, following
the policy below. They are never marked as passed.

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
| **OFF-06** | View emotion insights offline | Multiple days of check-ins exist locally. Navigate to Insights tab offline. | Effort heatmap and summary metrics render entirely from local Room data. No empty spinner or network failure view. | UC-12, NFR-04 |
| **OFF-07** | Local risk screening offline | While offline, enter text containing explicit self-harm keywords ("want to die") in check-in note or journal. | LocalRiskFloor detects match offline. Escalates immediately to L2/L3 protocol without attempting network calls. | UC-13, UC-15, NFR-04 |
| **OFF-08** | Emergency crisis dialer access offline | From any screen, navigate to crisis support or trigger safety protocol offline. Tap "Call 119 ext. 8". | Crisis numbers are bundled locally. Android phone dialer opens prefilled with `119` (extension `8`). No network connection required. | UC-15, NFR-04 |
| **OFF-09** | Account sign-in / upgrade attempt offline | Navigate to Profile -> Account & Backup while offline. Attempt email/password sign-in. | App displays clear message explaining that an active connection is required for account operations. Local session and data remain untouched. | UC-21, NFR-04, NFR-15 |
| **OFF-10** | Backup synchronization queued offline | User is signed in with backup enabled. New check-in logged while offline. | Backup sync request is deferred to WorkManager with `NetworkType.CONNECTED` constraint. Sync executes when connectivity resumes. | UC-22, NFR-04 |

---

### 3.2 Rotation Cases

All rotation cases test orientation changes between portrait (natural) and landscape modes.

| Case ID | Title / Scenario | Preconditions & Input Action | Expected Result | Reference |
| --- | --- | --- | --- | --- |
| **ROT-01** | Rotation during onboarding consent & inputs | Onboarding screen S-03 (consent toggles) or S-04 (nickname input). Enter text and toggle switches, then rotate 90 degrees. | Entered text, selected checkboxes, and scroll position are fully preserved. Form remains interactive with no reset. | UC-01, NFR-05, NFR-06 |
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
| **DATA-06** | Full data export as JSON | Navigate to Data Settings (S-92) -> "Export My Data". Tap Export. | App generates valid, structured JSON file containing user profile, check-in history, journal entries, and micro-actions via Storage Access Framework. | UC-16, NFR-08 |
| **DATA-07** | Complete local data deletion | In S-92, select "Delete All Local Data" and confirm dialog. | Room database is wiped, DataStore is reset, credentials are removed from Keystore. App restarts to first-run onboarding (S-01). | UC-16, NFR-08 |
| **DATA-08** | Server and local log sanitisation | Perform check-in and reframe session with sensitive personal text. Inspect logcat and diagnostic logs. | Zero user journal or check-in text is printed to logcat. Only structured event identifiers and stage timings appear (`NFR-12`). | NFR-08, NFR-12 |
| **DATA-09** | Tracker and third-party SDK audit | Inspect build dependencies and outgoing network traffic via MITM proxy. | Zero third-party ad networks, behavioural trackers, or analytics SDKs present in APK. No telemetry transmissions occur. | NFR-08 |

---

## 4. Evidence Format and Traceability

All manual test runs and release candidate validations must produce concrete evidentiary artifacts stored under `docs/testing/evidence/`.

### 4.1 Screenshot File Naming

Screenshots captured from devices must follow the canonical format:

```
<YYYYMMDD>-<screen_id>-<state_or_condition>.png
```

- `<YYYYMMDD>`: ISO calendar date of the test execution (e.g., `20261022`).
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
