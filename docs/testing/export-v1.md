# Export Format `mindurset-export-v1`

This document specifies the deterministic JSON export produced by `ExportLocalData` and saved via
the Android Storage Access Framework (`ACTION_CREATE_DOCUMENT`).

## Determinism guarantees

- **Fixed key order**: every JSON object emits its keys in the fixed declaration order shown below,
  regardless of JVM or Android `org.json` map implementation.
- **Stable list ordering**: every array is sorted in ascending order by its primary timestamp
  (`createdAt`, `startedAt`, or `grantedAt` in epoch milliseconds) and broken by `id` ascending.
  Session messages inside each reframe session sort by `(sequence, createdAt, id)`.
- **UTC ISO-8601 timestamps**: timestamps are formatted with `DateTimeFormatter.ISO_INSTANT` from an
  injected clock, independent of device timezone or locale.
- **Empty sections**: empty list sections are always emitted as `[]`, never omitted. When no local
  profile exists, `"profile"` is `null`.

## Top-level object

| Field | Type | Description |
| --- | --- | --- |
| `format` | `string` | Constant `"mindurset-export-v1"` |
| `exportedAt` | `string` | UTC ISO-8601 timestamp when the export was generated |
| `app` | `object` | `{ "versionName": string }` from `BuildConfig.VERSION_NAME` |
| `profile` | `object \| null` | Local profile and user preferences (see below) |
| `checkIns` | `array` | All recorded check-in rows |
| `reframeSessions` | `array` | All reframe sessions with nested `messages` |
| `journalEntries` | `array` | All reflection / journal entries |
| `microActions` | `array` | All micro-actions (open, snoozed, completed) |
| `sessionSummaries` | `array` | Layer 2 contextual memory summaries |
| `trustedContacts` | `array` | Saved crisis outreach contact rows |
| `safetyEvents` | `array` | Safety ladder events reduced to metadata only |
| `consentRecords` | `array` | Local processing consent records |

## Section fields

### `profile` (`object | null`)

- `displayName` (`string`)
- `email` (`string | null`)
- `joinedAt` (`string`, UTC ISO-8601)
- `language` (`string`)
- `reminderEnabled` (`boolean`)
- `reminderHour` (`integer`, 0–23)
- `memoryEnabled` (`boolean`)
- `memoryNote` (`string`, Layer 3 personal memory note)

### `checkIns[]`

- `id` (`string`)
- `emotion` (`string`: `HAPPY`, `CALM`, `LOW`, `ANXIOUS`, `STRESSED`)
- `intensity` (`integer`, 1–10)
- `note` (`string | null`)
- `timestamp` (`string`, UTC ISO-8601 from `createdAt`)

### `reframeSessions[]`

- `id` (`string`)
- `checkInId` (`string | null`)
- `currentStep` (`integer`, 0–3)
- `status` (`string`: `active`, `completed`, `halted`)
- `promptVersion` (`string | null`)
- `modelVersion` (`string | null`)
- `startedAt` (`string`, UTC ISO-8601)
- `completedAt` (`string | null`, UTC ISO-8601)
- `messages` (`array`):
  - `id` (`string`)
  - `role` (`string`: `thought`, `evidence`, `alternative`, `generation`)
  - `content` (`string`)
  - `sequence` (`integer`)
  - `timestamp` (`string`, UTC ISO-8601 from `createdAt`)
  - `promptVersion` (`string | null`)
  - `modelVersion` (`string | null`)

### `journalEntries[]`

- `id` (`string`)
- `challenge` (`string`)
- `reframe` (`string`)
- `gratitude` (`string`)
- `createdAt` (`string`, UTC ISO-8601)
- `updatedAt` (`string`, UTC ISO-8601)

### `microActions[]`

- `id` (`string`)
- `sessionId` (`string | null`)
- `title` (`string`)
- `minutes` (`integer`)
- `done` (`boolean`)
- `snoozedUntil` (`string | null`, UTC ISO-8601)
- `createdAt` (`string`, UTC ISO-8601)
- `completedAt` (`string | null`, UTC ISO-8601)

### `sessionSummaries[]`

- `id` (`string`)
- `sessionId` (`string`)
- `summary` (`string`)
- `createdAt` (`string`, UTC ISO-8601)
- `retentionUntil` (`string`, UTC ISO-8601)

### `trustedContacts[]`

- `id` (`string`)
- `name` (`string`)
- `phone` (`string`)
- `relationship` (`string | null`)
- `createdAt` (`string`, UTC ISO-8601)

> **Note on ADR 0006:** `trustedContacts` is included here per Task F-13 (UU PDP 27/2022 data
> portability across all user-entered local rows). This differs from `docs/decisions/0006-trusted-contact-stays-local.md`
> and `docs/testing/test-plan.md` (`DATA-06`), which previously argued for excluding the third party's
> phone number. Update or supersede ADR 0006 if exclusion should be restored.

### `safetyEvents[]`

- `level` (`string`: `L0`, `L1`, `L2`, `L3`)
- `trigger` (`string`: e.g. `checkin_note`, `journal_entry`, `reframe_thought`, `companion_message`, `asq_positive`)
- `patternId` (`string | null`)
- `timestamp` (`string`, UTC ISO-8601 from `createdAt`)

### `consentRecords[]`

- `id` (`string`)
- `kind` (`string`)
- `policyVersion` (`string`)
- `grantedAt` (`string`, UTC ISO-8601)
- `revokedAt` (`string | null`, UTC ISO-8601)

## Deliberately excluded data

1. **Safety event triggering text**: `SafetyEventEntity` does not store the text that triggered a
   safety ladder escalation, and the export never adds or reconstructs it (`level`, `trigger`,
   `patternId`, `timestamp` only).
2. **ASQ screening answers**: individual Yes/No responses and the acuity probe are ephemeral
   in-memory `AsqFlow.State` values never persisted to Room; only a positive-screen `SafetyEvent`
   (`asq_positive`) exists in storage.
3. **Uncommitted drafts**: unsaved check-in notes, uncommitted journal form fields, and unsaved
   memory note drafts exist only in ViewModel memory and are excluded from persistence and export.
4. **Password credentials**: `passwordHash`, `passwordSalt`, and `iterations` from `user_account`
   are authentication secrets and are never exported.
